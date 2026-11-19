import { queryOptions } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { dataOf, ProblemError } from "@/lib/api/problem";

/**
 * Who is logged in (FE-71). The app asks on start and on entering the shell, and the backend
 * answers 401 while nobody is. It lives in the query cache only, never in the browser's storage
 * (FE-74).
 */
export const sessionQuery = queryOptions({
  queryKey: ["identity", "session"],
  queryFn: ({ signal }) => dataOf(api.GET("/api/identity/session", { signal })),
});

/** Whether a call failed because there is no session (ENG-81). */
export function isUnauthorized(error: unknown): boolean {
  return error instanceof ProblemError && error.problem.status === 401;
}

/**
 * Where to go after logging in: the URL the user was sent away from (FE-72), as long as it is a
 * path within the app. Anything else, such as `//elsewhere.example`, goes Home, so a crafted
 * Login link cannot send the user to another site.
 */
export function afterLogin(redirect: string | undefined): string {
  if (redirect?.startsWith("/") && !redirect.startsWith("//") && !redirect.startsWith("/\\")) {
    return redirect;
  }
  return "/";
}
