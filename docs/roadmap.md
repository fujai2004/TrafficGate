# Roadmap
## Rough MVP implemented
- [x] Java 17 Maven project and runnable service jar.
- [x] Atomic Redis Lua token bucket with Redis-side time.
- [x] gRPC quota decisions, remaining tokens and retry timing.
- [x] Per-client server policy overrides and input validation.
- [x] Bounded best-effort Kafka publishing and consumer smoke command.
- [x] Prometheus counters and latency histogram.
- [x] Two-node Compose demo and readiness endpoints.
- [x] Constant-arrival-rate k6 quota RPC script.
- [x] Unit, concurrency, expiry, failure and API tests.
- [x] CI with real Redis and full-stack checks.

## Verification remaining
- [x] Execute the prepared CI and Compose checks with real Redis (GitHub CI run 38744422238).
- [ ] Benchmark a containerized stack with recorded hardware and resources.
- [ ] Establish whether the 10K requests/s, sub-5ms p99 target is achievable.

## Beyond MVP
- [ ] Durable event outbox, ordered replay and anomaly consumer.
- [ ] Gateway authentication/TLS integration.
- [ ] Redis failover and policy-change semantics.
- [ ] Grafana dashboards and longer soak tests.
