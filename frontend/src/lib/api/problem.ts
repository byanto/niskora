import { common } from "@/lib/i18n/en/common";

/** An error answer of the API: Problem Details (RFC 9457) with niskora's stable `code` (ENG-65). */
export interface Problem {
  status?: number;
  code?: string;
  title?: string;
  detail?: string;
  /** Every field error of a validation error at once (ENG-82). */
  errors?: FieldProblem[];
  /** When a change refused with 412 was overtaken, and by whom (ENG-87). */
  changedAt?: string;
  changedBy?: { id: string; fullName: string } | null;
}

/** Who made the change that overtook a refused one, and when (FE-81). */
export interface VersionConflict {
  changedAt: string;
  /** Null for a change that no user made. */
  changedBy: { id: string; fullName: string } | null;
}

/**
 * The conflict of a change the backend refused with 412 because the thing moved past the version
 * the change was based on (ENG-87), or null for any other outcome.
 */
export function versionConflictOf(error: unknown): VersionConflict | null {
  if (
    !(error instanceof ProblemError) ||
    error.problem.code !== "VERSION_MISMATCH" ||
    error.problem.changedAt === undefined
  ) {
    return null;
  }
  return { changedAt: error.problem.changedAt, changedBy: error.problem.changedBy ?? null };
}

/** One field error: a JSON Pointer to the field in the request body and its own stable code. */
export interface FieldProblem {
  pointer: string;
  code: string;
  detail: string;
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

/**
 * The data of an answered request, or a `ProblemError` for an error answer. A query function
 * passes its client call through this, so its screen gets the error to show (FE-53).
 */
export async function dataOf<Data>(
  request: Promise<{ data?: Data; error?: unknown; response: Response }>,
): Promise<Data> {
  const { data, error, response } = await request;
  if (!response.ok || data === undefined) {
    throw new ProblemError(problemOf(error, response.status));
  }
  return data;
}

/**
 * Waits for a request that answers with no content, such as a delete, and throws a `ProblemError`
 * for an error answer, as `dataOf` does.
 */
export async function noContentOf(
  request: Promise<{ error?: unknown; response: Response }>,
): Promise<void> {
  const { error, response } = await request;
  if (!response.ok) {
    throw new ProblemError(problemOf(error, response.status));
  }
}

/**
 * The problem in an error answer. The backend's own answers are Problem Details with a code. An
 * answer from elsewhere, such as the proxy while the backend is down or Actuator's health check,
 * gets the name of its status as its code, as the backend names an error that has none (ENG-65).
 */
function problemOf(body: unknown, status: number): Problem {
  if (
    typeof body === "object" &&
    body !== null &&
    "code" in body &&
    typeof body.code === "string"
  ) {
    return { ...(body as Problem), status };
  }
  return { status, code: statusCodes[status] ?? `HTTP_${String(status)}` };
}

/** The statuses an answer that is not the backend's own may carry. */
const statusCodes: Partial<Record<number, string>> = {
  502: "BAD_GATEWAY",
  503: "SERVICE_UNAVAILABLE",
  504: "GATEWAY_TIMEOUT",
};

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

/** A key under `fieldErrors` in the `common` translations. */
export type FieldErrorKey = keyof typeof common.fieldErrors;

/**
 * The field errors of a failed request (FE-56): the form field each one belongs to, named the
 * way React Hook Form names it, and the translation key of its code, `INVALID` when there is no
 * text for it. A request that failed for any other reason has none.
 */
export function fieldErrorsOf(error: unknown): { field: string; key: FieldErrorKey }[] {
  if (!(error instanceof ProblemError)) {
    return [];
  }
  return (error.problem.errors ?? []).map(({ pointer, code }) => ({
    field: fieldOf(pointer),
    key: Object.hasOwn(common.fieldErrors, code) ? (code as FieldErrorKey) : "INVALID",
  }));
}

/**
 * The form field a JSON Pointer (RFC 6901) points at: `/lines/2/quantity` is `lines.2.quantity`.
 * The empty pointer, the whole body, is the empty name.
 */
export function fieldOf(pointer: string): string {
  return pointer
    .split("/")
    .slice(1)
    .map((token) => token.replaceAll("~1", "/").replaceAll("~0", "~"))
    .join(".");
}
