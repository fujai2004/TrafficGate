# Contributing to TrafficGate

This repository is in **placeholder** mode. The README and docs describe the intended system; implementation is planned.

## Before coding

1. Read [docs/architecture.md](docs/architecture.md) and [docs/roadmap.md](docs/roadmap.md).
2. Open an issue or discussion for large design changes.

## Planned modules

| Path | Purpose |
|------|---------|
| `rate-limiter-service/` | Java gRPC rate limiter |
| `load-tests/` | k6 burst and sustained tests |
| `deploy/` | Docker Compose for Redis, Kafka, service |

## Guidelines

- Keep commits focused per module or milestone.
- Do not commit secrets or `.env` files.
- Add tests with each feature (unit + load where relevant).
