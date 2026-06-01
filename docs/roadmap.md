# Roadmap

## Milestone 1: Core limiter

- [ ] Java project scaffold (Maven or Gradle)
- [ ] Redis Lua token-bucket script
- [ ] gRPC `CheckQuota` RPC
- [ ] Unit tests for bucket math and script behavior

## Milestone 2: Distribution

- [ ] Multi-instance deployment behind load balancer
- [ ] Kafka producer for decision events
- [ ] Basic consumer for replay smoke test

## Milestone 3: Operations

- [ ] Prometheus metrics and Grafana dashboard sketch
- [ ] Docker Compose stack
- [ ] k6 load test with p99 / throughput report

## Milestone 4: Hardening

- [ ] Configurable limits per client / tier
- [ ] CI pipeline (build, test, optional load smoke)
- [ ] README runbook and architecture diagrams finalized
