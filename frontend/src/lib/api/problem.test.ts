import { describe, expect, it } from "vitest";
import { fieldErrorsOf, fieldOf, ProblemError } from "./problem";

describe("fieldOf", () => {
  it("names the form field a JSON Pointer points at (FE-56)", () => {
    expect(fieldOf("/loginName")).toBe("loginName");
    expect(fieldOf("/lines/2/quantity")).toBe("lines.2.quantity");
  });

  it("reads the escaped characters of a pointer", () => {
    expect(fieldOf("/a~1b/c~0d")).toBe("a/b.c~d");
  });

  it("names nothing for the whole body", () => {
    expect(fieldOf("")).toBe("");
  });
});

describe("fieldErrorsOf", () => {
  it("gives each field error's field and translation key", () => {
    const error = new ProblemError({
      status: 422,
      code: "VALIDATION_FAILED",
      errors: [
        {
          pointer: "/loginName",
          code: "LOGIN_NAME_TAKEN",
          detail: "The login name rina is taken.",
        },
        { pointer: "/password", code: "TOO_SHORT", detail: "Too short." },
      ],
    });

    expect(fieldErrorsOf(error)).toEqual([
      { field: "loginName", key: "LOGIN_NAME_TAKEN" },
      { field: "password", key: "TOO_SHORT" },
    ]);
  });

  it("translates a field code it has no text for as invalid", () => {
    const error = new ProblemError({
      status: 400,
      code: "VALIDATION_FAILED",
      errors: [{ pointer: "/fullName", code: "SOMETHING_NEW", detail: "" }],
    });

    expect(fieldErrorsOf(error)).toEqual([{ field: "fullName", key: "INVALID" }]);
  });

  it("finds none in an error without field errors", () => {
    expect(fieldErrorsOf(new ProblemError({ status: 409, code: "REQUEST_IN_PROGRESS" }))).toEqual(
      [],
    );
    expect(fieldErrorsOf(new TypeError("Failed to fetch"))).toEqual([]);
  });
});
