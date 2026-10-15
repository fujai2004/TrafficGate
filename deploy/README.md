# Local stack
From the repository root, run `docker compose -f deploy/docker-compose.yml up --build -d`.
The two nodes expose gRPC on 50051/50052 and HTTP on 8081/8082.
Prometheus is on http://localhost:9090. Redis and Kafka are internal services.
Run `node tests/stack-smoke.mjs` to verify a quota shared by both nodes.
Inspect events:
```sh
docker compose -f deploy/docker-compose.yml exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic trafficgate.decisions --from-beginning
```
Stop with `docker compose -f deploy/docker-compose.yml down`. Add `-v` only to discard demo state.
