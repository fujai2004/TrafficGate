package com.trafficgate;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
public final class Metrics {
    private Metrics() {}
    public static final Counter DECISIONS = Counter.build("trafficgate_decisions_total", "Quota decisions").labelNames("result").register();
    public static final Counter ERRORS = Counter.build("trafficgate_errors_total", "Backend errors").register();
    public static final Counter EVENTS_DROPPED = Counter.build("trafficgate_events_dropped_total", "Events not delivered to Kafka").register();
    public static final Histogram LATENCY = Histogram.build("trafficgate_check_seconds", "Quota check time")
            .buckets(.0005, .001, .0025, .005, .01, .025, .05, .1, .5, 1).register();
}
