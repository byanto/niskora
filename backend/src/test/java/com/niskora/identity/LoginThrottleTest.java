package com.niskora.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Repeated failed logins are slowed down (PLT-19). */
class LoginThrottleTest {

  private final MovableClock clock = new MovableClock(Instant.parse("2026-11-19T03:00:00Z"));
  private final LoginThrottle throttle = new LoginThrottle(clock);

  @Test
  void theFirstFiveFailuresMayBeRetriedAtOnce() {
    failTimes("rina", 4);
    assertThat(throttle.attempt("rina")).isEmpty();
  }

  @Test
  void fromTheFifthFailureEachRetryWaitsTwiceAsLong() {
    failTimes("rina", 5);
    assertThat(throttle.attempt("rina")).contains(Duration.ofSeconds(1));

    clock.advance(Duration.ofSeconds(1));
    assertThat(throttle.attempt("rina")).isEmpty();
    assertThat(throttle.attempt("rina")).contains(Duration.ofSeconds(2));

    clock.advance(Duration.ofSeconds(2));
    assertThat(throttle.attempt("rina")).isEmpty();
    assertThat(throttle.attempt("rina")).contains(Duration.ofSeconds(4));
  }

  @Test
  void theWaitCountsFromTheLastFailure() {
    failTimes("rina", 7);
    clock.advance(Duration.ofSeconds(3));

    assertThat(throttle.attempt("rina")).contains(Duration.ofSeconds(1));
    clock.advance(Duration.ofSeconds(1));
    assertThat(throttle.attempt("rina")).isEmpty();
  }

  @Test
  void anAttemptRefusedForWaitingIsNoFailure() {
    failTimes("rina", 6);
    for (int i = 0; i < 10; i++) {
      assertThat(throttle.attempt("rina")).contains(Duration.ofSeconds(2));
    }
  }

  @Test
  void attemptsStillWaitingForTheirAnswerCountAsFailures() {
    // Five logins arrive at the same moment, and none has been answered yet.
    failTimes("rina", 5);

    assertThat(throttle.attempt("rina")).contains(Duration.ofSeconds(1));
  }

  @Test
  void theWaitNeverExceedsFifteenMinutes() {
    failTimes("rina", 30);
    assertThat(throttle.attempt("rina")).contains(Duration.ofMinutes(15));
  }

  @Test
  void anHourWithoutAFailureForgetsTheEarlierOnes() {
    failTimes("rina", 30);
    clock.advance(Duration.ofHours(1));

    failTimes("rina", 1);
    assertThat(throttle.attempt("rina")).isEmpty();
  }

  @Test
  void aSuccessfulLoginForgetsTheFailures() {
    failTimes("rina", 5);
    clock.advance(Duration.ofSeconds(1));
    assertThat(throttle.attempt("rina")).isEmpty();
    throttle.succeeded("rina");

    failTimes("rina", 1);
    assertThat(throttle.attempt("rina")).isEmpty();
  }

  @Test
  void eachLoginNameIsSlowedDownOnItsOwnWhateverItsCase() {
    failTimes("rina", 5);

    assertThat(throttle.attempt("RINA")).contains(Duration.ofSeconds(1));
    assertThat(throttle.attempt("sari")).isEmpty();
  }

  /**
   * Attempts that are let through and never succeed. One that must wait is made once the wait is
   * over, so the clock moves on with them.
   */
  private void failTimes(String loginName, int times) {
    for (int i = 0; i < times; i++) {
      Optional<Duration> wait = throttle.attempt(loginName);
      if (wait.isPresent()) {
        clock.advance(wait.get());
        assertThat(throttle.attempt(loginName)).isEmpty();
      }
    }
  }

  private static final class MovableClock extends Clock {

    private Instant now;

    MovableClock(Instant now) {
      this.now = now;
    }

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public Instant instant() {
      return now;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      throw new UnsupportedOperationException();
    }
  }
}
