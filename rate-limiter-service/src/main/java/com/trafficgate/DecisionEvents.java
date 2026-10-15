package com.trafficgate;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.*;
import org.apache.kafka.clients.producer.*;

/** Bounded best-effort telemetry; Kafka outages cannot block quota checks. */
public final class DecisionEvents implements AutoCloseable {
  private final Producer<String, String> producer;
  private final String topic;
  private final ObjectMapper json = new ObjectMapper();
  private final ThreadPoolExecutor worker =
      new ThreadPoolExecutor(
          1,
          1,
          0,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<>(4096),
          new ThreadPoolExecutor.AbortPolicy());

  public DecisionEvents(Producer<String, String> producer, String topic) {
    this.producer = producer;
    this.topic = topic;
  }

  public static DecisionEvents connect(String brokers, String topic) {
    var p = new Properties();
    p.put("bootstrap.servers", brokers);
    p.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    p.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    p.put("acks", "all");
    p.put("enable.idempotence", "true");
    p.put("max.block.ms", "1000");
    p.put("delivery.timeout.ms", "10000");
    p.put("request.timeout.ms", "5000");
    return new DecisionEvents(new KafkaProducer<>(p), topic);
  }

  public void publish(String clientId, Decision decision) {
    try {
      String event =
          json.writeValueAsString(
              Map.of(
                  "eventId",
                  UUID.randomUUID().toString(),
                  "clientId",
                  clientId,
                  "timestamp",
                  Instant.now().toString(),
                  "decision",
                  decision));
      worker.execute(
          () -> {
            try {
              producer.send(
                  new ProducerRecord<>(topic, clientId, event),
                  (m, error) -> {
                    if (error != null) Metrics.EVENTS_DROPPED.inc();
                  });
            } catch (RuntimeException e) {
              Metrics.EVENTS_DROPPED.inc();
            }
          });
    } catch (Exception e) {
      Metrics.EVENTS_DROPPED.inc();
    }
  }

  @Override
  public void close() {
    worker.shutdown();
    try {
      if (!worker.awaitTermination(5, TimeUnit.SECONDS)) worker.shutdownNow();
    } catch (InterruptedException e) {
      worker.shutdownNow();
      Thread.currentThread().interrupt();
    }
    producer.close(Duration.ofSeconds(5));
  }
}
