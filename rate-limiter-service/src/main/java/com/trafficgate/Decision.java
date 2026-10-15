package com.trafficgate;
public record Decision(boolean allowed, long remaining, long retryAfterMs, long capacity) {}
