FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B package -DskipTests
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/rate-limiter-service-0.1.0.jar app.jar
USER 10001
EXPOSE 50051 8080
ENTRYPOINT ["java","-jar","app.jar"]
