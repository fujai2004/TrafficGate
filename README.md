# TrafficGate

A Java 17 distributed token-bucket rate limiter with Redis Lua, gRPC, Kafka decision events, and Prometheus metrics. The rough MVP includes two service instances sharing the same quota store.

## Run the demo

With Docker Desktop running:

```sh
docker compose -f deploy/docker-compose.yml up --build -d
node tests/stack-smoke.mjs
```

| Endpoint | Address |
| --- | --- |
| gRPC node A / B | localhost:50051 / localhost:50052 |
| HTTP node A / B | http://localhost:8081 / http://localhost:8082 |
| Prometheus | http://localhost:9090 |

Redis and Kafka stay on the internal Compose network. SecureShare uses different app ports, so the demos can run together.

Check a quota using HTTP:

```sh
curl -i http://localhost:8081/v1/check -H 'Content-Type: application/json' -d '{"clientId":"alice","cost":1}'
```

Allowed requests return 200, exhausted quotas return 429 with Retry-After. Responses include allowed, remaining, retryAfterMs, and capacity. The default policy is a burst of 10 tokens, refilling one token each second. Alternating requests between ports 8081 and 8082 consumes the same client's bucket.

The primary API is gRPC; server reflection is enabled:

```sh
grpcurl -plaintext -d '{"clientId":"alice","cost":1}' localhost:50051 trafficgate.v1.RateLimiter/CheckQuota
```

A quota denial is a successful RPC with allowed=false. Invalid requests return INVALID_ARGUMENT; an unavailable Redis store returns UNAVAILABLE and does not allow requests.

## Configuration

| Variable | Default | Meaning |
| --- | --- | --- |
| REDIS_URL | redis://localhost:6379 | Shared quota store |
| BUCKET_CAPACITY | 10 | Default maximum tokens |
| REFILL_PER_SECOND | 1 | Fractional refill rate |
| CLIENT_POLICIES | {} | JSON map from client ID to capacity/refillPerSecond |
| KAFKA_BOOTSTRAP_SERVERS | empty | Set to enable best-effort decision events |
| KAFKA_TOPIC | trafficgate.decisions | Event topic |
| GRPC_PORT / HTTP_PORT | 50051 / 8080 | Native service listeners |

For example, CLIENT_POLICIES='{"premium":{"capacity":100,"refillPerSecond":10}}'. Policy is selected on the server; callers cannot choose their own refill rate. cost=0 means one token for protobuf's default value. Negative costs and costs exceeding capacity are rejected.

This is an internal service: a trusted gateway must supply the client identity and control access. It does not authenticate caller-supplied client IDs.

## Events and metrics

Kafka events are keyed by client ID and include eventId, timestamp, and a structured decision. Publishing runs through a bounded queue. A broker outage can drop telemetry but cannot block quota enforcement indefinitely; drops are counted. There is no transactional outbox, and events are not a billing-grade audit trail.

Consume the demo log:

```sh
docker compose -f deploy/docker-compose.yml exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic trafficgate.decisions --from-beginning
```

/metrics exposes allowed/denied counts, backend errors, event drops, and a check-duration histogram. /health checks the process; /ready checks Redis. Kafka availability is deliberately excluded from readiness. Prometheus scrapes both nodes.

## Build and test

Use Java 17 and Maven 3.9+.

```sh
cd rate-limiter-service
mvn verify
java -jar target/rate-limiter-service-0.1.0.jar
```

Unit tests run without Redis; five integration tests require REDIS_URL. To run those locally, start a separate test Redis:

```sh
docker run --rm -p 127.0.0.1:6379:6379 redis:7.4-alpine
# In another shell:
REDIS_URL=redis://localhost:6379 mvn -f rate-limiter-service/pom.xml verify
```

PowerShell: `$env:REDIS_URL='redis://localhost:6379'`, then run Maven. CI provides real Redis and also runs the full Compose stack and Kafka consumption check.

Tests cover concurrent checks from two limiter instances, refill, per-client policy, expiry, gRPC validation, fail-closed behavior, and event publishing/failure handling. The optional local-codegen Maven profile accepts protoc.executable and grpc.plugin.executable for environments requiring locally built generators.

## Load checks

```sh
k6 run load-tests/burst.js
k6 run -e RATE=10000 -e P99_MS=5 --summary-export report.json load-tests/burst.js
```

The script calls the real quota RPC at a constant arrival rate, tracks transport failures separately from quota denials, and fails if iterations are dropped. Default RATE=1000, DURATION=30s, P99_MS=50; GRPC_ADDR defaults to localhost:50051.

**The 10K requests/s, sub-5ms p99 target is not verified.** A local functional run used a Redis-compatible Lua test server and a real Kafka broker, completing 1,001 quota RPCs at about 100/s without transport errors or dropped iterations. Its p99 was about 110 ms; that setup is not a Redis or container performance benchmark.

See [validation status](docs/mvp-status.md), [architecture](docs/architecture.md), and [roadmap](docs/roadmap.md).

MIT license.
