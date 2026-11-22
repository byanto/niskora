import { useMutation, useQueryClient, useSuspenseQuery } from "@tanstack/react-query";
import { useRouter } from "@tanstack/react-router";
import LogOut from "lucide-react/icons/log-out";
import { useTranslation } from "react-i18next";
import { api } from "@/lib/api/client";
import { errorKey, noContentOf } from "@/lib/api/problem";
import { toId } from "@/lib/api/ids";
import { Button } from "@/ui/button";
import { ChangePasswordDialog } from "./ChangePasswordDialog";
import { leaveForLogin, sessionQuery } from "./session";

/**
 * The logged-in user's full name with "Change password" and "Log out", at the foot of the sidebar
 * (FE-8). The shell
 * loads the session before it shows, so it is always there.
 */
export function SidebarUser() {
  const { t } = useTranslation("identity");
  const { t: tCommon } = useTranslation();
  const { data: session } = useSuspenseQuery(sessionQuery);
  const queryClient = useQueryClient();
  const router = useRouter();

  const logOut = useMutation({
    mutationFn: () => noContentOf(api.DELETE("/api/identity/session")),
    onSuccess: () => leaveForLogin(router, queryClient),
  });

  return (
    <div className="flex flex-col gap-2 border-t px-6 py-4">
      <p className="truncate text-sm font-medium">{session.user.fullName}</p>
      <div className="flex flex-wrap gap-2">
        <ChangePasswordDialog userId={toId<"User">(session.user.id)} />
        <Button
          variant="outline"
          size="sm"
          disabled={logOut.isPending}
          onClick={() => {
            logOut.mutate();
          }}
        >
          <LogOut aria-hidden="true" />
          {logOut.isPending ? t("loggingOut") : t("logOut")}
        </Button>
      </div>
      {/* A 401 goes to Login by itself (FE-72); any other error says why, until the next try
          (FE-55, FE-56). */}
      {logOut.isError ? (
        <p role="alert" className="text-xs text-late">
          {tCommon(`errors.${errorKey(logOut.error)}`)}
        </p>
      ) : null}
    </div>
  );
}
