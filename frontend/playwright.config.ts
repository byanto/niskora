import { defineConfig, devices } from "@playwright/test";

/**
 * The Playwright journeys (ENG-39): one happy path per capability, in a real browser against the
 * whole Compose stack, which `docker compose up` serves on port 8000. They need an account, which
 * the CI job creates with the first-user command; `e2e/account.ts` names it.
 */
export default defineConfig({
  testDir: "e2e",
  // A flaky journey is fixed or deleted, never retried (ENG-41).
  retries: 0,
  forbidOnly: process.env.CI !== undefined,
  // The journeys share one account and its session, so they run one at a time.
  workers: 1,
  reporter: process.env.CI === undefined ? "list" : [["list"], ["html", { open: "never" }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:8000",
    trace: "retain-on-failure",
  },
  // Screens are designed at 1280 px and wider (FE-106).
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"], viewport: { width: 1280, height: 800 } },
    },
  ],
});
