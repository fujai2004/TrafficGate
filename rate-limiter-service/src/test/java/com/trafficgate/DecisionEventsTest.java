package com.trafficgate;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
class DecisionEventsTest {
    @Test void publishesKeyedStructuredEvents() throws Exception {
        var producer=new MockProducer<String,String>(true,new StringSerializer(),new StringSerializer());
        try(var events=new DecisionEvents(producer,"decisions")) {
            events.publish("alice",new Decision(false,0,1000,10));
        }
        assertEquals(1,producer.history().size());
        var event=producer.history().get(0);
        assertEquals("alice",event.key());
        var value=new ObjectMapper().readTree(event.value());
        assertFalse(value.get("decision").get("allowed").asBoolean());
        assertEquals(1000,value.get("decision").get("retryAfterMs").asLong());
        assertTrue(value.has("eventId"));
    }
}
