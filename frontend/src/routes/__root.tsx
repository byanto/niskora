import { createRootRouteWithContext, Outlet } from "@tanstack/react-router";
import type { RouterContext } from "@/app/router";
import { Toaster } from "@/ui/sonner";

export const Route = createRootRouteWithContext<RouterContext>()({
  component: function Root() {
    return (
      <>
        <Outlet />
        <Toaster />
      </>
    );
  },
});
