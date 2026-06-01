# Architecture (planned)

## Overview

TrafficGate sits in front of backend services (or at an API gateway boundary) and answers: **should this client request proceed right now?**

## Rate limiting

- **Algorithm:** Token bucket per client ID (configurable refill rate and burst).
- **Storage:** Redis with a **Lua script** so read-check-update is atomic across all service instances.
- **Goal:** No race conditions when multiple Java nodes handle the same client concurrently.

## Event streaming

- Every allow/deny (and optional metadata) is published to **Kafka**.
- **Partition key:** client ID (keeps one client's timeline ordered).
- **Consumers:** Replay traffic for debugging, offline burst/anomaly detection, dashboards.

## API

- **gRPC** for low-latency synchronous checks (target: sub-5ms p99 under load).
- REST or HTTP gateway optional later.

## Observability

- **Prometheus** metrics: allowed/denied counts, latency histograms, Redis/Kafka health.

## Load testing

- **k6** scenarios: sustained RPS, burst spikes, multi-client fairness.
- **Target (resume):** sub-5ms p99 at 10K requests/second in containerized deployment.

## Deployment

- **Docker Compose** for local dev: Redis, Kafka, Zookeeper (if needed), rate-limiter-service, Prometheus.
