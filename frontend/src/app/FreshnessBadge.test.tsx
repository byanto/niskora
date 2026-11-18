import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { expectNoAxeViolations } from "../test/axe";
import { FreshnessBadge } from "./FreshnessBadge";

// Freshness takes the restock level badges: Stale takes Late's, Fresh takes Fine's (FE-104).
describe("freshness badge", () => {
  it.each([
    ["STALE", "Stale", "lucide-octagon-alert"],
    ["FRESH", "Fresh", "lucide-circle-check"],
  ] as const)("shows %s with its icon and the word %s", async (freshness, word, icon) => {
    const { container } = render(<FreshnessBadge freshness={freshness} />);

    const badge = screen.getByText(word);
    expect(badge.closest("[data-slot=badge]")?.querySelector(`svg.${icon}`)).not.toBeNull();
    await expectNoAxeViolations(container);
  });
});
