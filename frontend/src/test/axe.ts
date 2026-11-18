import axe from "axe-core";
import { expect } from "vitest";

/**
 * Checks what is rendered against WCAG 2.1 AA with axe (FE-58, FE-110). jsdom lays nothing out,
 * so colour contrast cannot be measured here; the Playwright journeys check it in a real browser.
 */
export async function expectNoAxeViolations(container: Element) {
  const results = await axe.run(container, {
    runOnly: { type: "tag", values: ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"] },
    rules: { "color-contrast": { enabled: false } },
  });
  expect(results.violations.map((violation) => `${violation.id}: ${violation.help}`)).toEqual([]);
}
