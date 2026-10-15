# Rate limiter service
Java 17 and Maven 3.9+. Build using `mvn verify`; set REDIS_URL to run Redis integration tests.
Run `java -jar target/rate-limiter-service-0.1.0.jar`. Defaults: gRPC 50051, HTTP 8080, Redis localhost:6379.
See the root README for the API, server policy configuration, Kafka, metrics and two-node demo.
