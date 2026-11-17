package com.niskora.common.web;

import com.niskora.common.Problems;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Answers every error raised in Spring MVC with Problem Details and a stable code (ENG-65).
 * Spring's own errors (an unknown path, a wrong method, an unreadable body) and a module's {@code
 * ErrorResponseException} come through {@link ResponseEntityExceptionHandler}. Anything else is a
 * bug: it is logged and answered with a 500 that does not reveal its message.
 */
@RestControllerAdvice
class ProblemDetailsHandler extends ResponseEntityExceptionHandler {

  static final String UNEXPECTED_DETAIL = "An unexpected error occurred.";

  private static final Logger log = LoggerFactory.getLogger(ProblemDetailsHandler.class);

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
    log.error("Unhandled error", exception);
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    return ResponseEntity.status(status)
        .body(Problems.of(status, Problems.codeOf(status), UNEXPECTED_DETAIL));
  }

  /** Spring words an unknown path as a missing static resource, which means nothing to a caller. */
  @Override
  protected @Nullable ResponseEntity<Object> handleNoResourceFoundException(
      NoResourceFoundException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    exception.getBody().setDetail("Nothing is found at this path.");
    return super.handleNoResourceFoundException(exception, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> createResponseEntity(
      @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
    if (body instanceof ProblemDetail problem
        && (problem.getProperties() == null
            || !problem.getProperties().containsKey(Problems.CODE))) {
      problem.setProperty(Problems.CODE, Problems.codeOf(statusCode));
    }
    return super.createResponseEntity(body, headers, statusCode, request);
  }
}
