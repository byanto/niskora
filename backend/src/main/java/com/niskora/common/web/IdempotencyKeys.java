package com.niskora.common.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * The stored {@code Idempotency-Key}s (ENG-86), kept for {@link #KEPT} after their claim. Every
 * statement but the hourly clean-up runs in the transaction of the request that sent the key, so a
 * key, the request's work and its answer commit together or not at all.
 */
class IdempotencyKeys {

  static final Duration KEPT = Duration.ofHours(24);

  private static final TypeReference<Map<String, String>> HEADERS = new TypeReference<>() {};

  private final JdbcClient jdbc;
  private final JsonMapper json;
  private final Clock clock;

  IdempotencyKeys(JdbcClient jdbc, JsonMapper json, Clock clock) {
    this.jdbc = jdbc;
    this.json = json;
    this.clock = clock;
  }

  /** What a request with a key meets. */
  sealed interface Claim {}

  /** The key is this request's: it runs, and its answer is then stored. */
  record Claimed() implements Claim {}

  /** The same request ran before, and this is what it answered. */
  record Answered(StoredAnswer answer) implements Claim {}

  /** The same request is still running. */
  record Running() implements Claim {}

  /** The key was used for a different request. */
  record Reused() implements Claim {}

  /** An answer as it is replayed: its status, the headers that matter and its body. */
  static final class StoredAnswer {

    private final int status;
    private final Map<String, String> headers;
    private final byte[] body;

    StoredAnswer(int status, Map<String, String> headers, byte[] body) {
      this.status = status;
      this.headers = Map.copyOf(headers);
      this.body = body.clone();
    }

    int status() {
      return status;
    }

    Map<String, String> headers() {
      return headers;
    }

    byte[] body() {
      return body.clone();
    }
  }

  /**
   * Claims a key in the caller's transaction (ENG-86). It first takes the key's advisory lock
   * without waiting: a request that cannot take it meets another request with the same key that is
   * still running. Holding it, the claim is a single insert, which the primary key lets only one
   * request win. An expired key is claimed afresh by the same statement. A request that loses is
   * told what the winner answered, that it is still running, or that the key was used for another
   * request.
   */
  Claim claim(String owner, UUID key, byte[] requestHash) {
    boolean locked =
        Boolean.TRUE.equals(
            jdbc.sql("SELECT pg_try_advisory_xact_lock(hashtext(:owner), hashtext(:key))")
                .param("owner", owner)
                .param("key", key.toString())
                .query(Boolean.class)
                .single());
    if (!locked) {
      return new Running();
    }
    Instant now = clock.instant();
    int claimed =
        jdbc.sql(
                """
                INSERT INTO common.idempotency_keys (owner, key, request_hash, claimed_at)
                VALUES (:owner, :key, :hash, :now)
                ON CONFLICT (owner, key) DO UPDATE
                SET request_hash = EXCLUDED.request_hash,
                    claimed_at = EXCLUDED.claimed_at,
                    response_status = NULL,
                    response_headers = NULL,
                    response_body = NULL
                WHERE common.idempotency_keys.claimed_at <= :expiredAt
                """)
            .param("owner", owner)
            .param("key", key)
            .param("hash", requestHash)
            .param("now", utc(now))
            .param("expiredAt", utc(now.minus(KEPT)))
            .update();
    if (claimed == 1) {
      return new Claimed();
    }
    Optional<Claim> found =
        jdbc.sql(
                """
                SELECT request_hash, response_status, response_headers::text AS headers,
                       response_body
                FROM common.idempotency_keys
                WHERE owner = :owner AND key = :key
                """)
            .param("owner", owner)
            .param("key", key)
            .<Claim>query((row, number) -> {
              if (!Arrays.equals(row.getBytes("request_hash"), requestHash)) {
                return new Reused();
              }
              int status = row.getInt("response_status");
              String headers = row.getString("headers");
              byte[] body = row.getBytes("response_body");
              // A row commits only with its answer, so a row without one is never seen.
              if (headers == null || body == null) {
                throw new IllegalStateException("A stored Idempotency-Key has no answer.");
              }
              return new Answered(new StoredAnswer(status, json.readValue(headers, HEADERS), body));
            })
            .optional();
    // Holding the lock, the key is either claimed above or stored with its answer.
    return found.orElseThrow(
        () -> new IllegalStateException("A locked Idempotency-Key was neither new nor stored."));
  }

  /** Stores the answer of the request that claimed the key, to commit with its work. */
  void answer(String owner, UUID key, StoredAnswer answer) {
    jdbc.sql(
            """
            UPDATE common.idempotency_keys
            SET response_status = :status,
                response_headers = CAST(:headers AS jsonb),
                response_body = :body
            WHERE owner = :owner AND key = :key
            """)
        .param("owner", owner)
        .param("key", key)
        .param("status", answer.status())
        .param("headers", json.writeValueAsString(answer.headers()))
        .param("body", answer.body())
        .update();
  }

  /** Forgets the keys whose 24 hours have passed, every hour. */
  @Scheduled(fixedDelay = 1, initialDelay = 1, timeUnit = TimeUnit.HOURS)
  void forgetExpired() {
    jdbc.sql("DELETE FROM common.idempotency_keys WHERE claimed_at <= :expiredAt")
        .param("expiredAt", utc(clock.instant().minus(KEPT)))
        .update();
  }

  /** A moment as the PostgreSQL driver takes it for a timestamptz. */
  private static OffsetDateTime utc(Instant moment) {
    return moment.atOffset(ZoneOffset.UTC);
  }
}
