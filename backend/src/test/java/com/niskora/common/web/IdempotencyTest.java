package com.niskora.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.niskora.DatabaseTest;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Repeated requests (ENG-85, ENG-86): every {@code POST} under {@code /api} requires an {@code
 * Idempotency-Key}, and a repeat of it is answered from what the first one answered, against the
 * real database. A test controller counts how often its action really runs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DatabaseTest
@Import({IdempotencyTest.Orders.class, IdempotencyTest.Time.class})
class IdempotencyTest {

  private static final String ORDERS = "/api/test/orders";
  private static final Instant START = Instant.parse("2026-11-19T03:00:00Z");

  @Autowired MockMvc mvc;

  @Autowired Orders orders;

  @Autowired SettableClock clock;

  @BeforeEach
  void reset() {
    orders.reset();
    clock.set(START);
  }

  @Test
  void aPostWithoutAKeyIsABadRequestAndDoesNothing() throws Exception {
    mvc.perform(
            post(ORDERS)
                .with(user("rina"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(order(3)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_MISSING"));

    assertThat(orders.placed()).isZero();
  }

  @Test
  void aKeyThatIsNotAUuidIsABadRequest() throws Exception {
    send("rina", "not-a-uuid", ORDERS, order(3))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_MISSING"));

    assertThat(orders.placed()).isZero();
  }

  @Test
  void aRepeatIsAnsweredWithTheFirstAnswerAndDoesNothingAgain() throws Exception {
    String key = UUID.randomUUID().toString();
    send("rina", key, ORDERS, order(3))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/test/orders/1"))
        .andExpect(jsonPath("$.number").value(1));

    send("rina", key, ORDERS, order(3))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/test/orders/1"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("{\"number\": 1, \"quantity\": 3}", JsonCompareMode.STRICT));

    assertThat(orders.placed()).isEqualTo(1);
  }

  @Test
  void aNewKeyIsANewIntent() throws Exception {
    send("rina", UUID.randomUUID().toString(), ORDERS, order(3)).andExpect(status().isCreated());
    send("rina", UUID.randomUUID().toString(), ORDERS, order(3))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.number").value(2));

    assertThat(orders.placed()).isEqualTo(2);
  }

  @Test
  void aRepeatWhileTheFirstIsStillRunningIsAConflict() throws Exception {
    String key = UUID.randomUUID().toString();
    orders.holdNext();
    CompletableFuture<ResultActions> first =
        CompletableFuture.supplyAsync(() -> sendUnchecked("rina", key, ORDERS, order(3)));
    orders.awaitHeld();

    send("rina", key, ORDERS, order(3))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("REQUEST_IN_PROGRESS"));

    orders.release();
    first.get(10, TimeUnit.SECONDS).andExpect(status().isCreated());
    send("rina", key, ORDERS, order(3))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.number").value(1));
    assertThat(orders.placed()).isEqualTo(1);
  }

  @Test
  void aKeyReusedWithADifferentBodyIsRefused() throws Exception {
    String key = UUID.randomUUID().toString();
    send("rina", key, ORDERS, order(3)).andExpect(status().isCreated());

    send("rina", key, ORDERS, order(4))
        .andExpect(status().isUnprocessableContent())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));

    assertThat(orders.placed()).isEqualTo(1);
  }

