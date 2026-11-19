package com.niskora.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Repeated failed logins are slowed down (PLT-24). */
class LoginThrottleTest {

  private final MovableClock clock = new MovableClock(Instant.parse("2026-11-19T03:00:00Z"));
  private final LoginThrottle throttle = new LoginThrottle(clock);

  @Test
  void theFirstFiveFailuresMayBeRetriedAtOnce() {
    failTimes("rina", 4);
    assertThat(throttle.waitBefore("rina")).isEmpty();
  }

  @Test
  void fromTheFifthFailureEachRetryWaitsTwiceAsLong() {
    failTimes("rina", 5);
    assertThat(throttle.waitBefore("rina")).contains(Duration.ofSeconds(1));

    clock.advance(Duration.ofSeconds(1));
    throttle.failed("rina");
    assertThat(throttle.waitBefore("rina")).contains(Duration.ofSeconds(2));

    clock.advance(Duration.ofSeconds(2));
    throttle.failed("rina");
    assertThat(throttle.waitBefore("rina")).contains(Duration.ofSeconds(4));
  }

  @Test
  void theWaitCountsFromTheLastFailure() {
    failTimes("rina", 7);
    clock.advance(Duration.ofSeconds(3));

    assertThat(throttle.waitBefore("rina")).contains(Duration.ofSeconds(1));
    clock.advance(Duration.ofSeconds(1));
    assertThat(throttle.waitBefore("rina")).isEmpty();
  }

  @Test
  void theWaitNeverExceedsFifteenMinutes() {
    failTimes("rina", 30);
    assertThat(throttle.waitBefore("rina")).contains(Duration.ofMinutes(15));
  }

  @Test
  void anHourWithoutAFailureForgetsTheEarlierOnes() {
    failTimes("rina", 30);
    clock.advance(Duration.ofHours(1));

    throttle.failed("rina");
    assertThat(throttle.waitBefore("rina")).isEmpty();
  }

  @Test
  void aSuccessfulLoginForgetsTheFailures() {
    failTimes("rina", 5);
    clock.advance(Duration.ofSeconds(1));
    throttle.succeeded("rina");

    throttle.failed("rina");
    assertThat(throttle.waitBefore("rina")).isEmpty();
  }

  @Test
  void eachLoginNameIsSlowedDownOnItsOwnWhateverItsCase() {
    failTimes("rina", 5);

    assertThat(throttle.waitBefore("RINA")).contains(Duration.ofSeconds(1));
    assertThat(throttle.waitBefore("sari")).isEmpty();
  }

  private void failTimes(String loginName, int times) {
    for (int i = 0; i < times; i++) {
      throttle.failed(loginName);
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
