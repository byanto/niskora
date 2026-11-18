import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { expectNoAxeViolations } from "../../test/axe";
import { RestockLevelBadge } from "./RestockLevelBadge";

// A level reads without colour: its own icon and its word, always shown (FE-58, FE-103).
describe("restock level badge", () => {
  it.each([
    ["LATE", "Late", "lucide-octagon-alert"],
    ["ORDER_NOW", "Order now", "lucide-triangle-alert"],
    ["FINE", "Fine", "lucide-circle-check"],
  ] as const)("shows %s with its icon and the word %s", async (level, word, icon) => {
    const { container } = render(<RestockLevelBadge level={level} />);

    const badge = screen.getByText(word);
    expect(badge.closest("[data-slot=badge]")?.querySelector(`svg.${icon}`)).not.toBeNull();
    await expectNoAxeViolations(container);
  });
});
