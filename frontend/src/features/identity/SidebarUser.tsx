import { useMutation, useQueryClient, useSuspenseQuery } from "@tanstack/react-query";
import { useRouter } from "@tanstack/react-router";
import LogOut from "lucide-react/icons/log-out";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { api } from "@/lib/api/client";
import { errorKey, noContentOf } from "@/lib/api/problem";
import { Button } from "@/ui/button";
import { leaveForLogin, sessionQuery } from "./session";

/**
 * The logged-in user's full name with "Log out", at the foot of the sidebar (FE-8). The shell loads the
 * session before it shows, so it is always there.
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
    // A 401 goes to Login by itself (FE-72); any other error says why it failed (FE-55, FE-56).
    onError: (error) => {
      toast.error(tCommon(`errors.${errorKey(error)}`), { id: "logOut" });
    },
  });

  return (
    <div className="flex flex-col gap-2 border-t px-6 py-4">
      <p className="truncate text-sm font-medium">{session.user.fullName}</p>
      <Button
        variant="outline"
        size="sm"
        className="self-start"
        disabled={logOut.isPending}
        onClick={() => {
          logOut.mutate();
        }}
      >
        <LogOut aria-hidden="true" />
        {logOut.isPending ? t("loggingOut") : t("logOut")}
      </Button>
    </div>
  );
}
