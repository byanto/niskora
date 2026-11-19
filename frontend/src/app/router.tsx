import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createRouter, type RouterHistory } from "@tanstack/react-router";
import { isUnauthorized, leaveForLogin, sessionQuery } from "@/features/identity";
import { routeTree } from "../routeTree.gen";

/** What every route can reach through its context. */
export interface RouterContext {
  queryClient: QueryClient;
}

interface Options {
  queryClient?: QueryClient;
  /** The browser's history by default; tests pass a memory history. */
  history?: RouterHistory;
}

/**
 * The router of the app, with TanStack Query holding everything from the backend (FE-40, FE-42).
 * A factory, so each test starts from its own router and its own empty cache.
 */
export function createAppRouter({ queryClient = new QueryClient(), history }: Options = {}) {
  const router = createRouter({
    routeTree,
    ...(history === undefined ? {} : { history }),
    context: { queryClient },
    // Query decides when data is stale; the router's own preload cache would hide that.
    defaultPreloadStaleTime: 0,
    Wrap: ({ children }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    ),
  });

  // A 401 from any call means the session ended: go to Login, and back here after (FE-72). The
  // session query itself is left to the routes, which ask it before a screen shows. On Login a
  // 401 is a wrong password, which Login shows itself.
  const toLogin = (error: unknown) => {
    const { location } = router.state;
    if (!isUnauthorized(error) || location.pathname === "/login") {
      return;
    }
    void leaveForLogin(router, queryClient, location.href);
  };
  queryClient.getQueryCache().subscribe((event) => {
    if (
      event.type === "updated" &&
      event.action.type === "error" &&
      event.query.queryHash !== sessionQueryHash
    ) {
      toLogin(event.action.error);
    }
  });
  queryClient.getMutationCache().subscribe((event) => {
    if (event.type === "updated" && event.action.type === "error") {
      toLogin(event.action.error);
    }
  });
  return router;
}

const sessionQueryHash = JSON.stringify(sessionQuery.queryKey);

declare module "@tanstack/react-router" {
  interface Register {
    router: ReturnType<typeof createAppRouter>;
  }
}
