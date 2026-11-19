package com.niskora.identity;

import com.niskora.common.Problems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;

/**
 * Logging in and out of the server-side session (PLT-17). Spring Security's own login form is off,
 * so this does what it would: check the password, slow down repeated failures (PLT-24), give the
 * session a new id and the browser a new CSRF token, so nothing from before the login carries over,
 * and keep the logged-in user in the session.
 */
@Component
class Logins {

  private final AuthenticationManager authenticationManager;
  private final LoginThrottle throttle;
  private final SessionAuthenticationStrategy sessionStrategy;
  private final SecurityContextRepository contextRepository;
  private final CookieCsrfTokenRepository csrfTokenRepository;
  private final SecurityContextHolderStrategy contextHolder =
      SecurityContextHolder.getContextHolderStrategy();

  Logins(
      UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder,
      LoginThrottle throttle,
      SecurityContextRepository contextRepository,
      CookieCsrfTokenRepository csrfTokenRepository) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    this.authenticationManager = new ProviderManager(provider);
    this.throttle = throttle;
    this.sessionStrategy =
        new CompositeSessionAuthenticationStrategy(
            List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfTokenRepository)));
    this.contextRepository = contextRepository;
    this.csrfTokenRepository = csrfTokenRepository;
  }

  /** Logs in, or refuses with a 401 for a wrong password and a 429 while slowed down. */
  LoggedInUser logIn(
      String loginName, String password, HttpServletRequest request, HttpServletResponse response) {
    Optional<Duration> wait = throttle.waitBefore(loginName);
    if (wait.isPresent()) {
      throw tooManyAttempts(wait.get());
    }
    Authentication authentication;
    try {
      authentication =
          authenticationManager.authenticate(
              UsernamePasswordAuthenticationToken.unauthenticated(loginName, password));
    } catch (AuthenticationException failed) {
      throttle.failed(loginName);
      throw new ErrorResponseException(
          HttpStatus.UNAUTHORIZED,
          Problems.of(
              HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "The login name or password is wrong."),
          failed);
    }
    throttle.succeeded(loginName);

    sessionStrategy.onAuthentication(authentication, request, response);
    SecurityContext context = contextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    contextHolder.setContext(context);
    contextRepository.saveContext(context, request, response);
    handOutCsrfToken(request);
    // The provider's principal is the LoggedInUser that userDetailsService loaded.
    return (LoggedInUser) Objects.requireNonNull(authentication.getPrincipal());
  }

  /** Ends the session, and gives the browser a new CSRF token for the next login. */
  void logOut(HttpServletRequest request, HttpServletResponse response) {
    SecurityContextLogoutHandler logout = new SecurityContextLogoutHandler();
    logout.setSecurityContextRepository(contextRepository);
    logout.logout(request, response, contextHolder.getContext().getAuthentication());
    csrfTokenRepository.saveToken(csrfTokenRepository.generateToken(request), request, response);
  }

  /** The CSRF strategy replaced the token, which Spring Security writes only once it is read. */
  private static void handOutCsrfToken(HttpServletRequest request) {
    if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) {
      token.getToken();
    }
  }

  private static ErrorResponseException tooManyAttempts(Duration wait) {
    ErrorResponseException refused =
        new ErrorResponseException(
            HttpStatus.TOO_MANY_REQUESTS,
            Problems.of(
                HttpStatus.TOO_MANY_REQUESTS,
                "TOO_MANY_LOGIN_ATTEMPTS",
                "Too many failed logins with this login name. Wait before trying again."),
            null);
    // Whole seconds, rounded up, so a retry at that time is never early.
    long seconds = wait.plusNanos(999_999_999).toSeconds();
    refused.getHeaders().set(HttpHeaders.RETRY_AFTER, Long.toString(seconds));
    return refused;
  }
}
