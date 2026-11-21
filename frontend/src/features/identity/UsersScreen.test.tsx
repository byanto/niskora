import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { expectNoAxeViolations } from "@/test/axe";
import { renderRoute } from "@/test/render";
import { server } from "@/test/server";
import { loggedIn, problem, rina } from "@/test/session";

const USERS = "*/api/identity/users";

interface User {
  id: string;
  loginName: string;
  fullName: string;
  createdAt: string;
}

const bayu: User = {
  id: "8c2d4e6f-1a3b-4c5d-8e7f-9a0b1c2d3e4f",
  loginName: "bayu",
  fullName: "Bayu Santoso",
  createdAt: "2026-11-17T02:00:00Z",
};
const rinaAccount: User = { ...rina, createdAt: "2026-11-16T03:15:00Z" };
const sari: User = {
  id: "2f4a6c8e-0b1d-4f3a-9c5e-7d9f1b3d5f7a",
  loginName: "sari",
  fullName: "Sari Dewi",
  createdAt: "2026-11-19T07:05:00Z",
};

/** A page of the list in the shape of ENG-76. */
function page(data: User[], { page = 1, totalItems = data.length, totalPages = 1 } = {}) {
  return HttpResponse.json({ data, pagination: { page, pageSize: 50, totalItems, totalPages } });
}

/** The backend lists these accounts, and notes the URL of every request for the list. */
function listing(users: () => User[], requests: URL[] = []) {
  return http.get(USERS, ({ request }) => {
    requests.push(new URL(request.url));
    return page(users());
  });
}

/**
 * Waits for the account's row in the list. Its name alone is not enough: the sidebar shows the
 * logged-in user's name as well (FE-8), often before the list has loaded.
 */
function listed(fullName: string) {
  return screen.findByRole("cell", { name: fullName });
}

const never = () =>
  new Promise<never>(() => {
    // Never answers.
  });

function openAddUser() {
  fireEvent.click(screen.getByRole("button", { name: "Add user" }));
  return screen.getByRole("dialog", { name: "Add user" });
}

function fillIn(dialog: HTMLElement, user = { fullName: "Sari Dewi", loginName: "sari" }) {
  const field = (label: string) => within(dialog).getByLabelText(label);
  fireEvent.change(field("Full name"), { target: { value: user.fullName } });
  fireEvent.change(field("Login name"), { target: { value: user.loginName } });
  fireEvent.change(field("Password"), { target: { value: "another password" } });
}

function save(dialog: HTMLElement) {
  fireEvent.click(within(dialog).getByRole("button", { name: "Add user" }));
}

