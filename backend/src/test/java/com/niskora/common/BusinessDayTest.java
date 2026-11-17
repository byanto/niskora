package com.niskora.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BusinessDayTest {

  @Test
  void aMomentBeforeMidnightInJakartaBelongsToThatDayEvenThoughUtcIsEarlier() {
    // 16:59:59 UTC is 23:59:59 WIB on 16 November.
    assertThat(BusinessDay.of(Instant.parse("2026-11-16T16:59:59Z")).date())
        .isEqualTo(LocalDate.of(2026, 11, 16));
  }

  @Test
  void midnightInJakartaStartsTheNextDayWhileUtcIsStillOnTheDayBefore() {
    // 17:00 UTC on 16 November is 00:00 WIB on 17 November.
    assertThat(BusinessDay.of(Instant.parse("2026-11-16T17:00:00Z")).date())
        .isEqualTo(LocalDate.of(2026, 11, 17));
  }

  @Test
  void aBusinessDayRunsFromJakartaMidnightToTheNextJakartaMidnight() {
    BusinessDay day = new BusinessDay(LocalDate.of(2026, 11, 17));

    assertThat(day.startsAt()).isEqualTo(Instant.parse("2026-11-16T17:00:00Z"));
    assertThat(day.endsAt()).isEqualTo(Instant.parse("2026-11-17T17:00:00Z"));
  }

  @Test
  void writesAsAPlainIsoDate() {
    assertThat(new BusinessDay(LocalDate.of(2026, 11, 7))).hasToString("2026-11-07");
  }

  @Test
  void theBusinessClockGivesTheMomentInUtcAndTodayInJakarta() {
    BusinessClock clock =
        new BusinessClock(Clock.fixed(Instant.parse("2026-11-16T18:30:00Z"), ZoneOffset.UTC));

    assertThat(clock.now()).isEqualTo(Instant.parse("2026-11-16T18:30:00Z"));
    assertThat(clock.today()).isEqualTo(new BusinessDay(LocalDate.of(2026, 11, 17)));
  }

  @Test
  void theBusinessClockReadsTheSameDayWhateverZoneItsClockIsIn() {
    BusinessClock clock =
        new BusinessClock(
            Clock.fixed(Instant.parse("2026-11-16T18:30:00Z"), ZoneOffset.ofHours(-10)));

    assertThat(clock.today()).isEqualTo(new BusinessDay(LocalDate.of(2026, 11, 17)));
  }
}
