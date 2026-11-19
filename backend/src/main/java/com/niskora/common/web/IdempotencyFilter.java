package com.niskora.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Lets {@link IdempotencyInterceptor} see a {@code POST}'s whole body before the controller reads
 * it, and its whole answer before it leaves, so a repeat can be recognised and answered again. It
 * runs after Spring Security, so it knows who sent the request.
 */
class IdempotencyFilter extends OncePerRequestFilter {

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !HttpMethod.POST.matches(request.getMethod());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    ContentCachingResponseWrapper answer = new ContentCachingResponseWrapper(response);
    try {
      chain.doFilter(new BufferedBody(request), answer);
    } finally {
      answer.copyBodyToResponse();
    }
  }

  /** A request whose body is read in full at once, and can then be read again. */
  static final class BufferedBody extends HttpServletRequestWrapper {

    private final byte[] body;

    BufferedBody(HttpServletRequest request) throws IOException {
      super(request);
      this.body = request.getInputStream().readAllBytes();
    }

    byte[] body() {
      return body.clone();
    }

    @Override
    public ServletInputStream getInputStream() {
      ByteArrayInputStream bytes = new ByteArrayInputStream(body);
      return new ServletInputStream() {
        @Override
        public int read() {
          return bytes.read();
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
          return bytes.read(buffer, offset, length);
        }

        @Override
        public boolean isFinished() {
          return bytes.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
          throw new UnsupportedOperationException("The body is already read in full.");
        }
      };
    }

    @Override
    public BufferedReader getReader() {
      String encoding = getCharacterEncoding();
      Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
      return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
  }
}
