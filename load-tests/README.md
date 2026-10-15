# gRPC load checks
Run from the repository root with Grafana k6 installed and TrafficGate running.
```sh
k6 run load-tests/burst.js
k6 run -e RATE=100 -e DURATION=10s -e P99_MS=1000 load-tests/burst.js
k6 run -e RATE=10000 -e DURATION=60s -e P99_MS=5 --summary-export report.json load-tests/burst.js
```
GRPC_ADDR defaults to localhost:50051. One persistent gRPC connection is created per virtual user.
Quota denials are valid responses, not transport errors. Each VU maps into a pool of 100 client IDs so the test exercises shared buckets.
Record hardware, Java/Redis/Kafka versions, CPU/memory allocation, resource saturation, delivered arrival rate, dropped iterations, p99 and error rate before making performance claims.
The 10K/s command is an acceptance target; no passing result is implied.
