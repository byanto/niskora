import { ProblemError } from "./problem";

/**
 * Repeated requests (FE-80). Every `POST` that creates something or triggers an action carries an
 * `Idempotency-Key`: one per user intent, made when the user presses the button, and sent again on
 * every retry of that same press. The backend then runs the press once, however often it arrives,
 * and answers a repeat with what the first one answered (ENG-85, ENG-86).
 *
 * A mutation makes the key in its button's handler and passes it in its variables, so a retry,
 * which calls the mutation function again with the same variables, sends the same key.
 */
export function newIdempotencyKey(): string {
  return crypto.randomUUID();
}

/** How often a press is tried again after its first try. */
const RETRIES = 3;

/**
 * The retry options of a mutation that sends an `Idempotency-Key`. It is tried again when it got
 * no answer, which may mean the backend never saw it or saw it and the answer was lost, and while
 * the backend says the first request with the key is still running. Any other failure is the
 * answer, and is shown.
 */
export const retryRepeated = {
  retry: (failures: number, error: unknown) =>
    failures < RETRIES &&
    (error instanceof TypeError ||
      (error instanceof ProblemError && error.problem.code === "REQUEST_IN_PROGRESS")),
  // Half a second, then one, then two.
  retryDelay: (failures: number) => 500 * 2 ** failures,
};
