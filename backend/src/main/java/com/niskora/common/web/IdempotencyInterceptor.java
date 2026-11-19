package com.niskora.common.web;

import com.niskora.common.Problems;
import com.niskora.common.WithoutIdempotencyKey;
import com.niskora.common.web.IdempotencyFilter.BufferedBody;
import com.niskora.common.web.IdempotencyKeys.Answered;
import com.niskora.common.web.IdempotencyKeys.Claim;
import com.niskora.common.web.IdempotencyKeys.Claimed;
import com.niskora.common.web.IdempotencyKeys.Reused;
import com.niskora.common.web.IdempotencyKeys.Running;
import com.niskora.common.web.IdempotencyKeys.StoredAnswer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * Repeated requests (ENG-85, ENG-86). Every {@code POST} to a controller requires an {@code
 * Idempotency-Key}, a UUID the frontend makes once per user intent and sends again on a retry,
 * unless the method is marked {@link WithoutIdempotencyKey}.
 *
 * <p>A request with a key runs in one transaction: the claim of its key, its own work, which joins
 * that transaction, and, when it succeeds, its answer commit together or not at all. So a success
 * is never stored without its work, nor its work without the answer a repeat is given.
 *
 * <ul>
 *   <li>A repeat of a success is answered from the store and runs nothing.
 *   <li>A repeat that arrives while the first still runs gets a 409, and a key reused for a
 *       different request a 422.
 *   <li>A request that fails, refused (4xx) or broken (5xx), rolls back and stores nothing, so a
 *       repeat runs again: safe, since the failed one changed nothing. So does a request whose
 *       commit fails, which is answered with a 500 instead of what it was about to answer.
 *   <li>Should the backend stop while a request runs, its transaction rolls back with it, and its
 *       key is free at once.
 * </ul>
 *
 * <p>Work outside the database, such as a file copied to Object Storage, is not rolled back.
 */
class IdempotencyInterceptor implements HandlerInterceptor {

  static final String HEADER = "Idempotency-Key";

