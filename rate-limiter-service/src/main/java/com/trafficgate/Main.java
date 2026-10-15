package com.trafficgate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.*;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;

public final class Main {
    private static final ObjectMapper JSON = new ObjectMapper();
    private record HttpCheck(String clientId, int cost) {}
    private static String env(String key, String fallback) { return System.getenv().getOrDefault(key, fallback); }
    public static void main(String[] args) throws Exception {
        var overrides = JSON.readValue(env("CLIENT_POLICIES", "{}"), new TypeReference<Map<String, Policy>>() {});
        var limiter = new RedisLimiter(env("REDIS_URL", "redis://localhost:6379"),
                new Policy(Integer.parseInt(env("BUCKET_CAPACITY", "10")), Double.parseDouble(env("REFILL_PER_SECOND", "1"))), overrides);
        String brokers = env("KAFKA_BOOTSTRAP_SERVERS", "");
        var events = brokers.isBlank() ? null : DecisionEvents.connect(brokers, env("KAFKA_TOPIC", "trafficgate.decisions"));
        var service = new QuotaService(limiter, events);
        var grpc = ServerBuilder.forPort(Integer.parseInt(env("GRPC_PORT", "50051")))
                .addService(service).addService(ProtoReflectionService.newInstance()).build().start();
        var http = HttpServer.create(new InetSocketAddress(Integer.parseInt(env("HTTP_PORT", "8080"))), 128);
        var executor = Executors.newFixedThreadPool(16);
        http.setExecutor(executor);
        http.createContext("/health", x -> respond(x, 200, "ok", "text/plain"));
        http.createContext("/ready", x -> respond(x, limiter.healthy() ? 200 : 503, "", "text/plain"));
        http.createContext("/metrics", x -> {
            var writer = new StringWriter();
            TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
            respond(x, 200, writer.toString(), TextFormat.CONTENT_TYPE_004);
        });
        http.createContext("/v1/check", x -> {
            try {
                if (!x.getRequestURI().getPath().equals("/v1/check")) { respond(x, 404, "", "text/plain"); return; }
                if (!"POST".equals(x.getRequestMethod())) { respond(x, 405, "POST required", "text/plain"); return; }
                byte[] body = x.getRequestBody().readNBytes(4097);
                if (body.length > 4096) { respond(x, 413, "request too large", "text/plain"); return; }
                var input = JSON.readValue(body, HttpCheck.class);
                if (input == null) throw new IllegalArgumentException("request required");
                var d = service.check(input.clientId(), input.cost());
                if (!d.allowed()) x.getResponseHeaders().set("Retry-After", "" + ((d.retryAfterMs() + 999) / 1000));
                respond(x, d.allowed() ? 200 : 429, JSON.writeValueAsString(d), "application/json");
            } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException e) {
                respond(x, 400, "invalid quota request", "text/plain");
            } catch (RuntimeException e) {
                Metrics.ERRORS.inc(); respond(x, 503, "quota store unavailable", "text/plain");
            } finally { x.close(); }
        });
        http.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            http.stop(1); executor.shutdown(); grpc.shutdown();
            if (events != null) events.close(); limiter.close();
        }));
        System.out.println("TrafficGate listening: gRPC=" + grpc.getPort() + " HTTP=" + http.getAddress().getPort());
        grpc.awaitTermination();
    }
    private static void respond(HttpExchange x, int status, String body, String type) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type", type);
        x.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        try (var out = x.getResponseBody()) { out.write(bytes); }
    }
}
