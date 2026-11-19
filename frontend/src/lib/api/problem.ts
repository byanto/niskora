import { common } from "@/lib/i18n/en/common";

/** An error answer of the API: Problem Details (RFC 9457) with niskora's stable `code` (ENG-65). */
export interface Problem {
  status?: number;
  code?: string;
  title?: string;
  detail?: string;
}

/** A request the backend answered with an error. A query throws it, so its screen can show it. */
export class ProblemError extends Error {
  readonly problem: Problem;

  constructor(problem: Problem) {
    super(problem.detail ?? problem.code ?? "The request failed.");
    this.name = "ProblemError";
    this.problem = problem;
  }
}

/** A key under `errors` in the `common` translations. */
export type ErrorKey = keyof typeof common.errors;

/**
 * The translation key of an error (FE-56): its Problem Details code when there is text for it,
 * `NETWORK` when the request got no answer (`fetch` then throws a TypeError), and `UNEXPECTED`
 * for anything else. The backend's own `detail` is English for developers and is never shown.
 */
export function errorKey(error: unknown): ErrorKey {
  if (error instanceof ProblemError) {
    const code = error.problem.code;
    return code !== undefined && isErrorKey(code) ? code : "UNEXPECTED";
  }
  return error instanceof TypeError ? "NETWORK" : "UNEXPECTED";
}

function isErrorKey(code: string): code is ErrorKey {
  return Object.hasOwn(common.errors, code);
}