  /** The headers a repeat gets again with the stored body. */
  private static final List<String> REPLAYED_HEADERS =
      List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.LOCATION, HttpHeaders.ETAG);

  private static final String CLAIM = IdempotencyInterceptor.class.getName() + ".claim";

  private static final Logger log = LoggerFactory.getLogger(IdempotencyInterceptor.class);

  private final IdempotencyKeys keys;
  private final PlatformTransactionManager transactions;
  private final JsonMapper json;

  IdempotencyInterceptor(
      IdempotencyKeys keys, PlatformTransactionManager transactions, JsonMapper json) {
    this.keys = keys;
    this.transactions = transactions;
    this.json = json;
  }

  /** A key, whose it is, and the transaction of the request that claimed it. */
  private record OwnKey(String owner, UUID key, TransactionStatus transaction) {}

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws IOException {
    if (!(handler instanceof HandlerMethod method) || !requiresKey(method)) {
      return true;
    }
    String owner = Objects.requireNonNullElse(request.getRemoteUser(), "");
    UUID key = keyOf(request);
    byte[] hash = hash(request);
    // The request's own work joins this transaction; afterCompletion ends it.
    TransactionStatus transaction =
        transactions.getTransaction(TransactionDefinition.withDefaults());
    boolean claimed = false;
    try {
      Claim claim = keys.claim(owner, key, hash);
      return switch (claim) {
        case Claimed first -> {
          request.setAttribute(CLAIM, new OwnKey(owner, key, transaction));
          claimed = true;
          yield true;
        }
        case Answered answered -> {
          replay(answered.answer(), response);
          yield false;
        }
        case Running running ->
            throw refused(
                HttpStatus.CONFLICT,
                "REQUEST_IN_PROGRESS",
                "The first request with this Idempotency-Key is still running.");
        case Reused reused ->
            throw refused(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "IDEMPOTENCY_KEY_REUSED",
                "This Idempotency-Key was used for a different request.");
      };
    } finally {
      if (!claimed) {
        transactions.rollback(transaction);
      }
    }
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      @Nullable Exception exception) {
    if (!(request.getAttribute(CLAIM) instanceof OwnKey own)) {
      return;
    }
    ContentCachingResponseWrapper answer =
        WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
    if (answer == null || exception != null || answer.getStatus() >= 400) {
      transactions.rollback(own.transaction());
      return;
    }
    try {
      keys.answer(own.owner(), own.key(), stored(answer));
      transactions.commit(own.transaction());
    } catch (RuntimeException failed) {
      log.error("A request's work and its answer could not be committed", failed);
      if (!own.transaction().isCompleted()) {
        transactions.rollback(own.transaction());
      }
      // Nothing was kept, so the caller must not be told it was.
      answerServerError(request, answer);
    }
  }

  /** The answer as a repeat is given it: its status, the headers that matter and its body. */
  private static StoredAnswer stored(ContentCachingResponseWrapper answer) {
    Map<String, String> headers = new LinkedHashMap<>();
    for (String name : REPLAYED_HEADERS) {
      String value = answer.getHeader(name);
      if (value != null) {
        headers.put(name, value);
      }
    }
    return new StoredAnswer(answer.getStatus(), headers, answer.getContentAsByteArray());
  }

  /**
   * Replaces an answer that has not left yet with a 500, as the error page words one (ENG-65). The
   * headers that describe the replaced body go; the others, such as the build id and cookies, stay.
   */
  private void answerServerError(HttpServletRequest request, ContentCachingResponseWrapper answer) {
    if (answer.isCommitted()) {
      return;
    }
    Map<String, List<String>> kept = new LinkedHashMap<>();
    for (String name : answer.getHeaderNames()) {
      if (!DESCRIBE_THE_BODY.contains(name.toLowerCase(Locale.ROOT))) {
        kept.put(name, List.copyOf(answer.getHeaders(name)));
      }
    }
    answer.reset();
    kept.forEach((name, values) -> values.forEach(value -> answer.addHeader(name, value)));
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    Map<String, Object> problem = new LinkedHashMap<>();
    problem.put("type", "about:blank");
    problem.put("title", status.getReasonPhrase());
    problem.put("status", status.value());
    problem.put("detail", ProblemDetailsHandler.UNEXPECTED_DETAIL);
    problem.put("instance", request.getRequestURI());
    problem.put(Problems.CODE, Problems.codeOf(status));
    answer.setStatus(status.value());
    answer.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    try {
      answer.getOutputStream().write(json.writeValueAsBytes(problem));
    } catch (IOException unwritable) {
      throw new UncheckedIOException(unwritable);
    }
  }

  /** The headers of an answer that belong to its body, in lower case. */
  private static final Set<String> DESCRIBE_THE_BODY =
      Set.of("content-type", "content-length", "location", "etag");

  /**
   * Whether a controller method requires an {@code Idempotency-Key}: every {@code POST} but one
   * marked {@link WithoutIdempotencyKey}. The OpenAPI document names the header by the same rule.
   */
  static boolean requiresKey(HandlerMethod method) {
    RequestMapping mapping =
        AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequestMapping.class);
    return mapping != null
        && Arrays.asList(mapping.method()).contains(RequestMethod.POST)
        && !method.hasMethodAnnotation(WithoutIdempotencyKey.class);
  }

  private static UUID keyOf(HttpServletRequest request) {
    String key = request.getHeader(HEADER);
    if (key != null) {
      try {
        return UUID.fromString(key);
      } catch (IllegalArgumentException notAUuid) {
        // Refused below like a missing one.
      }
    }
    throw refused(
        HttpStatus.BAD_REQUEST,
        "IDEMPOTENCY_KEY_MISSING",
        "This request requires an Idempotency-Key header holding a UUID.");
  }

  /** SHA-256 of what makes two requests the same: the method, the path with its query, the body. */
  private static byte[] hash(HttpServletRequest request) {
    BufferedBody buffered = WebUtils.getNativeRequest(request, BufferedBody.class);
    if (buffered == null) {
      throw new IllegalStateException("IdempotencyFilter did not buffer this request's body.");
    }
    String query = request.getQueryString();
    String target = request.getRequestURI() + (query == null ? "" : "?" + query);
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update((request.getMethod() + " " + target + "\n").getBytes(StandardCharsets.UTF_8));
      digest.update(buffered.body());
      return digest.digest();
    } catch (NoSuchAlgorithmException missing) {
      throw new IllegalStateException("Every Java runtime has SHA-256.", missing);
    }
  }

  private static void replay(StoredAnswer answer, HttpServletResponse response) throws IOException {
    response.setStatus(answer.status());
    answer.headers().forEach(response::setHeader);
    response.getOutputStream().write(answer.body());
  }

  private static ErrorResponseException refused(HttpStatus status, String code, String detail) {
    return new ErrorResponseException(status, Problems.of(status, code, detail), null);
  }
}
