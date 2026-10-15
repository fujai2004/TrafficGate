# Architecture

Clients call CheckQuota(client_id,cost) over gRPC. An HTTP adapter exposes the same operation for simple demos. Each Java node selects a server-configured policy and evaluates one Lua script against Redis.

## Atomic token bucket

The script loads tokens and update time, refills elapsed tokens up to capacity, conditionally subtracts cost, and writes state and expiry in one atomic EVAL operation. Redis TIME supplies the clock for every Java node, avoiding application-clock skew. Negative clock movement is clamped, and the stored update timestamp never goes backwards.

Keys expire only after an idle bucket would have fully refilled. Denials preserve fractional tokens and return the rounded-up milliseconds until the requested cost becomes affordable. The client sees the floor of the remaining token count.

Two nodes use the same key for a client, so the quota is not multiplied by the number of Java instances. Request cost must fit in the bucket. Redis failures produce UNAVAILABLE/503 rather than fabricating an allow decision.

The Java connection pool bounds Redis connections. Lua execution is short and constant-time, but a single very hot key still belongs to one Redis shard. This MVP uses a single Redis instance and does not establish Redis failover durability.

## Events

Decisions are serialized into structured JSON with a UUID event ID and timestamp, then queued for a Kafka worker. The queue holds at most 4,096 pending events; producer failures and queue overflow increment a dropped-event counter. Kafka producer idempotence and acks=all reduce producer retry duplicates, but there is no atomic transaction between Redis mutation and Kafka publication.

Client ID is the Kafka partition key. Events from concurrent Java instances can be enqueued in a different order from Redis execution. Consumers must not assume exact quota replay order. The MVP ships a consumer smoke command; durable replay and anomaly detection are future work.

## Observability

Metrics have fixed, low-cardinality labels. Never label metrics with arbitrary client IDs. Check latency includes the Redis call and event enqueue/serialization, but excludes network transit and RPC framing; use k6 for end-to-end latency.

Readiness depends on Redis. Kafka telemetry is optional and best effort, so a Kafka outage does not remove healthy quota nodes from service.

## Demo boundaries

Compose launches two nodes, Redis, a single Kafka KRaft broker, and Prometheus. Redis AOF is persisted in a named volume. Kafka demo logs are not persisted across container recreation. Ports bind only to loopback; no caller authentication or transport TLS is built into the service.

The demo uses a modest Java heap/CPU cap. Change resource allocation deliberately when benchmarking and record it with the results. Throughput, p99, Redis durability on failure, and cluster availability must be measured separately.
