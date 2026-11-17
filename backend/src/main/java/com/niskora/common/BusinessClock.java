package com.niskora.common;

import java.time.Clock;
import java.time.Instant;

/**
 * The time as the business reads it (ENG-64): moments in UTC and today as the Jakarta business day.
 * It wraps the injected {@link Clock}, the one source of time, so a test fixes the time by giving
 * it a fixed clock. Code that needs only the moment may take the {@link Clock} itself.
 */
public class BusinessClock {

  private final Clock clock;

  public BusinessClock(Clock clock) {
    this.clock = clock;
  }

  /** The current moment. An {@link Instant} has no zone, so it is stored and sent as UTC. */
  public Instant now() {
    return clock.instant();
  }

  /** The business day it is now in Jakarta. */
  public BusinessDay today() {
    return BusinessDay.of(now());
  }
}
