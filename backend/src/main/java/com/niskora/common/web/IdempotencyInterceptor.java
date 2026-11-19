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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

/**
 * Repeated requests (ENG-85, ENG-86). Every {@code POST} to a controller requires an {@code
 * Idempotency-Key}, a UUID the frontend makes once per user intent and sends again on a retry,
 * unless the method is marked {@link WithoutIdempotencyKey}.
 *
 * <p>The first request with a key claims it and runs, and its answer is stored. A repeat with the
 * same body is answered from the store and runs nothing; one that arrives while the first still
 * runs gets a 409, and a key reused for a different request a 422. A server error gives the claim
 * up, so a retry runs again. Should the backend stop between running the request and storing its
 * answer, the key stays claimed, and a repeat gets a 409 until the 24 hours have passed.
 */
class IdempotencyInterceptor implements HandlerInterceptor {

  static final String HEADER = "Idempotency-Key";

  /** The headers a repeat gets again with the stored body. */
  private static final List<String> REPLAYED_HEADERS =
      List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.LOCATION, HttpHeaders.ETAG);

  private static final String CLAIM = IdempotencyInterceptor.class.getName() + ".claim";

  private final IdempotencyKeys keys;

  IdempotencyInterceptor(IdempotencyKeys keys) {
    this.keys = keys;
  }

  /** The key a request claimed, and whose it is. */
  private record OwnKey(String owner, UUID key) {}

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws IOException {
    if (!HttpMethod.POST.matches(request.getMethod())
        || !(handler instanceof HandlerMethod method)
        || method.hasMethodAnnotation(WithoutIdempotencyKey.class)) {
      return true;
    }
    OwnKey own =
        new OwnKey(Objects.requireNonNullElse(request.getRemoteUser(), ""), keyOf(request));
    Claim claim = keys.claim(own.owner(), own.key(), hash(request));
    return switch (claim) {
      case Claimed claimed -> {
        request.setAttribute(CLAIM, own);
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
    if (answer == null || exception != null || answer.getStatus() >= 500) {
      keys.release(own.owner(), own.key());
      return;
    }
    Map<String, String> headers = new LinkedHashMap<>();
    for (String name : REPLAYED_HEADERS) {
      String value = answer.getHeader(name);
      if (value != null) {
        headers.put(name, value);
      }
    }
    keys.answer(
        own.owner(),
        own.key(),
        new StoredAnswer(answer.getStatus(), headers, answer.getContentAsByteArray()));
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
