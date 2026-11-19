import { createRootRouteWithContext, Outlet } from "@tanstack/react-router";
import type { RouterContext } from "@/app/router";

export const Route = createRootRouteWithContext<RouterContext>()({
  component: function Root() {
    return <Outlet />;
  },
});
