import { createFileRoute, redirect } from "@tanstack/react-router";
import { StartFailed } from "@/app/StartFailed";
import { afterLogin, LoginScreen, whoIsLoggedIn } from "@/features/identity";

interface LoginSearch {
  /** The URL to go back to after logging in (FE-72). */
  redirect?: string;
}

/** Login stands outside the shell (FE-38). A user who is already logged in is sent on. */
export const Route = createFileRoute("/login")({
  validateSearch: (search): LoginSearch =>
    typeof search.redirect === "string" ? { redirect: search.redirect } : {},
  beforeLoad: async ({ context, search }) => {
    // Asking also hands out the CSRF token cookie the login request echoes (FE-73).
    if ((await whoIsLoggedIn(context.queryClient, { askAfresh: true })) === null) {
      return;
    }
    // eslint-disable-next-line @typescript-eslint/only-throw-error -- the router's way to redirect
    throw redirect({ href: afterLogin(search.redirect), replace: true });
  },
  component: function Login() {
    const { redirect: target } = Route.useSearch();
    return <LoginScreen redirect={target} />;
  },
  errorComponent: StartFailed,
});
