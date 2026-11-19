import { createFileRoute, redirect } from "@tanstack/react-router";
import { Shell } from "@/app/Shell";
import { StartFailed } from "@/app/StartFailed";
import { whoIsLoggedIn } from "@/features/identity";

/**
 * Every screen except Login sits in the shell (FE-1), and only for a logged-in user: the app asks
 * who is logged in on start (FE-71), and sends a user who is not to Login, to come back to the
 * same URL afterwards (FE-72).
 */
export const Route = createFileRoute("/_shell")({
  beforeLoad: async ({ context, location }) => {
    if ((await whoIsLoggedIn(context.queryClient)) === null) {
      // eslint-disable-next-line @typescript-eslint/only-throw-error -- the router's way to redirect
      throw redirect({ to: "/login", search: { redirect: location.href }, replace: true });
    }
  },
  component: Shell,
  errorComponent: StartFailed,
});
