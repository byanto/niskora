package com.niskora.common;

/**
 * One field error of a validation error (ENG-82): a JSON Pointer to the field in the request body,
 * such as {@code /lines/2/quantity}, its own stable code, which the frontend translates like the
 * top-level code, and a detail in English for developers.
 */
public record FieldProblem(String pointer, String code, String detail) {

  /** The code of a field that must be given and is missing or blank. */
  public static final String REQUIRED = "REQUIRED";
}
