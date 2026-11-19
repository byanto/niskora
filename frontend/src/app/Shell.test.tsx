import { act, fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { api } from "@/lib/api/client";
import { expectNoAxeViolations } from "@/test/axe";
import { renderRoute } from "@/test/render";
import { server } from "@/test/server";

describe("Shell", () => {
  it("groups the screens in the sidebar by job and marks the current one", async () => {
    const { container } = renderRoute("/");

    const sidebar = await screen.findByRole("navigation", { name: "Main" });
    const daily = within(sidebar).getByRole("list", { name: "Daily" });
    const home = within(daily).getByRole("link", { name: "Home" });
    expect(home).toHaveAttribute("href", "/");
    expect(home).toHaveAttribute("aria-current", "page");
    await expectNoAxeViolations(container);
  });

  it("opens the sidebar as a drawer from the menu button, and closes it on navigating", async () => {
    renderRoute("/");

    fireEvent.click(await screen.findByRole("button", { name: "Open menu" }));

    const drawer = await screen.findByRole("dialog", { name: "Menu" });
    await expectNoAxeViolations(drawer);
    fireEvent.click(within(drawer).getByRole("link", { name: "Home" }));
    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
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
    server.use(
      http.get("*/api/health", () =>
        HttpResponse.json({ status: "UP" }, { headers: { "Niskora-Build-Id": "a1b2c3" } }),
      ),
    );
    const { container } = renderRoute("/");
    await screen.findByRole("navigation", { name: "Main" });

    await act(() => api.GET("/api/health"));

    const notice = screen.getByRole("status");
    expect(notice).toHaveTextContent("A new version is available,");
    expect(within(notice).getByRole("button", { name: "reload" })).toBeInTheDocument();
    await expectNoAxeViolations(container);
  });

  it("offers no reload while the backend answers from the same build", async () => {
    server.use(
      http.get("*/api/health", () =>
        HttpResponse.json({ status: "UP" }, { headers: { "Niskora-Build-Id": "dev" } }),
      ),
    );
    renderRoute("/");
    await screen.findByRole("navigation", { name: "Main" });

    await act(() => api.GET("/api/health"));

    expect(screen.queryByText(/A new version is available/)).not.toBeInTheDocument();
  });
});
