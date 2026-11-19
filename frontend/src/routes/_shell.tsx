import { createFileRoute, redirect } from "@tanstack/react-router";
import { Shell } from "@/app/Shell";
import { StartFailed } from "@/app/StartFailed";
import { isUnauthorized, sessionQuery } from "@/features/identity";

/**
 * Every screen except Login sits in the shell (FE-1), and only for a logged-in user: the app asks
 * who is logged in on start (FE-71), and sends a user who is not to Login, to come back to the
 * same URL afterwards (FE-72).
 */
export const Route = createFileRoute("/_shell")({
  beforeLoad: async ({ context, location }) => {
    try {
      // The session the cache already holds, or the backend's answer when it holds none.
      await context.queryClient.query({ ...sessionQuery, staleTime: "static" });
    } catch (error) {
      if (isUnauthorized(error)) {
        // eslint-disable-next-line @typescript-eslint/only-throw-error -- the router's way to redirect
        throw redirect({ to: "/login", search: { redirect: location.href }, replace: true });
      }
      throw error;
    }
  },
  component: Shell,
  errorComponent: StartFailed,
});
