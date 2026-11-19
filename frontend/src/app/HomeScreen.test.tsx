import { fireEvent, screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { expectNoAxeViolations } from "@/test/axe";
import { renderRoute } from "@/test/render";
import { server } from "@/test/server";

const up = () => HttpResponse.json({ status: "UP" });

describe("Home", () => {
  it("says what is missing while nothing has been imported, and offers the next action", async () => {
    server.use(http.get("*/api/health", up));

    const { container } = renderRoute("/");

    expect(
      await screen.findByRole("heading", { name: "Nothing imported yet" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1, name: "Home" })).toBeInTheDocument();
    expect(screen.getByText(/once the first BigSeller exports are imported/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Import BigSeller exports" })).toHaveAttribute(
      "href",
      "/imports",
    );
    await expectNoAxeViolations(container);
  });

  it("shows a skeleton while it loads", async () => {
    server.use(
      http.get(
        "*/api/health",
        () =>
          new Promise<never>(() => {
            // Never answers.
          }),
      ),
    );

    const { container } = renderRoute("/");

    expect(await screen.findByText("Loading…")).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1, name: "Home" })).toBeInTheDocument();
    await expectNoAxeViolations(container);
  });

  it("says why it failed to load, and loads again on retry", async () => {
    let answers = 0;
    server.use(
      http.get("*/api/health", () => {
        answers += 1;
        return answers === 1 ? HttpResponse.json({ status: "DOWN" }, { status: 503 }) : up();
      }),
    );

    const { container } = renderRoute("/");

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The server is not ready. Wait a moment.",
    );
    await expectNoAxeViolations(container);
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));
    expect(
      await screen.findByRole("heading", { name: "Nothing imported yet" }),
    ).toBeInTheDocument();
  });

  it("shows the translated message of the backend's Problem Details code", async () => {
    server.use(
      http.get("*/api/health", () =>
        HttpResponse.json(
          { status: 500, code: "INTERNAL_SERVER_ERROR", detail: "An unexpected error occurred." },
          { status: 500, headers: { "Content-Type": "application/problem+json" } },
        ),
      ),
    );

    renderRoute("/");

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Something went wrong on the server.",
    );
  });

  it("says the server could not be reached when the request gets no answer", async () => {
    server.use(http.get("*/api/health", () => HttpResponse.error()));

    renderRoute("/");

    expect(await screen.findByRole("alert")).toHaveTextContent("The server could not be reached.");
  });
});
