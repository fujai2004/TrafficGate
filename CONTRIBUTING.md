# Contributing
Read docs/architecture.md and docs/mvp-status.md before changing quota semantics.
Use Java 17 and Maven 3.9+. Run mvn verify in rate-limiter-service; set REDIS_URL to exercise the integration tests.
Run the two-node Compose smoke check when changing deployment, HTTP, or Kafka behavior.
Keep commits focused and document actual validation results. Distinguish measured performance from targets.
Never commit secrets, .env files or generated build output.
