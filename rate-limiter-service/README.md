# rate-limiter-service

**Status:** Placeholder

Planned **Java** service exposing gRPC endpoints for distributed rate limiting.

## Planned responsibilities

- Execute atomic **Redis Lua** token-bucket updates
- Return allow/deny with optional retry-after metadata
- Publish decisions to **Kafka**
- Expose **Prometheus** metrics

## Placeholder layout (future)

```
rate-limiter-service/
├── src/main/java/          # gRPC server, limiter logic
├── src/main/resources/     # Redis script, config
└── pom.xml                 # Dependencies: gRPC, Redis, Kafka client
```

Implementation tracked in [docs/roadmap.md](../docs/roadmap.md).
