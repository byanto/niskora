package com.niskora.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.niskora.DatabaseTest;
import com.niskora.identity.Users.UserRefusedException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * Users created at the same moment, against the real database (ENG-33). Hashing the password takes
 * a while between the checks and the insert, so without a guard both would get through. The
 * annotations match {@code FirstUserCommandTest}, so both share one context.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DatabaseTest
class UsersTest {

  private static final String PASSWORD = "correct horse battery";

  @Autowired Users users;

  @Autowired UserRepository repository;

  @Test
  void ofTwoFirstUsersCreatedAtTheSameMomentOnlyOneIsCreated() throws Exception {
    List<@Nullable Throwable> outcomes =
        atTheSameMoment(
            () -> users.createFirstUser("rina", "Rina Wulandari", PASSWORD),
            () -> users.createFirstUser("sari", "Sari Dewi", PASSWORD));

    assertThat(repository.count()).isEqualTo(1);
    assertThat(outcomes)
        .filteredOn(outcome -> outcome != null)
        .singleElement(InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(UserRefusedException.class)
        .hasMessage("A user already exists, so this is not the first one.");
  }

  @Test
  void aLoginNameTakenAtTheSameMomentIsRefused() throws Exception {
    List<@Nullable Throwable> outcomes =
        atTheSameMoment(
            () -> users.create("rina", "Rina Wulandari", PASSWORD),
            () -> users.create("RINA", "Rina Kusuma", PASSWORD));

    assertThat(repository.count()).isEqualTo(1);
    assertThat(outcomes)
        .filteredOn(outcome -> outcome != null)
        .singleElement(InstanceOfAssertFactories.THROWABLE)
        .isInstanceOf(UserRefusedException.class)
        .hasMessageEndingWith("is taken.");
  }

  /** Runs each at the same moment, and gives what each threw, or null when it went through. */
  private static List<@Nullable Throwable> atTheSameMoment(Runnable... actions)
      throws InterruptedException {
    CountDownLatch start = new CountDownLatch(1);
    List<Future<?>> running = new ArrayList<>();
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (Runnable action : actions) {
        running.add(
            executor.submit(() -> {
              start.await();
              action.run();
              return null;
            }));
      }
      start.countDown();
    }
    List<@Nullable Throwable> outcomes = new ArrayList<>();
    for (Future<?> done : running) {
      try {
        done.get();
        outcomes.add(null);
      } catch (ExecutionException failed) {
        outcomes.add(failed.getCause());
      }
    }
    return outcomes;
  }
}
