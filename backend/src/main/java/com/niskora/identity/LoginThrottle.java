package com.niskora.identity;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Slows down repeated failed logins (PLT-24), per login name whatever its case. The first {@value
 * #FREE_FAILURES} failures may be retried at once. From then on each retry waits twice as long as
 * the one before, counted from the last failure: 1 second, then 2, 4 and so on up to {@link
 * #LONGEST_WAIT}. A successful login, or an hour without a failure, forgets them.
 *
 * <p>The failures are kept in memory only, so a restart forgets them too. Only the most recent
 * {@value #TRACKED_NAMES} login names are kept, so made-up names cannot fill the memory.
 */
final class LoginThrottle {

  static final int FREE_FAILURES = 5;
  static final Duration LONGEST_WAIT = Duration.ofMinutes(15);
  static final Duration FORGOTTEN_AFTER = Duration.ofHours(1);
  static final int TRACKED_NAMES = 10_000;

  private record Failures(int count, Instant last) {}

  private final Clock clock;

  private final Map<String, Failures> failures =
      new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Failures> eldest) {
          return size() > TRACKED_NAMES;
        }
      };

  LoginThrottle(Clock clock) {
    this.clock = clock;
  }

  /** How long a login with this name must wait before it is tried, or empty when it may be now. */
  synchronized Optional<Duration> waitBefore(String loginName) {
    Failures known = current(key(loginName));
    if (known == null || known.count() < FREE_FAILURES) {
      return Optional.empty();
    }
    Duration remaining = Duration.between(clock.instant(), known.last().plus(waitAfter(known)));
    return remaining.isPositive() ? Optional.of(remaining) : Optional.empty();
  }

  synchronized void failed(String loginName) {
    String key = key(loginName);
    Failures known = current(key);
    failures.put(key, new Failures(known == null ? 1 : known.count() + 1, clock.instant()));
  }

  synchronized void succeeded(String loginName) {
    failures.remove(key(loginName));
  }

  /** The failures of a login name, unless an hour has passed since the last one. */
  private @Nullable Failures current(String key) {
    Failures known = failures.get(key);
    if (known != null && !clock.instant().isBefore(known.last().plus(FORGOTTEN_AFTER))) {
      failures.remove(key);
      return null;
    }
    return known;
  }

  private static Duration waitAfter(Failures known) {
    int doublings = known.count() - FREE_FAILURES;
    // 2^10 seconds is already past the longest wait, so the shift never overflows.
    Duration wait = Duration.ofSeconds(1L << Math.min(doublings, 10));
    return wait.compareTo(LONGEST_WAIT) > 0 ? LONGEST_WAIT : wait;
  }

  private static String key(String loginName) {
    return loginName.toLowerCase(Locale.ROOT);
  }
}
