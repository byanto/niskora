package com.niskora.common.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Puts the backend's build id on every response, so the frontend can tell when a new version is
 * running (ENG-92). The header is set before the request goes on, so it is there however the
 * response ends, an error included.
 */
class BuildIdFilter implements Filter {

  static final String HEADER = "Niskora-Build-Id";

  private final String buildId;

  BuildIdFilter(String buildId) {
    this.buildId = buildId;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    ((HttpServletResponse) response).setHeader(HEADER, buildId);
    chain.doFilter(request, response);
  }
}
