import { fireEvent, screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { expectNoAxeViolations } from "@/test/axe";
import { renderRoute } from "@/test/render";
import { server } from "@/test/server";
import { rina, loggedIn, notLoggedIn, problem, usersListed } from "@/test/session";

const up = () => HttpResponse.json({ status: "UP" });

/** The backend logs rina in with the right password, and from then on knows who is logged in. */
function loginAccepted(onRequest: (request: Request) => void = () => undefined) {
  return http.post("*/api/identity/session", async ({ request }) => {
    onRequest(request.clone());
    const { password } = (await request.json()) as { password: string };
    if (password !== "correct horse battery") {
      return problem(401, "BAD_CREDENTIALS");
    }
    server.use(loggedIn());
    return HttpResponse.json({ user: rina });
  });
}

function logIn(loginName = "rina", password = "correct horse battery") {
  fireEvent.change(screen.getByLabelText("Login name"), { target: { value: loginName } });
  fireEvent.change(screen.getByLabelText("Password"), { target: { value: password } });
  fireEvent.click(screen.getByRole("button", { name: "Log in" }));
}

describe("Login", () => {
  beforeEach(() => {
    server.use(http.get("*/api/health", up));
    document.cookie = "XSRF-TOKEN=token-from-the-backend; path=/";
  });

  afterEach(() => {
    document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
  });

  it("stands outside the shell and asks for a login name and a password", async () => {
    server.use(notLoggedIn());
    const { container } = renderRoute("/login");

    expect(await screen.findByRole("heading", { level: 1, name: "Log in" })).toBeInTheDocument();
    expect(screen.getByLabelText("Login name")).toHaveAttribute("autocomplete", "username");
    expect(screen.getByLabelText("Password")).toHaveAttribute("type", "password");
    expect(screen.queryByRole("navigation", { name: "Main" })).not.toBeInTheDocument();
    await expectNoAxeViolations(container);
  });

  it("sends a user who is not logged in to Login, and back to the same URL after logging in", async () => {
    server.use(notLoggedIn(), loginAccepted(), usersListed());
    const { router } = renderRoute("/users");

    await screen.findByRole("heading", { level: 1, name: "Log in" });
    expect(router.state.location.pathname).toBe("/login");
    logIn();

    expect(await screen.findByRole("heading", { level: 1, name: "Users" })).toBeInTheDocument();
    expect(router.state.location.href).toBe("/users");
  });

  it("echoes the CSRF token cookie in a header on the login request", async () => {
    let sent: Request | undefined;
    server.use(
      notLoggedIn(),
      loginAccepted((request) => {
        sent = request;
      }),
    );
    renderRoute("/login");

    await screen.findByRole("heading", { level: 1, name: "Log in" });
    logIn();

    await screen.findByRole("heading", { level: 1, name: "Home" });
    expect(sent?.headers.get("X-XSRF-TOKEN")).toBe("token-from-the-backend");
    expect(await sent?.json()).toEqual({ loginName: "rina", password: "correct horse battery" });
  });

  it("goes Home after logging in when it was opened on its own", async () => {
    server.use(notLoggedIn(), loginAccepted(), usersListed());
    const { router } = renderRoute("/login");

    await screen.findByRole("heading", { level: 1, name: "Log in" });
    logIn();

    expect(
      await screen.findByRole("heading", { name: "Nothing imported yet" }),
    ).toBeInTheDocument();
    expect(router.state.location.href).toBe("/");
  });

  it("never sends the user to another site after logging in", async () => {
    server.use(notLoggedIn(), loginAccepted(), usersListed());
    const { router } = renderRoute("/login?redirect=%2F%2Fevil.example%2Fsteal");

    await screen.findByRole("heading", { level: 1, name: "Log in" });
    logIn();

    await screen.findByRole("heading", { name: "Nothing imported yet" });
    expect(router.state.location.href).toBe("/");
  });

  it("sends a user who is already logged in on from Login", async () => {
    server.use(loggedIn(), usersListed());
    const { router } = renderRoute("/login?redirect=%2Fusers");

    expect(await screen.findByRole("heading", { level: 1, name: "Users" })).toBeInTheDocument();
    expect(router.state.location.href).toBe("/users");
  });

  it("says when the login name or password is wrong, and stays on Login", async () => {
    server.use(notLoggedIn(), loginAccepted(), usersListed());
    const { container } = renderRoute("/login");

    await screen.findByRole("heading", { level: 1, name: "Log in" });
    logIn("rina", "wrong password");

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The login name or password is wrong.",
    );
    expect(screen.getByRole("heading", { level: 1, name: "Log in" })).toBeInTheDocument();
    await expectNoAxeViolations(container);
  });

  it("says to wait when failed logins are being slowed down", async () => {
    server.use(
      notLoggedIn(),
      http.post("*/api/identity/session", () =>
        problem(429, "TOO_MANY_LOGIN_ATTEMPTS", { "Retry-After": "4" }),
      ),
    );
    renderRoute("/login");

    await screen.findByRole("heading", { level: 1, name: "Log in" });
    logIn();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Too many failed logins. Wait a moment, then try again.",
    );
  });

  it("asks for both fields before it sends anything", async () => {
    let requests = 0;
    server.use(
      notLoggedIn(),
      http.post("*/api/identity/session", () => {
        requests += 1;
        return HttpResponse.json({ user: rina });
      }),
    );
    const { container } = renderRoute("/login");

    await screen.findByRole("heading", { level: 1, name: "Log in" });
    fireEvent.click(screen.getByRole("button", { name: "Log in" }));

    expect(await screen.findByText("Enter your login name.")).toBeInTheDocument();
    expect(screen.getByText("Enter your password.")).toBeInTheDocument();
    expect(screen.getByLabelText("Login name")).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByLabelText("Login name")).toHaveAccessibleDescription(
      "Enter your login name.",
    );
    expect(requests).toBe(0);
    await expectNoAxeViolations(container);
  });

  it("shows that it is logging in until the server answers", async () => {
    server.use(
      notLoggedIn(),
      http.post(
        "*/api/identity/session",
        () =>
          new Promise<never>(() => {
            // Never answers.
          }),
      ),
    );
    renderRoute("/login");

    await screen.findByRole("heading", { level: 1, name: "Log in" });
    logIn();

    expect(await screen.findByRole("button", { name: "Logging in…" })).toBeDisabled();
  });

  it("says the server could not be reached when it cannot ask who is logged in", async () => {
    server.use(http.get("*/api/identity/session", () => HttpResponse.error()));
    renderRoute("/");

    expect(await screen.findByRole("alert")).toHaveTextContent("The server could not be reached.");
    server.use(loggedIn());
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));
    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "Nothing imported yet" })).toBeInTheDocument();
    });
  });
});
