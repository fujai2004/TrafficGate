package com.trafficgate;
import com.trafficgate.proto.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
public final class QuotaService extends RateLimiterGrpc.RateLimiterImplBase {
    private final RedisLimiter limiter;
    private final DecisionEvents events;
    public QuotaService(RedisLimiter limiter, DecisionEvents events) { this.limiter = limiter; this.events = events; }
    public Decision check(String clientId, int cost) {
        try (var timer = Metrics.LATENCY.startTimer()) {
            var d = limiter.check(clientId, cost);
            Metrics.DECISIONS.labels(d.allowed() ? "allowed" : "denied").inc();
            if (events != null) events.publish(clientId, d);
            return d;
        }
    }
    @Override public void checkQuota(CheckRequest request, StreamObserver<CheckResponse> observer) {
        try {
            var d = check(request.getClientId(), request.getCost());
            observer.onNext(CheckResponse.newBuilder().setAllowed(d.allowed()).setRemaining(d.remaining())
                    .setRetryAfterMs(d.retryAfterMs()).setCapacity(d.capacity()).build());
            observer.onCompleted();
        } catch (IllegalArgumentException e) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (RuntimeException e) {
            Metrics.ERRORS.inc();
            observer.onError(Status.UNAVAILABLE.withDescription("quota store unavailable").asRuntimeException());
        }
    }
}
