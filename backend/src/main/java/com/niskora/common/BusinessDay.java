package com.niskora.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * A business day: midnight to midnight in Jakarta time (WIB), whatever zone the machine runs in
 * (ENG-64). It writes as a plain ISO date, the form the API uses for a business date (ENG-80).
 */
public record BusinessDay(LocalDate date) {

  /** The zone every business date is computed in. */
  public static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");

  /** The business day a moment falls on. */
  public static BusinessDay of(Instant moment) {
    return new BusinessDay(LocalDate.ofInstant(moment, ZONE));
  }

  /** The moment this day starts: midnight in Jakarta. */
  public Instant startsAt() {
    return date.atStartOfDay(ZONE).toInstant();
  }

  /** The moment the next day starts, so a moment on this day is before it, never at it. */
  public Instant endsAt() {
    return date.plusDays(1).atStartOfDay(ZONE).toInstant();
  }

  @Override
  public String toString() {
    return date.toString();
  }
}
