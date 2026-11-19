package com.niskora.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Hands out the CSRF token cookie on every response. Spring Security loads the token only when
 * something reads it, which a single page app never does on the server, so the frontend would
 * otherwise have no token to echo on its first change.
 */
final class CsrfCookieFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    handOutToken(request);
    chain.doFilter(request, response);
  }

  /** Reads the request's CSRF token, so Spring Security writes it to the cookie. */
  static void handOutToken(HttpServletRequest request) {
    if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) {
      token.getToken();
    }
  }
}
