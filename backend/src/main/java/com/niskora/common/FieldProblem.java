package com.niskora.common;

/**
 * One field error of a validation error (ENG-82): a JSON Pointer to the field in the request body,
 * such as {@code /lines/2/quantity}, its own stable code, which the frontend translates like the
 * top-level code, and a detail in English for developers.
 */
public record FieldProblem(String pointer, String code, String detail) {

  /** The code of a field that must be given and is missing or blank. */
  public static final String REQUIRED = "REQUIRED";

  /**
   * The error of a field named by its path, as Spring and Bean Validation name it: {@code
   * lines[2].quantity} points at {@code /lines/2/quantity}.
   */
  public static FieldProblem at(String path, String code, String detail) {
    return new FieldProblem(pointer(path), code, detail);
  }

  /** A field's path as a JSON Pointer (RFC 6901). The empty path is the whole body. */
  static String pointer(String path) {
    if (path.isEmpty()) {
      return "";
    }
    StringBuilder pointer = new StringBuilder();
    for (String token : path.replace("[", ".").replace("]", "").split("\\.", -1)) {
      pointer.append('/').append(token.replace("~", "~0").replace("/", "~1"));
    }
    return pointer.toString();
  }
}
