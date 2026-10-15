package com.trafficgate;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import redis.clients.jedis.JedisPooled;

public final class RedisLimiter implements AutoCloseable {
    private final JedisPooled redis;
    private final Policy defaultPolicy;
    private final Map<String, Policy> overrides;
    private final String script;
    public RedisLimiter(String uri, Policy defaultPolicy, Map<String, Policy> overrides) throws IOException {
        redis = new JedisPooled(java.net.URI.create(uri));
        this.defaultPolicy = defaultPolicy;
        this.overrides = Map.copyOf(overrides);
        try (var resource = RedisLimiter.class.getResourceAsStream("/token_bucket.lua")) {
            if (resource == null) throw new IOException("missing token bucket script");
            script = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    public Decision check(String clientId, int cost) {
        if (clientId == null || !clientId.matches("[a-zA-Z0-9_.:@-]{1,128}"))
            throw new IllegalArgumentException("client_id must contain 1..128 letters, digits, or _.:@-");
        var p = overrides.getOrDefault(clientId, defaultPolicy);
        if (cost < 0 || cost > p.capacity()) throw new IllegalArgumentException("cost must be between 0 and capacity");
        var r = (List<?>) redis.eval(script, List.of("trafficgate:bucket:" + clientId),
                List.of("" + p.capacity(), "" + p.refillPerSecond(), "" + (cost == 0 ? 1 : cost)));
        return new Decision(((Number) r.get(0)).longValue() == 1, ((Number) r.get(1)).longValue(),
                ((Number) r.get(2)).longValue(), p.capacity());
    }
    public boolean healthy() {
        try { return "PONG".equals(redis.ping()); } catch (RuntimeException e) { return false; }
    }
    @Override public void close() { redis.close(); }
}
