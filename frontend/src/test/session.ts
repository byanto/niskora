import { http, HttpResponse } from "msw";

/** The user the tests log in as: invented, like all test data. */
export const rina = {
  id: "5b1e8c3a-2f4d-4a6b-9c8e-1d2f3a4b5c6d",
  loginName: "rina",
  fullName: "Rina Wulandari",
};

/** The backend's answer to "who is logged in" while the given user is (FE-71). */
export function loggedIn(user = rina) {
  return http.get("*/api/identity/session", () => HttpResponse.json({ user }));
}

/** The backend's list of accounts, with only the given user in it (ENG-76). */
export function usersListed(user = rina) {
  return http.get("*/api/identity/users", () =>
    HttpResponse.json({
      data: [{ ...user, createdAt: "2026-11-16T03:15:00Z" }],
      pagination: { page: 1, pageSize: 50, totalItems: 1, totalPages: 1 },
    }),
  );
}

/** The backend's answer to "who is logged in" while nobody is. */
export function notLoggedIn() {
  return http.get("*/api/identity/session", () => unauthorized());
}

/** The 401 the backend answers any call without a session with. */
export function unauthorized() {
  return problem(401, "UNAUTHORIZED");
}

/** An error answer of the backend: Problem Details with its code (ENG-65). */
export function problem(status: number, code: string, headers: Record<string, string> = {}) {
  return HttpResponse.json(
    { status, code },
    { status, headers: { "Content-Type": "application/problem+json", ...headers } },
  );
}
