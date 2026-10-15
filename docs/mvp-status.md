# MVP validation status

## Locally exercised
- Java/protobuf build and executable service jar.
- Eight tests passed against a Redis-compatible Lua test server, with no skips: policy validation, keyed Kafka serialization, producer failure handling, two-node 200-request concurrency, refill/overrides, expiry, gRPC validation and fail-closed store errors.
- Two real Java service processes shared the test server; HTTP smoke checked shared quota, retry, invalid input and Prometheus exposition.
- A real Kafka 3.9 broker accepted decision events; the console consumer read a structured event from trafficgate.decisions.
- k6 2.3.0 called the real gRPC endpoint: 1,001 requests over 10 seconds at about 100/s, zero transport errors and zero dropped iterations. p99 was about 110 ms. This is a functional test on a simulated Redis backend, not a performance result for Redis.
- Compose configuration parses successfully.

The Java build and packaged jar also passed Maven verify.

## Not yet established
Docker Desktop's engine could not start in this environment. Windows Redis ports were blocked by runtime restrictions, so Lua integration used fakeredis with the Lua extension. This validates the script/protocol/API paths but does not replace running the same suite against Redis.

The GitHub CI definition includes real Redis integration plus two-node Compose and Kafka consumption checks. It has not run on these local commits yet. Prometheus scraping in containers and container startup remain unverified. No 10K/s or sub-5ms p99 performance claim is justified by the functional run.

## Resume alignment
The implemented code supports discussing token-bucket math, Lua atomicity, gRPC contracts/errors, Redis shared state, asynchronous Kafka telemetry, low-cardinality metrics, and reproducible load testing. Keep the original throughput/latency bullet as a target until a real containerized benchmark supplies evidence.
