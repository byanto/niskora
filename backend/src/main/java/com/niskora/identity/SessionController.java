package com.niskora.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The session of the user in this browser: who is logged in (FE-71), logging in and logging out. It
 * is one resource, so its path is singular.
 */
@RestController
@RequestMapping(path = SessionController.PATH, produces = MediaType.APPLICATION_JSON_VALUE)
class SessionController {

  static final String PATH = "/api/identity/session";

  private final Logins logins;

  SessionController(Logins logins) {
    this.logins = logins;
  }

  /** Who is logged in. Nobody is a 401, like any other call without a session. */
  @GetMapping
  Session currentSession(@AuthenticationPrincipal LoggedInUser user) {
    return Session.of(user);
  }

  /**
   * Logs in with a login name and a password, and starts a new session. A wrong login name or
   * password is a 401, and an attempt while failed logins are slowed down is a 429 (PLT-24).
   */
  @PostMapping
  Session logIn(
      @Valid @RequestBody LoginRequest login,
      HttpServletRequest request,
      HttpServletResponse response) {
    return Session.of(logins.logIn(login.loginName(), login.password(), request, response));
  }

  /** Logs out: the session ends. Logging out without a session is a 401. */
  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void logOut(HttpServletRequest request, HttpServletResponse response) {
    logins.logOut(request, response);
  }

  record LoginRequest(@NotBlank String loginName, @NotBlank String password) {}

  /** The session of the logged-in user. */
  record Session(SessionUser user) {

    static Session of(LoggedInUser user) {
      return new Session(new SessionUser(user.id(), user.loginName(), user.fullName()));
    }
  }

  /**
   * The logged-in user: the login name they log in with and their full name, which the app shows.
   */
  record SessionUser(UUID id, String loginName, String fullName) {}
}
