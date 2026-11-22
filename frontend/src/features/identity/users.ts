import { queryOptions } from "@tanstack/react-query";
import { z } from "zod";
import { api } from "@/lib/api/client";
import { type Id, toId } from "@/lib/api/ids";
import { dataOf } from "@/lib/api/problem";
import type { components } from "@/lib/api/schema";
import { ifMatch } from "@/lib/api/version";
import { pageSearch } from "@/lib/search";

export type UserId = Id<"User">;

/**
 * An account as the Users screen lists it, with its branded id (FE-44), its version, which a
 * change sends back (ENG-87), and whether it is disabled (PLT-45).
 */
export type User = Omit<components["schemas"]["UserResponse"], "id"> & { id: UserId };

/** What the Users screen needs to add an account: who, the login name and the first password. */
export type NewUser = components["schemas"]["NewUserRequest"];

/** The columns the list sorts by, as the backend names them (ENG-78). */
export const userSortFields = ["fullName", "loginName", "createdAt"] as const;
export type UserSortField = (typeof userSortFields)[number];

/** The column the backend sorts by when the URL names none. */
export const defaultUserSort: UserSortField = "fullName";

/**
 * The Users screen's URL (FE-41): its page and its sort, one column with a minus for descending.
 * A hand-edited sort falls back to the default rather than failing.
 */
export const usersSearch = pageSearch.extend({
  sort: z
    .enum(userSortFields.flatMap((field) => [field, `-${field}`] as const))
    .optional()
    .catch(undefined),
});
export type UsersSearch = z.infer<typeof usersSearch>;

/** Every account, a page at a time, in the order the URL asks (ENG-76, FE-59). */
export function usersQuery({ page, sort }: UsersSearch) {
  return queryOptions({
    queryKey: ["identity", "users", { page, sort }],
    queryFn: async ({ signal }) => {
      const list = await dataOf(
        api.GET("/api/identity/users", {
          params: { query: sort === undefined ? { page } : { page, sort } },
          signal,
        }),
      );
      return { ...list, data: list.data.map(branded) };
    },
  });
}

/** Every page of the list, in any order, and every account read alone, to load after a change. */
export const allUsersKey = ["identity", "users"] as const;

function branded(user: components["schemas"]["UserResponse"]): User {
  return { ...user, id: toId<"User">(user.id) };
}

/** One account as it is now, with its current version (ENG-87). */
export function userQuery(id: UserId) {
  return queryOptions({
    queryKey: [...allUsersKey, "one", id],
    queryFn: async ({ signal }) =>
      branded(
        await dataOf(api.GET("/api/identity/users/{id}", { params: { path: { id } }, signal })),
      ),
  });
}

/**
 * A change to one account (ENG-70): the account as the user saw it, whose version goes back as
 * `If-Match` (ENG-87), and the key of the press that asked for it (FE-80).
 */
export interface AccountChange<Values = undefined> {
  user: User;
  values: Values;
  idempotencyKey: string;
}

function changeHeaders({ user, idempotencyKey }: AccountChange<unknown>) {
  return { "Idempotency-Key": idempotencyKey, "If-Match": ifMatch(user.version) };
}

/** Gives another user a new password (PLT-21). */
export async function resetPassword(change: AccountChange<{ password: string }>) {
  return branded(
    await dataOf(
      api.POST("/api/identity/users/{id}/reset-password", {
        params: { path: { id: change.user.id }, header: changeHeaders(change) },
        body: change.values,
      }),
    ),
  );
}

/** Disables another user's account, which ends their sessions at once (PLT-45). */
export async function disableUser(change: AccountChange) {
  return branded(
    await dataOf(
      api.POST("/api/identity/users/{id}/disable", {
        params: { path: { id: change.user.id }, header: changeHeaders(change) },
      }),
    ),
  );
}

/** Enables a disabled account again (PLT-21). */
export async function enableUser(change: AccountChange) {
  return branded(
    await dataOf(
      api.POST("/api/identity/users/{id}/enable", {
        params: { path: { id: change.user.id }, header: changeHeaders(change) },
      }),
    ),
  );
}

/** Changes the logged-in user's own password, given their current one (PLT-22). */
export async function changeOwnPassword(
  change: AccountChange<{ currentPassword: string; newPassword: string }>,
) {
  return branded(
    await dataOf(
      api.POST("/api/identity/users/{id}/change-password", {
        params: { path: { id: change.user.id }, header: changeHeaders(change) },
        body: change.values,
      }),
    ),
  );
}

/** Adds an account, with the key of the press that asked for it (FE-80). */
export function addUser({ user, idempotencyKey }: { user: NewUser; idempotencyKey: string }) {
  return dataOf(
    api.POST("/api/identity/users", {
      params: { header: { "Idempotency-Key": idempotencyKey } },
      body: user,
    }),
  );
}
