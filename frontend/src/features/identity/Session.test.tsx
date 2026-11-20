import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { expectNoAxeViolations } from "@/test/axe";
import { renderRoute } from "@/test/render";
import { server } from "@/test/server";
import { rina, loggedIn, notLoggedIn, problem, unauthorized, usersListed } from "@/test/session";

describe("The session", () => {
  beforeEach(() => {
    document.cookie = "XSRF-TOKEN=token-from-the-backend; path=/";
  });

  afterEach(() => {
    document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
  });

  it("shows the logged-in user's name in the sidebar with Log out", async () => {
    server.use(
      loggedIn(),
      http.get("*/api/health", () => HttpResponse.json({ status: "UP" })),
    );
    const { container } = renderRoute("/");

    const sidebar = (await screen.findByRole("navigation", { name: "Main" })).closest("aside");
    if (sidebar === null) {
      throw new Error("The sidebar is not in an aside.");
    }
    expect(within(sidebar).getByText(rina.fullName)).toBeInTheDocument();
    expect(within(sidebar).getByRole("button", { name: "Log out" })).toBeInTheDocument();
    await expectNoAxeViolations(container);
  });

  it("logs out with the CSRF token and goes to Login", async () => {
    let sent: Request | undefined;
    server.use(
      loggedIn(),
      usersListed(),
      http.delete("*/api/identity/session", ({ request }) => {
        sent = request;
        server.use(notLoggedIn());
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { router } = renderRoute("/users");

    const sidebar = (await screen.findByRole("navigation", { name: "Main" })).closest("aside");
    if (sidebar === null) {
      throw new Error("The sidebar is not in an aside.");
    }
    fireEvent.click(within(sidebar).getByRole("button", { name: "Log out" }));

    expect(await screen.findByRole("heading", { level: 1, name: "Log in" })).toBeInTheDocument();
    expect(sent?.headers.get("X-XSRF-TOKEN")).toBe("token-from-the-backend");
    // Logging out on purpose does not ask to come back to where the user was.
    expect(router.state.location.href).toBe("/login");
  });

  it("says why logging out failed, and stays logged in", async () => {
    server.use(
      loggedIn(),
      usersListed(),
      http.delete("*/api/identity/session", () => problem(403, "CSRF_TOKEN_INVALID")),
    );
    const { router } = renderRoute("/users");

    const sidebar = (await screen.findByRole("navigation", { name: "Main" })).closest("aside");
    if (sidebar === null) {
      throw new Error("The sidebar is not in an aside.");
    }
    fireEvent.click(within(sidebar).getByRole("button", { name: "Log out" }));

    expect(await within(sidebar).findByRole("alert")).toHaveTextContent(
      "This page is out of date. Reload it, then try again.",
    );
    expect(router.state.location.href).toBe("/users");
    expect(within(sidebar).getByRole("button", { name: "Log out" })).toBeEnabled();
  });

  it("sends the user to Login when any call answers 401, to come back to the same URL", async () => {
    server.use(
      loggedIn(),
      // The session ended on the server, so from now on every call is refused.
      http.get("*/api/health", () => {
        server.use(notLoggedIn());
        return unauthorized();
      }),
    );
    const { router } = renderRoute("/");

    expect(await screen.findByRole("heading", { level: 1, name: "Log in" })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/login");
    expect(router.state.location.search).toEqual({ redirect: "/" });
  });

  it("sends no CSRF header on a request that changes nothing", async () => {
    let sent: Request | undefined;
    server.use(
      http.get("*/api/identity/session", ({ request }) => {
        sent = request;
        return HttpResponse.json({ user: rina });
      }),
      http.get("*/api/health", () => HttpResponse.json({ status: "UP" })),
    );
    renderRoute("/");

    await screen.findByRole("heading", { name: "Nothing imported yet" });
    expect(sent?.headers.has("X-XSRF-TOKEN")).toBe(false);
  });
});
