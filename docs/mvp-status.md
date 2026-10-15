# MVP validation status

## GitHub CI verified
On October 15, 2026, [CI run 38744422238](https://github.com/fujai2004/TrafficGate/actions/runs/38744422238) passed on commit 227d938. The test job ran against real Redis; the stack job built the containers, exercised two limiter nodes sharing a quota, and consumed a real Kafka event. This closes the original real-Redis and container-startup verification gaps. It does not establish the throughput target or verify Prometheus scraping end to end.

## Locally exercised
- Java/protobuf build and executable service jar.
- Eight tests passed against a Redis-compatible Lua test server, with no skips: policy validation, keyed Kafka serialization, producer failure handling, two-node 200-request concurrency, refill/overrides, expiry, gRPC validation and fail-closed store errors.
- Two real Java service processes shared the test server; HTTP smoke checked shared quota, retry, invalid input and Prometheus exposition.
- A real Kafka 3.9 broker accepted decision events; the console consumer read a structured event from trafficgate.decisions.
- k6 2.3.0 called the real gRPC endpoint: 1,001 requests over 10 seconds at about 100/s, zero transport errors and zero dropped iterations. p99 was about 110 ms. This is a functional test on a simulated Redis backend, not a performance result for Redis.
- Compose configuration parses successfully.

The Java build and packaged jar also passed Maven verify.

## Not yet established
Docker Desktop's engine could not start locally. Windows Redis ports were blocked by runtime restrictions, so local Lua integration used fakeredis with the Lua extension. The subsequent GitHub CI run above supplies the real Redis and container verification.

Prometheus scraping remains unverified end to end. No 10K/s or sub-5ms p99 performance claim is justified by these functional checks.

## Resume alignment
The implemented code supports discussing token-bucket math, Lua atomicity, gRPC contracts/errors, Redis shared state, asynchronous Kafka telemetry, low-cardinality metrics, and reproducible load testing. Keep the original throughput/latency bullet as a target until a real containerized benchmark supplies evidence.
