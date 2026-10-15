package com.trafficgate;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PolicyTest {
  @Test
  void rejectsInvalidLimits() {
    assertThrows(IllegalArgumentException.class, () -> new Policy(0, 1));
    assertThrows(IllegalArgumentException.class, () -> new Policy(10, Double.NaN));
    assertThrows(IllegalArgumentException.class, () -> new Policy(10, 0));
    assertEquals(10, new Policy(10, 1).capacity());
  }
}
