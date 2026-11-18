import { screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { expectNoAxeViolations } from "@/test/axe";
import { renderRoute } from "@/test/render";
import { server } from "@/test/server";

describe("Status page", () => {
  it("shows the backend as up when it answers UP", async () => {
    server.use(http.get("*/api/health", () => HttpResponse.json({ status: "UP" })));

    const { container } = renderRoute("/");

    expect(await screen.findByText("Up")).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1, name: "niskora" })).toBeInTheDocument();
    await expectNoAxeViolations(container);
  });

  it("shows checking until the backend answers", async () => {
    server.use(
      http.get(
        "*/api/health",
        () =>
          new Promise<never>(() => {
            // Never answers.
          }),
      ),
    );

    renderRoute("/");

    expect(await screen.findByText("Checking…")).toBeInTheDocument();
  });

  it("shows the backend as down when it answers DOWN", async () => {
    server.use(
      http.get("*/api/health", () => HttpResponse.json({ status: "DOWN" }, { status: 503 })),
    );

    renderRoute("/");

    expect(await screen.findByText("Down")).toBeInTheDocument();
  });

  it("shows the backend as down when it cannot be reached", async () => {
    server.use(http.get("*/api/health", () => HttpResponse.error()));

    renderRoute("/");

    expect(await screen.findByText("Down")).toBeInTheDocument();
  });
});
