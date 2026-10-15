package com.trafficgate;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

class DecisionEventsTest {
  @Test
  void producerFailureIsCountedWithoutEscapingTheWorker() {
    var producer =
        new MockProducer<String, String>(true, new StringSerializer(), new StringSerializer()) {
          @Override
          public synchronized java.util.concurrent.Future<
                  org.apache.kafka.clients.producer.RecordMetadata>
              send(
                  org.apache.kafka.clients.producer.ProducerRecord<String, String> record,
                  org.apache.kafka.clients.producer.Callback callback) {
            throw new org.apache.kafka.common.KafkaException("broker offline");
          }
        };
    double before = Metrics.EVENTS_DROPPED.get();
    try (var events = new DecisionEvents(producer, "decisions")) {
      events.publish("alice", new Decision(true, 1, 0, 10));
    }
    assertEquals(before + 1, Metrics.EVENTS_DROPPED.get());
  }

  @Test
  void publishesKeyedStructuredEvents() throws Exception {
    var producer =
        new MockProducer<String, String>(true, new StringSerializer(), new StringSerializer());
    try (var events = new DecisionEvents(producer, "decisions")) {
      events.publish("alice", new Decision(false, 0, 1000, 10));
    }
    assertEquals(1, producer.history().size());
    var event = producer.history().get(0);
    assertEquals("alice", event.key());
    var value = new ObjectMapper().readTree(event.value());
    assertFalse(value.get("decision").get("allowed").asBoolean());
    assertEquals(1000, value.get("decision").get("retryAfterMs").asLong());
    assertTrue(value.has("eventId"));
  }
}
