import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { versionConflictOf } from "@/lib/api/problem";
import { newIdempotencyKey, retryRepeated } from "@/lib/api/repeat";
import { type AccountChange, allUsersKey, type User, userQuery } from "./users";

/**
 * A change to one account from a dialog (ENG-87, FE-80, FE-81). It sends the version of the
 * account as the dialog last saw it, with one key per press. When someone else changed the
 * account first, the backend refuses with 412: `conflict` says who and when, and `loadLatest`
 * brings in the latest version, which the next press is based on. The dialog's form keeps what
 * was typed throughout, and nothing is sent again by itself.
 */
export function useAccountChange<Values>({
  user,
  change,
  onChanged,
  onFailed,
}: {
  /** The account as the dialog opened on it. */
  user: User;
  change: (request: AccountChange<Values>) => Promise<User>;
  /** Once the server has changed the account and the list shows it (FE-54). */
  onChanged: (changed: User) => void;
  /** When the change failed, to place field errors on their fields (FE-56). */
  onFailed?: (error: unknown) => void;
}) {
  const queryClient = useQueryClient();
  const [current, setCurrent] = useState(user);

  const save = useMutation({
    mutationFn: change,
    // A press that got no answer is sent again with its own key and version (FE-80).
    ...retryRepeated,
    onSuccess: async (changed) => {
      await queryClient.invalidateQueries({ queryKey: allUsersKey });
      onChanged(changed);
    },
    onError: (error) => onFailed?.(error),
  });

  const latest = useMutation({
    mutationFn: () => queryClient.query({ ...userQuery(current.id), staleTime: 0 }),
    onSuccess: async (loaded) => {
      setCurrent(loaded);
      save.reset();
      await queryClient.invalidateQueries({ queryKey: allUsersKey });
    },
  });

  const conflict = versionConflictOf(save.error);
  return {
    /** The account as the dialog now knows it: as it opened, or the latest version loaded. */
    current,
    /** Sends the change, as one new press of its button (FE-80). */
    save: (values: Values) => {
      latest.reset();
      save.mutate({ user: current, values, idempotencyKey: newIdempotencyKey() });
    },
    saving: save.isPending,
    /** Who changed the account first and when, while the change waits for the latest version. */
    conflict,
    /** Why the change failed, unless someone changed the account first. */
    error: conflict === null && save.isError ? save.error : null,
    loadLatest: () => {
      latest.mutate();
    },
    loadingLatest: latest.isPending,
    /** Why the latest version could not be loaded. */
    loadLatestError: latest.isError ? latest.error : null,
  };
}
