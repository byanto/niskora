import { useMutation, useQueryClient, useSuspenseQuery } from "@tanstack/react-query";
import { useRouter } from "@tanstack/react-router";
import LogOut from "lucide-react/icons/log-out";
import { useTranslation } from "react-i18next";
import { api } from "@/lib/api/client";
import { errorKey, ProblemError } from "@/lib/api/problem";
import { Button } from "@/ui/button";
import { sessionQuery } from "./session";

/**
 * The logged-in user's name with "Log out", at the foot of the sidebar (FE-8). The shell loads the
 * session before it shows, so it is always there.
 */
export function SidebarUser() {
  const { t } = useTranslation("identity");
  const { t: tCommon } = useTranslation();
  const { data: session } = useSuspenseQuery(sessionQuery);
  const queryClient = useQueryClient();
  const router = useRouter();

  const logOut = useMutation({
    mutationFn: async () => {
      const { response } = await api.DELETE("/api/identity/session");
      if (!response.ok) {
        throw new ProblemError({ status: response.status });
      }
    },
    onSuccess: async () => {
      // Off the shell first, so no screen asks again for what is about to be forgotten.
      await router.navigate({ to: "/login", replace: true });
      queryClient.clear();
    },
  });

  return (
    <div className="flex flex-col gap-2 border-t px-6 py-4">
      <p className="truncate text-sm font-medium">{session.user.name}</p>
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
      {logOut.isError ? (
        <p role="alert" className="text-xs text-late">
          {tCommon(`errors.${errorKey(logOut.error)}`)}
        </p>
      ) : null}
    </div>
  );
}
