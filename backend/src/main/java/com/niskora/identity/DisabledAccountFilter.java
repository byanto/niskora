package com.niskora.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ends the open sessions of a disabled account at once (PLT-45). Every request of a session asks
 * whether its account may still use the app, so a session that outlives its account being disabled
 * ends on its next request, which is then answered as one without a session. The question is one
 * lookup by primary key, and holds however many sessions or machines there are.
 */
final class DisabledAccountFilter extends OncePerRequestFilter {

  private final UserRepository users;
  private final SecurityContextHolderStrategy contextHolder =
      SecurityContextHolder.getContextHolderStrategy();

  DisabledAccountFilter(UserRepository users) {
    this.users = users;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication authentication = contextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof LoggedInUser user
        && !users.existsByIdAndDisabledFalse(user.id())) {
      HttpSession session = request.getSession(false);
      if (session != null) {
        session.invalidate();
      }
      contextHolder.clearContext();
    }
    chain.doFilter(request, response);
  }
}