describe("Users", () => {
  beforeEach(() => {
    server.use(loggedIn());
    document.cookie = "XSRF-TOKEN=token-from-the-backend; path=/";
  });

  afterEach(() => {
    document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
  });

  it("lists every account with its login name and when it was added", async () => {
    const requests: URL[] = [];
    server.use(listing(() => [bayu, rinaAccount], requests));
    const { container } = renderRoute("/users");

    expect(await screen.findByRole("heading", { level: 1, name: "Users" })).toBeInTheDocument();
    const rows = await screen.findAllByRole("row");
    expect(rows.map((row) => row.textContent)).toEqual([
      "Full nameLogin nameAdded",
      "Bayu Santosobayu17 Nov 2026 09:00",
      "Rina Wulandaririna16 Nov 2026 10:15",
    ]);
    expect(screen.getByText("2 users")).toBeInTheDocument();
    expect(requests[0]?.searchParams.toString()).toBe("page=1");
    // One page: nothing to page through.
    expect(screen.queryByRole("button", { name: "Next page" })).not.toBeInTheDocument();
    await expectNoAxeViolations(container);
  });

  it("shows a skeleton while the list loads", async () => {
    server.use(http.get(USERS, never));
    renderRoute("/users");

    // The toaster is a status region too, so the skeleton is found by what it says.
    expect((await screen.findByText("Loading…")).closest("[role=status]")).toBeInTheDocument();
  });

  it("says why the list failed to load, and loads it again on retry", async () => {
    server.use(http.get(USERS, () => problem(503, "SERVICE_UNAVAILABLE")));
    renderRoute("/users");

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The server is not ready. Wait a moment.",
    );
    server.use(listing(() => [rinaAccount]));
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));
    expect(await listed("Rina Wulandari")).toBeInTheDocument();
  });

  it("sorts by full name, and by another column when its header is pressed", async () => {
    const requests: URL[] = [];
    server.use(listing(() => [bayu, rinaAccount], requests));
    const { router } = renderRoute("/users");

    const fullName = await screen.findByRole("columnheader", { name: "Full name" });
    expect(fullName).toHaveAttribute("aria-sort", "ascending");
    fireEvent.click(within(fullName).getByRole("button"));

    await waitFor(() => {
      expect(requests.at(-1)?.searchParams.get("sort")).toBe("-fullName");
    });
    expect(router.state.location.href).toBe("/users?sort=-fullName");
    expect(await screen.findByRole("columnheader", { name: "Full name" })).toHaveAttribute(
      "aria-sort",
      "descending",
    );

    const added = screen.getByRole("columnheader", { name: "Added" });
    expect(added).not.toHaveAttribute("aria-sort");
    fireEvent.click(within(added).getByRole("button"));
    await waitFor(() => {
      expect(requests.at(-1)?.searchParams.get("sort")).toBe("createdAt");
    });
  });

  it("opens the sort and page of a link", async () => {
    const requests: URL[] = [];
    server.use(listing(() => [rinaAccount], requests));
    renderRoute("/users?page=2&sort=-createdAt");

    await listed("Rina Wulandari");
    expect(requests[0]?.searchParams.toString()).toBe("page=2&sort=-createdAt");
  });

  it("pages through a list longer than a page", async () => {
    const requests: URL[] = [];
    server.use(
      http.get(USERS, ({ request }) => {
        const url = new URL(request.url);
        requests.push(url);
        return url.searchParams.get("page") === "2"
          ? page([sari], { page: 2, totalItems: 51, totalPages: 2 })
          : page([bayu, rinaAccount], { totalItems: 51, totalPages: 2 });
      }),
    );
    const { router, container } = renderRoute("/users");

    expect(await screen.findByText("51 users")).toBeInTheDocument();
    expect(screen.getByText("Page 1 of 2")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Previous page" })).toBeDisabled();
    await expectNoAxeViolations(container);
    fireEvent.click(screen.getByRole("button", { name: "Next page" }));

    expect(await screen.findByText("Sari Dewi")).toBeInTheDocument();
    expect(screen.getByText("Page 2 of 2")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Next page" })).toBeDisabled();
    expect(router.state.location.href).toBe("/users?page=2");
    expect(requests.at(-1)?.searchParams.get("page")).toBe("2");
  });

  it("says a page past the last is empty, and offers the first", async () => {
    server.use(
      http.get(USERS, ({ request }) =>
        new URL(request.url).searchParams.get("page") === "1"
          ? page([rinaAccount])
          : page([], { page: 4, totalItems: 1, totalPages: 1 }),
      ),
    );
    renderRoute("/users?page=4");

    expect(await screen.findByRole("heading", { name: "No users on this page" })).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Go to the first page" }));
    expect(await listed("Rina Wulandari")).toBeInTheDocument();
  });

  describe("Add user", () => {
    it("adds a user once the server has saved them, and says so", async () => {
      let users = [rinaAccount];
      let sent: Request | undefined;
      let answer: (() => void) | undefined;
      server.use(
        listing(() => users),
        http.post(USERS, async ({ request }) => {
          sent = request.clone();
          await new Promise<void>((resolve) => {
            answer = resolve;
          });
          users = [rinaAccount, sari];
          return HttpResponse.json(sari, {
            status: 201,
            headers: { Location: `/api/identity/users/${sari.id}` },
          });
        }),
      );
      const { container } = renderRoute("/users");
      await listed("Rina Wulandari");

      const dialog = openAddUser();
      expect(within(dialog).getByLabelText("Password")).toHaveAttribute(
        "autocomplete",
        "new-password",
      );
      await expectNoAxeViolations(container);
      fillIn(dialog);
      save(dialog);

      // Saving shows on the button, and nothing else changes until the server answers (FE-54).
      expect(await within(dialog).findByRole("button", { name: "Adding…" })).toBeDisabled();
      expect(screen.queryByText("Sari Dewi")).not.toBeInTheDocument();
      await waitFor(() => {
        expect(answer).toBeDefined();
      });
      answer?.();

      expect(await screen.findByText("Added Sari Dewi.")).toBeInTheDocument();
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
      expect(screen.getByRole("cell", { name: "Sari Dewi" })).toBeInTheDocument();
      expect(await sent?.json()).toEqual({
        fullName: "Sari Dewi",
        loginName: "sari",
        password: "another password",
      });
      expect(sent?.headers.get("Idempotency-Key")).toMatch(
        /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/,
      );
      expect(sent?.headers.get("X-XSRF-TOKEN")).toBe("token-from-the-backend");
    });

    it("asks for every field before it sends anything", async () => {
      let requests = 0;
      server.use(
        listing(() => [rinaAccount]),
        http.post(USERS, () => {
          requests += 1;
          return HttpResponse.json(sari, { status: 201 });
        }),
      );
      const { container } = renderRoute("/users");
      await listed("Rina Wulandari");

      const dialog = openAddUser();
      save(dialog);

      expect(await within(dialog).findByText("Enter their full name.")).toBeInTheDocument();
      expect(within(dialog).getByText("Enter a login name.")).toBeInTheDocument();
      expect(within(dialog).getByText("Enter a password.")).toBeInTheDocument();
      expect(within(dialog).getByLabelText("Login name")).toHaveAccessibleDescription(
        "Enter a login name. One word, used to log in.",
      );
      expect(requests).toBe(0);
      await expectNoAxeViolations(container);
    });

    it("shows the server's field errors on their fields, such as a taken login name", async () => {
      server.use(
        listing(() => [rinaAccount]),
        http.post(USERS, () =>
          HttpResponse.json(
            {
              status: 422,
              code: "VALIDATION_FAILED",
              errors: [
                { pointer: "/loginName", code: "LOGIN_NAME_TAKEN", detail: "Taken." },
                { pointer: "/password", code: "TOO_SHORT", detail: "Short." },
              ],
            },
            { status: 422, headers: { "Content-Type": "application/problem+json" } },
          ),
        ),
      );
      const { container } = renderRoute("/users");
      await listed("Rina Wulandari");

      const dialog = openAddUser();
      fillIn(dialog, { fullName: "Rina Kusuma", loginName: "rina" });
      save(dialog);

      const loginName = within(dialog).getByLabelText("Login name");
      await waitFor(() => {
        expect(loginName).toHaveAttribute("aria-invalid", "true");
      });
      expect(loginName).toHaveAccessibleDescription(
        "Another user has this login name. One word, used to log in.",
      );
      expect(within(dialog).getByLabelText("Password")).toHaveAccessibleDescription(
        "This is too short. At least 8 characters.",
      );
      // Field errors show on their fields only, never as an alert too (FE-55).
      expect(within(dialog).queryByRole("alert")).not.toBeInTheDocument();
      expect(within(dialog).getByRole("button", { name: "Add user" })).toBeEnabled();
      await expectNoAxeViolations(container);
    });

    it("shows any other failure as an alert above the buttons until the next try", async () => {
      server.use(
        listing(() => [rinaAccount]),
        http.post(USERS, () => problem(500, "INTERNAL_SERVER_ERROR")),
      );
      renderRoute("/users");
      await listed("Rina Wulandari");

      const dialog = openAddUser();
      fillIn(dialog);
      save(dialog);

      expect(await within(dialog).findByRole("alert")).toHaveTextContent(
        "Something went wrong on the server.",
      );
      server.use(http.post(USERS, never));
      save(dialog);
      await waitFor(() => {
        expect(within(dialog).queryByRole("alert")).not.toBeInTheDocument();
      });
    });

    it("makes one key per press, and sends it again on a retry of that press", async () => {
      const keys: (string | null)[] = [];
      let answers = 0;
      server.use(
        listing(() => [rinaAccount]),
        http.post(USERS, ({ request }) => {
          keys.push(request.headers.get("Idempotency-Key"));
          answers += 1;
          // No answer at all, then the first request still running, then saved.
          if (answers === 1) {
            return HttpResponse.error();
          }
          if (answers === 2) {
            return problem(409, "REQUEST_IN_PROGRESS");
          }
          return HttpResponse.json(sari, { status: 201 });
        }),
      );
      renderRoute("/users");
      await listed("Rina Wulandari");

      const first = openAddUser();
      fillIn(first);
      save(first);
      expect(await screen.findByText("Added Sari Dewi.", {}, { timeout: 8000 })).toBeVisible();
      expect(keys).toHaveLength(3);
      expect(new Set(keys).size).toBe(1);

      const second = openAddUser();
      fillIn(second, { fullName: "Bayu Santoso", loginName: "bayu" });
      save(second);
      await waitFor(() => {
        expect(keys).toHaveLength(4);
      });
      expect(keys[3]).not.toBe(keys[0]);
    }, 15_000);

    it("leaves the list as it was when cancelled", async () => {
      server.use(listing(() => [rinaAccount]));
      renderRoute("/users");
      await listed("Rina Wulandari");

      const dialog = openAddUser();
      fillIn(dialog);
      fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));

      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
      // A fresh form the next time.
      expect(within(openAddUser()).getByLabelText("Full name")).toHaveValue("");
    });
  });
});
