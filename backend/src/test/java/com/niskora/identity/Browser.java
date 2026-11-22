package com.niskora.identity;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Calls the API the way the frontend does in a browser: it keeps the session and the CSRF token
 * cookie between calls, and echoes the token in a header on every changing request (FE-73).
 */
final class Browser {

  static final String CSRF_COOKIE = "XSRF-TOKEN";
  static final String CSRF_HEADER = "X-XSRF-TOKEN";

  private final MockMvc mvc;
  private @Nullable MockHttpSession session;
  private @Nullable String csrfToken;

  Browser(MockMvc mvc) {
    this.mvc = mvc;
  }

  /** Opens the app, which asks who is logged in and so gets the CSRF token. */
  Browser visit() throws Exception {
    get("/api/identity/session");
    return this;
  }

  ResultActions get(String path) throws Exception {
    return perform(MockMvcRequestBuilders.get(path), false);
  }

  ResultActions post(String path, String json) throws Exception {
    return perform(
        MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON).content(json),
        true);
  }

  /** A change that requires an {@code Idempotency-Key} (ENG-85), sent with the given one. */
  ResultActions post(String path, String json, UUID idempotencyKey) throws Exception {
    return perform(
        MockMvcRequestBuilders.post(path)
            .header("Idempotency-Key", idempotencyKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json),
        true);
  }

  /**
   * A change to an editable resource (ENG-70, ENG-87): a {@code POST} with the given {@code
   * Idempotency-Key}, the {@code If-Match} unless it is null, and the body unless it is null.
   */
  ResultActions change(
      String path, @Nullable String json, @Nullable String ifMatch, UUID idempotencyKey)
      throws Exception {
    MockHttpServletRequestBuilder request =
        MockMvcRequestBuilders.post(path).header("Idempotency-Key", idempotencyKey.toString());
    if (ifMatch != null) {
      request.header("If-Match", ifMatch);
    }
    if (json != null) {
      request.contentType(MediaType.APPLICATION_JSON).content(json);
    }
    return perform(request, true);
  }

  /** A change as above, with a key of its own. */
  ResultActions change(String path, @Nullable String json, @Nullable String ifMatch)
      throws Exception {
    return change(path, json, ifMatch, UUID.randomUUID());
  }

  ResultActions postWithoutCsrfToken(String path, String json) throws Exception {
    return perform(
        MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON).content(json),
        false);
  }

  ResultActions delete(String path) throws Exception {
    return perform(MockMvcRequestBuilders.delete(path), true);
  }

  ResultActions deleteWithoutCsrfToken(String path) throws Exception {
    return perform(MockMvcRequestBuilders.delete(path), false);
  }

  /** Starts from a session id the browser was given, as an attacker might plant one. */
  Browser withSession(MockHttpSession given) {
    session = given;
    return this;
  }

  String csrfToken() {
    return Objects.requireNonNull(csrfToken, "no CSRF token cookie was handed out");
  }

  String sessionId() {
    return Objects.requireNonNull(session, "no session was started").getId();
  }

  private ResultActions perform(MockHttpServletRequestBuilder request, boolean echoCsrfToken)
      throws Exception {
    MockHttpSession current = session;
    if (current != null) {
      // As if the browser sent the session cookie, which MockMvc does not model by itself.
      request
          .session(current)
          .with(sent -> {
            sent.setRequestedSessionId(current.getId());
            sent.setRequestedSessionIdValid(true);
            return sent;
          });
    }
    if (csrfToken != null) {
      request.cookie(new Cookie(CSRF_COOKIE, csrfToken));
      if (echoCsrfToken) {
        request.header(CSRF_HEADER, csrfToken);
      }
    }
    ResultActions actions = mvc.perform(request);
    remember(actions.andReturn());
    return actions;
  }

  private void remember(MvcResult result) {
    HttpSession current = result.getRequest().getSession(false);
    session = current instanceof MockHttpSession mock && !mock.isInvalid() ? mock : null;
    // A response may clear the cookie and then set a new one; a browser keeps the last.
    for (Cookie cookie : result.getResponse().getCookies()) {
      if (cookie.getName().equals(CSRF_COOKIE)) {
        csrfToken = cookie.getValue().isEmpty() ? null : cookie.getValue();
      }
    }
  }
}
