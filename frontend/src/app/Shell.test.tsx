import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";
import { expectNoAxeViolations } from "@/test/axe";
import { renderRoute } from "@/test/render";
import { server } from "@/test/server";
import { loggedIn, usersListed } from "@/test/session";

/** The backend's health answer, from the given build. Home asks for it until step 3. */
function healthFrom(build: string) {
  return http.get("*/api/health", () =>
    HttpResponse.json({ status: "UP" }, { headers: { "Niskora-Build-Id": build } }),
  );
}

describe("Shell", () => {
  beforeEach(() => {
    server.use(loggedIn(), healthFrom("dev"), usersListed());
  });

  it("groups the screens in the sidebar by job and marks the current one", async () => {
    const { container } = renderRoute("/");

    const sidebar = await screen.findByRole("navigation", { name: "Main" });
    const home = within(within(sidebar).getByRole("list", { name: "Daily" })).getByRole("link", {
      name: "Home",
    });
    const users = within(within(sidebar).getByRole("list", { name: "Setup" })).getByRole("link", {
      name: "Users",
    });
    expect(home).toHaveAttribute("href", "/");
    expect(home).toHaveAttribute("aria-current", "page");
    expect(users).toHaveAttribute("href", "/users");
    expect(users).not.toHaveAttribute("aria-current");
    await expectNoAxeViolations(container);
  });

  it("marks another screen as current once the user goes there", async () => {
    renderRoute("/");

    const sidebar = await screen.findByRole("navigation", { name: "Main" });
    fireEvent.click(within(sidebar).getByRole("link", { name: "Users" }));

    expect(await screen.findByRole("heading", { level: 1, name: "Users" })).toBeInTheDocument();
    expect(within(sidebar).getByRole("link", { name: "Users" })).toHaveAttribute(
      "aria-current",
      "page",
    );
    expect(within(sidebar).getByRole("link", { name: "Home" })).not.toHaveAttribute("aria-current");
  });

  it("opens the sidebar as a drawer from the menu button, and closes it on navigating", async () => {
    renderRoute("/");

    fireEvent.click(await screen.findByRole("button", { name: "Open menu" }));

    const drawer = await screen.findByRole("dialog", { name: "Menu" });
    await expectNoAxeViolations(drawer);
    fireEvent.click(within(drawer).getByRole("link", { name: "Users" }));
    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
    expect(screen.getByRole("heading", { level: 1, name: "Users" })).toBeInTheDocument();
  });

  it("closes the drawer from its close button", async () => {
    renderRoute("/");

    fireEvent.click(await screen.findByRole("button", { name: "Open menu" }));
    const drawer = await screen.findByRole("dialog", { name: "Menu" });
    fireEvent.click(within(drawer).getByRole("button", { name: "Close menu" }));

    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
    expect(screen.getByRole("button", { name: "Open menu" })).toHaveFocus();
  });

  it("offers a reload once the backend answers from another build", async () => {
    server.use(healthFrom("a1b2c3"));

    const { container } = renderRoute("/");

    expect(await screen.findByText(/A new version is available,/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "reload" })).toBeInTheDocument();
    await expectNoAxeViolations(container);
  });

  it("offers no reload while the backend answers from the same build", async () => {
    renderRoute("/");

    await screen.findByRole("heading", { name: "Nothing imported yet" });
    expect(screen.queryByText(/A new version is available/)).not.toBeInTheDocument();
  });

  it("stands in for /imports until its screen is built", async () => {
    const { container } = renderRoute("/imports");

    expect(await screen.findByRole("heading", { level: 1, name: "Imports" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Not built yet" })).toBeInTheDocument();
    await expectNoAxeViolations(container);
  });
});
