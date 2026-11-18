import { QueryClient } from "@tanstack/react-query";
import { createMemoryHistory, RouterProvider } from "@tanstack/react-router";
import { render } from "@testing-library/react";
import { createAppRouter } from "../app/router";

/**
 * Renders the whole app at a URL, as a user opening that link would see it, with its own router
 * and an empty cache. The backend is whatever the test told the fake network (`server.use`).
 */
export function renderRoute(url: string) {
  const queryClient = new QueryClient({
    // A test states each answer it relies on; a retry would only slow a failure down.
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const router = createAppRouter({
    queryClient,
    history: createMemoryHistory({ initialEntries: [url] }),
  });
  return { router, ...render(<RouterProvider router={router} />) };
}
