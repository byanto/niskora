import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createRouter, type RouterHistory } from "@tanstack/react-router";
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
  return createRouter({
    routeTree,
    ...(history === undefined ? {} : { history }),
    context: { queryClient },
    // Query decides when data is stale; the router's own preload cache would hide that.
    defaultPreloadStaleTime: 0,
    Wrap: ({ children }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    ),
  });
}

declare module "@tanstack/react-router" {
  interface Register {
    router: ReturnType<typeof createAppRouter>;
  }
}
