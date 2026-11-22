package com.niskora.identity;

import com.niskora.common.Problems;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * The security of every endpoint (PLT-17): a server-side session, CSRF protection for a single page
 * app, and Problem Details for a call that is not logged in (401) or lacks the CSRF token (403).
 * Every path needs a session except logging in.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

  /** Only when the backend serves: a command such as create-first-user runs without a server. */
  @Bean
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      CookieCsrfTokenRepository csrfTokenRepository,
      SecurityContextRepository securityContextRepository,
      UserRepository users,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
    http.authorizeHttpRequests(requests ->
            requests
                // The error page answers an error raised outside Spring MVC (ENG-65).
                .dispatcherTypeMatchers(DispatcherType.ERROR)
                .permitAll()
                // Actuator answers only on its internal port, which is never published, and the
                // proxy forwards only the health check there (ENG-68). This chain guards that port
                // too. The application's port has no Actuator path, so this opens nothing there.
                .requestMatchers("/actuator/**")
                .permitAll()
                .requestMatchers(HttpMethod.POST, SessionController.PATH)
                .permitAll()
                .anyRequest()
                .authenticated())
        // The token is in a cookie the frontend reads and echoes in a header (FE-73).
        .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
        .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
        .securityContext(context -> context.securityContextRepository(securityContextRepository))
        // Before the anonymous user stands in, so an ended session is a 401 like no session.
        .addFilterBefore(new DisabledAccountFilter(users), AnonymousAuthenticationFilter.class)
        .exceptionHandling(exceptions ->
            exceptions
                .authenticationEntryPoint((request, response, exception) ->
                    answer(
                        exceptionResolver,
                        request,
                        response,
                        Problems.of(HttpStatus.UNAUTHORIZED),
                        exception))
                .accessDeniedHandler((request, response, exception) ->
                    answer(
                        exceptionResolver,
                        request,
                        response,
                        exception instanceof CsrfException
                            ? Problems.of(
                                HttpStatus.FORBIDDEN,
                                "CSRF_TOKEN_INVALID",
                                "The CSRF token is missing or out of date.")
                            : Problems.of(HttpStatus.FORBIDDEN),
                        exception)))
        // Logging in and out are SessionController's; nothing else answers with a page.
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable);
    return http.build();
  }

  /** Answers through Spring MVC's error handling, so it is Problem Details like any other. */
  private static void answer(
      HandlerExceptionResolver resolver,
      HttpServletRequest request,
      HttpServletResponse response,
      ProblemDetail problem,
      Exception cause) {
    ErrorResponseException error =
        new ErrorResponseException(HttpStatus.valueOf(problem.getStatus()), problem, cause);
    resolver.resolveException(request, response, null, error);
  }

  /**
   * The CSRF token in a cookie that is {@code Secure} and {@code SameSite=Lax} like the session's,
   * and readable by the frontend, which echoes it in the {@code X-XSRF-TOKEN} header.
   */
  @Bean
  CookieCsrfTokenRepository csrfTokenRepository() {
    CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repository.setCookiePath("/");
    repository.setCookieCustomizer(cookie -> cookie.secure(true).sameSite("Lax"));
    return repository;
  }

  /** The logged-in user is kept in the server-side session (PLT-17). */
  @Bean
  SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  /**
   * Argon2 with Spring Security's current defaults (PLT-17). Each hash is prefixed with the id of
   * its encoder, so the parameters can be raised later without locking anyone out.
   */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new DelegatingPasswordEncoder(
        "argon2", Map.of("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
  }

  @Bean
  UserDetailsService userDetailsService(UserRepository users) {
    return loginName ->
        users
            .findByLoginName(loginName)
            .map(LoggedInUser::of)
            .orElseThrow(() -> new UsernameNotFoundException("No user has this login name."));
  }

  @Bean
  LoginThrottle loginThrottle(Clock clock) {
    return new LoginThrottle(clock);
  }
}