  @Test
  void aKeyReusedOnAnotherPathIsRefused() throws Exception {
    String key = UUID.randomUUID().toString();
    send("rina", key, ORDERS, order(3)).andExpect(status().isCreated());

    send("rina", key, ORDERS + "/rush", order(3))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));

    assertThat(orders.placed()).isEqualTo(1);
  }

  @Test
  void eachUserHasTheirOwnKeys() throws Exception {
    String key = UUID.randomUUID().toString();
    send("rina", key, ORDERS, order(3)).andExpect(jsonPath("$.number").value(1));

    send("sari", key, ORDERS, order(3)).andExpect(jsonPath("$.number").value(2));

    assertThat(orders.placed()).isEqualTo(2);
  }

  @Test
  void aKeyIsKeptFor24Hours() throws Exception {
    String key = UUID.randomUUID().toString();
    send("rina", key, ORDERS, order(3)).andExpect(status().isCreated());

    clock.set(START.plus(Duration.ofHours(24)).minusSeconds(1));
    send("rina", key, ORDERS, order(3)).andExpect(jsonPath("$.number").value(1));
    clock.set(START.plus(Duration.ofHours(24)));
    send("rina", key, ORDERS, order(4))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.number").value(2));

    assertThat(orders.placed()).isEqualTo(2);
  }

  @Test
  void aRefusalIsAnsweredAgainToo() throws Exception {
    String key = UUID.randomUUID().toString();
    send("rina", key, ORDERS, order(0)).andExpect(status().isUnprocessableContent());

    send("rina", key, ORDERS, order(0))
        .andExpect(status().isUnprocessableContent())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("NOTHING_TO_ORDER"));

    assertThat(orders.attempts()).isEqualTo(1);
  }

  @Test
  void aServerErrorLetsTheSameKeyTryAgain() throws Exception {
    String key = UUID.randomUUID().toString();
    orders.failNext();
    send("rina", key, ORDERS, order(3)).andExpect(status().isInternalServerError());

    send("rina", key, ORDERS, order(3))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.number").value(1));

    assertThat(orders.placed()).isEqualTo(1);
  }

  private ResultActions send(String user, String key, String path, String json) throws Exception {
    return mvc.perform(
        post(path)
            .with(user(user))
            .with(csrf())
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
  }

  private ResultActions sendUnchecked(String user, String key, String path, String json) {
    try {
      return send(user, key, path, json);
    } catch (Exception failed) {
      throw new IllegalStateException(failed);
    }
  }

  private static String order(int quantity) {
    return "{\"quantity\": " + quantity + "}";
  }

  /** Places numbered orders, and can hold one back or fail one, to test what a repeat meets. */
  @RestController
  static class Orders {

    private final AtomicInteger placed = new AtomicInteger();
    private final AtomicInteger attempts = new AtomicInteger();
    private volatile boolean failNext;
    private volatile CountDownLatch held = new CountDownLatch(0);
    private volatile CountDownLatch release = new CountDownLatch(0);

    record OrderRequest(int quantity) {}

    record OrderResponse(int number, int quantity) {}

    @PostMapping({ORDERS, ORDERS + "/rush"})
    ResponseEntity<?> place(@RequestBody OrderRequest order) throws InterruptedException {
      attempts.incrementAndGet();
      held.countDown();
      if (!release.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("The held order was never released.");
      }
      if (failNext) {
        failNext = false;
        throw new IllegalStateException("The order failed.");
      }
      if (order.quantity() == 0) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(
                com.niskora.common.Problems.of(
                    HttpStatus.UNPROCESSABLE_CONTENT, "NOTHING_TO_ORDER", "Nothing to order."));
      }
      int number = placed.incrementAndGet();
      return ResponseEntity.created(URI.create(ORDERS + "/" + number))
          .body(new OrderResponse(number, order.quantity()));
    }

    void reset() {
      placed.set(0);
      attempts.set(0);
      failNext = false;
      held = new CountDownLatch(0);
      release = new CountDownLatch(0);
    }

    void holdNext() {
      held = new CountDownLatch(1);
      release = new CountDownLatch(1);
    }

    void awaitHeld() throws InterruptedException {
      assertThat(held.await(10, TimeUnit.SECONDS)).isTrue();
    }

    void release() {
      release.countDown();
    }

    void failNext() {
      failNext = true;
    }

    int placed() {
      return placed.get();
    }

    int attempts() {
      return attempts.get();
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class Time {

    @Bean
    @Primary
    SettableClock settableClock() {
      return new SettableClock();
    }
  }

  /** A clock a test sets, so a key's 24 hours pass at once. */
  static final class SettableClock extends Clock {

    private volatile Instant now = START;

    void set(Instant moment) {
      now = moment;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      throw new UnsupportedOperationException("The test clock is on UTC.");
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
