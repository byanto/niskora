import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterAll, afterEach, beforeAll } from "vitest";
import "../lib/i18n";
import { server } from "./server";

// jsdom has no layout, so it does not implement scrolling, which the router calls on navigation.
// A test that runs in Node instead, such as eslint.config.test.ts, has no window at all.
if (typeof window !== "undefined") {
  window.scrollTo = () => {
    // Nothing to scroll.
  };
}

beforeAll(() => {
  server.listen({ onUnhandledFrame: "error" });
});
afterEach(() => {
  cleanup();
  server.resetHandlers();
});
afterAll(() => {
  server.close();
});
