import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { expectNoAxeViolations } from "@/test/axe";
import { renderRoute } from "@/test/render";

describe("Home", () => {
  it("says what is missing while nothing has been imported", async () => {
    const { container } = renderRoute("/");

    expect(await screen.findByRole("heading", { level: 1, name: "Home" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Nothing imported yet" })).toBeInTheDocument();
    expect(screen.getByText(/once the first BigSeller exports are imported/)).toBeInTheDocument();
    await expectNoAxeViolations(container);
  });
});
