package com.niskora.common;

import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * The error shape of the API: Problem Details (RFC 9457) with a stable error code in the {@value
 * #CODE} property, which the frontend maps to a translation key (ENG-65).
 *
 * <p>A module refuses a request by throwing Spring's {@code ErrorResponseException} with a problem
 * built here. An error that names no code of its own gets the name of its status, such as {@code
 * NOT_FOUND}.
 */
public final class Problems {

  /** The property that carries the stable error code. */
  public static final String CODE = "code";

  /** The property that carries the field errors of a validation error (ENG-82). */
  public static final String ERRORS = "errors";

  /** The code of a request whose fields are wrong, each named in {@value #ERRORS}. */
  public static final String VALIDATION_FAILED = "VALIDATION_FAILED";

  private Problems() {}

  /** A problem with the given status, stable error code and human-readable detail. */
  public static ProblemDetail of(HttpStatusCode status, String code, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setProperty(CODE, code);
    return problem;
  }

  /** A problem with the given status, its name as the code and no detail. */
  public static ProblemDetail of(HttpStatusCode status) {
    ProblemDetail problem = ProblemDetail.forStatus(status);
    problem.setProperty(CODE, codeOf(status));
    return problem;
  }

  /**
   * A validation error with every field error at once (ENG-82): a 400 for fields that fail shape
   * validation, and a 422 for fields that break a business rule (ENG-81).
   */
  public static ProblemDetail ofFields(HttpStatusCode status, List<FieldProblem> errors) {
    ProblemDetail problem =
        of(status, VALIDATION_FAILED, "One or more fields are wrong; see errors.");
    problem.setProperty(ERRORS, List.copyOf(errors));
    return problem;
  }

  /** The code of a change refused because the resource moved past the version it was based on. */
  public static final String VERSION_MISMATCH = "VERSION_MISMATCH";

  /**
   * A change refused with a 412 because its {@code If-Match} names an older version (ENG-87): it
   * says who made the latest change and when, so the screen can (FE-81). Who is {@code null} for a
   * change that no user made, such as the command that creates the first user.
   */
  public static ProblemDetail versionMismatch(Instant changedAt, @Nullable ChangedBy changedBy) {
    ProblemDetail problem =
        of(
            HttpStatus.PRECONDITION_FAILED,
            VERSION_MISMATCH,
            "This was changed since the version in If-Match; load it again.");
    problem.setProperty("changedAt", changedAt);
    problem.setProperty("changedBy", changedBy);
    return problem;
  }

  /** The code of an error that names none of its own: the name of its status. */
  public static String codeOf(HttpStatusCode status) {
    HttpStatus known = HttpStatus.resolve(status.value());
    return known == null ? "HTTP_" + status.value() : known.name();
  }
}
