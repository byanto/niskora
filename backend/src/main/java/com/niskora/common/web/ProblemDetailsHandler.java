package com.niskora.common.web;

import com.niskora.common.FieldProblem;
import com.niskora.common.Problems;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

  /**
   * A request body that fails shape validation is a 400 with every field error at once (ENG-82), in
   * the order of their pointers, so the answer is the same each time.
   */
  @Override
  protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    Stream<FieldProblem> fields =
        exception.getFieldErrors().stream()
            .map(error -> fieldProblem(pointer(error.getField()), error));
    Stream<FieldProblem> whole =
        exception.getGlobalErrors().stream().map(error -> fieldProblem("", error));
    List<FieldProblem> errors =
        Stream.concat(fields, whole).sorted(Comparator.comparing(FieldProblem::pointer)).toList();
    return handleExceptionInternal(
        exception, Problems.ofFields(status, errors), headers, status, request);
  }

  private static FieldProblem fieldProblem(String pointer, ObjectError error) {
    String constraint = error.getCode();
    String code =
        constraint != null && REQUIRED_CONSTRAINTS.contains(constraint)
            ? FieldProblem.REQUIRED
            : "INVALID";
    String detail = error.getDefaultMessage();
    return new FieldProblem(pointer, code, detail == null ? code : detail);
  }

  /** The Bean Validation constraints that mean a field must be given. */
  private static final Set<String> REQUIRED_CONSTRAINTS = Set.of("NotNull", "NotBlank", "NotEmpty");

  /** A field's path as Spring names it, {@code lines[2].quantity}, as a JSON Pointer. */
  static String pointer(String path) {
    StringBuilder pointer = new StringBuilder();
    for (String token : path.replace("[", ".").replace("]", "").split("\\.", -1)) {
      pointer.append('/').append(token.replace("~", "~0").replace("/", "~1"));
    }
    return pointer.toString();
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
