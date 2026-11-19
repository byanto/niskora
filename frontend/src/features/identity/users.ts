import { queryOptions } from "@tanstack/react-query";
import { z } from "zod";
import { api } from "@/lib/api/client";
import { type Id, toId } from "@/lib/api/ids";
import { dataOf } from "@/lib/api/problem";
import type { components } from "@/lib/api/schema";
import { pageSearch } from "@/lib/search";

export type UserId = Id<"User">;

/** An account as the Users screen lists it, with its branded id (FE-44). */
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
      return {
        ...list,
        data: list.data.map((user): User => ({ ...user, id: toId<"User">(user.id) })),
      };
    },
  });
}

/** Every page of the list, in any order, to load again after a change. */
export const allUsersKey = ["identity", "users"] as const;

/** Adds an account, with the key of the press that asked for it (FE-80). */
export function addUser({ user, idempotencyKey }: { user: NewUser; idempotencyKey: string }) {
  return dataOf(
    api.POST("/api/identity/users", {
      params: { header: { "Idempotency-Key": idempotencyKey } },
      body: user,
    }),
  );
}
