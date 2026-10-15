package com.trafficgate;

public record Policy(int capacity, double refillPerSecond) {
  public Policy {
    if (capacity < 1
        || capacity > 1_000_000
        || !Double.isFinite(refillPerSecond)
        || refillPerSecond < 0.001
        || refillPerSecond > 1_000_000)
      throw new IllegalArgumentException("invalid bucket capacity or refill rate");
  }
}
