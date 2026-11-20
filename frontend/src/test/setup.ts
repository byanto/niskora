import "@testing-library/jest-dom/vitest";
import { cleanup, configure } from "@testing-library/react";
import { afterAll, afterEach, beforeAll } from "vitest";
import { resetBuildWatch } from "@/lib/api/build";
import { toast } from "@/ui/toaster";
import "@/lib/i18n";
import { server } from "./server";

// jsdom has no layout, so it does not implement scrolling, which the router calls on navigation.
// A test that runs in Node instead, such as eslint.config.test.ts, has no window at all.
if (typeof window !== "undefined") {
  window.scrollTo = () => {
    // Nothing to scroll.
  };
  // Nor does it implement media queries: every query reads as not matching, as on a narrow window.
  window.matchMedia = (query) =>
    ({
      matches: false,
      media: query,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
    }) as Partial<MediaQueryList> as MediaQueryList;
}

// A screen's first render in a test file also loads its modules, which takes more than Testing
// Library's default second when every file runs at once. A screen that never appears still fails.
configure({ asyncUtilTimeout: 5000 });

beforeAll(() => {
  server.listen({ onUnhandledFrame: "error" });
});
afterEach(() => {
  cleanup();
  server.resetHandlers();
  resetBuildWatch();
  // The toaster keeps its toasts in one store for the whole page, so each test starts with none.
  toast.dismiss();
});
afterAll(() => {
  server.close();
});
