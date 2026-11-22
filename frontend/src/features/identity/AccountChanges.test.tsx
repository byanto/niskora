import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { expectNoAxeViolations } from "@/test/axe";
import { renderRoute } from "@/test/render";
import { server } from "@/test/server";
import { loggedIn, problem, rina } from "@/test/session";

const USERS = "*/api/identity/users";

interface Account {
  id: string;
  loginName: string;
  fullName: string;
  createdAt: string;
  version: number;
  isDisabled: boolean;
}

const rinaAccount: Account = {
  ...rina,
  createdAt: "2026-11-16T03:15:00Z",
  version: 0,
  isDisabled: false,
};
const bayu: Account = {
  id: "8c2d4e6f-1a3b-4c5d-8e7f-9a0b1c2d3e4f",
  loginName: "bayu",
  fullName: "Bayu Santoso",
  createdAt: "2026-11-17T02:00:00Z",
  version: 3,
  isDisabled: false,
};
const sari = { id: "2f4a6c8e-0b1d-4f3a-9c5e-7d9f1b3d5f7a", fullName: "Sari Dewi" };

/** The backend lists these accounts, as they are when it is asked (ENG-76). */
function listing(accounts: () => Account[]) {
  return http.get(USERS, () => {
    const data = accounts();
    return HttpResponse.json({
      data,
      pagination: { page: 1, pageSize: 50, totalItems: data.length, totalPages: 1 },
    });
  });
}

/** One account with its version as its ETag (ENG-87). */
function reading(account: () => Account) {
  return http.get(`${USERS}/:id`, () =>
    HttpResponse.json(account(), { headers: { ETag: `"${String(account().version)}"` } }),
  );
}

/** A change the backend refused with a 412: who changed the account and when (FE-81). */
function changedMeanwhile(changedBy: typeof sari | null = sari) {
  return HttpResponse.json(
    {
      status: 412,
      code: "VERSION_MISMATCH",
      changedAt: "2026-11-22T07:05:00Z",
      changedBy,
    },
    { status: 412, headers: { "Content-Type": "application/problem+json" } },
  );
}

interface Sent {
  url: string;
  ifMatch: string | null;
  idempotencyKey: string | null;
  body: unknown;
}

async function noted(request: Request, sent: Sent[]) {
  const text = await request.text();
  sent.push({
    url: new URL(request.url).pathname,
    ifMatch: request.headers.get("If-Match"),
    idempotencyKey: request.headers.get("Idempotency-Key"),
    body: text === "" ? null : (JSON.parse(text) as unknown),
  });
}

function rowOf(fullName: string) {
  const cell = screen.getByRole("cell", { name: new RegExp(`^${fullName}`) });
  const row = cell.closest("tr");
  if (row === null) {
    throw new Error(`${fullName} has no row`);
  }
  return row;
}

async function listed(fullName: string) {
  await screen.findByRole("cell", { name: new RegExp(`^${fullName}`) });
  return rowOf(fullName);
}

describe("Changing an account", () => {
  beforeEach(() => {
    server.use(loggedIn());
    document.cookie = "XSRF-TOKEN=token-from-the-backend; path=/";
  });

  afterEach(() => {
    document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
  });

  it("offers reset and disable on another user's row, and neither on your own", async () => {
    const disabled: Account = { ...bayu, id: sari.id, fullName: "Sari Dewi", isDisabled: true };
    server.use(listing(() => [bayu, rinaAccount, disabled]));
    const { container } = renderRoute("/users");

    const bayus = await listed("Bayu Santoso");
    expect(within(bayus).getByRole("button", { name: "Reset password" })).toBeVisible();
    expect(within(bayus).getByRole("button", { name: "Disable" })).toBeVisible();
    expect(within(bayus).getByText("Active")).toBeVisible();

    const own = rowOf("Rina Wulandari");
    expect(within(own).getByText("You")).toBeVisible();
    expect(within(own).queryByRole("button")).not.toBeInTheDocument();

    const saris = rowOf("Sari Dewi");
    expect(within(saris).getByText("Disabled")).toBeVisible();
    expect(within(saris).getByRole("button", { name: "Enable" })).toBeVisible();
    expect(within(saris).queryByRole("button", { name: "Disable" })).not.toBeInTheDocument();
    await expectNoAxeViolations(container);
  });

  describe("Reset password", () => {
    it("gives the user a new password with the version it read, and says so", async () => {
      let accounts = [bayu, rinaAccount];
      const sent: Sent[] = [];
      server.use(
        listing(() => accounts),
        http.post(`${USERS}/:id/reset-password`, async ({ request }) => {
          await noted(request, sent);
          accounts = [{ ...bayu, version: 4 }, rinaAccount];
          return HttpResponse.json(accounts[0], { headers: { ETag: '"4"' } });
        }),
      );
      const { container } = renderRoute("/users");

      fireEvent.click(
        within(await listed("Bayu Santoso")).getByRole("button", { name: "Reset password" }),
      );
      const dialog = screen.getByRole("dialog", { name: "Reset password for Bayu Santoso" });
      const password = within(dialog).getByLabelText("New password");
      expect(password).toHaveAttribute("autocomplete", "new-password");
      await expectNoAxeViolations(container);
      fireEvent.change(password, { target: { value: "a brand new password" } });
      fireEvent.click(within(dialog).getByRole("button", { name: "Reset password" }));

      expect(await screen.findByText("Bayu Santoso's password is reset.")).toBeInTheDocument();
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
      expect(sent).toEqual([
        {
          url: `/api/identity/users/${bayu.id}/reset-password`,
          ifMatch: '"3"',
          idempotencyKey: expect.stringMatching(/^[0-9a-f-]{36}$/) as unknown,
          body: { password: "a brand new password" },
        },
      ]);
    });

    it("asks for the new password, and shows the server's field error on it", async () => {
      let requests = 0;
      server.use(
        listing(() => [bayu, rinaAccount]),
        http.post(`${USERS}/:id/reset-password`, () => {
          requests += 1;
          return HttpResponse.json(
            {
              status: 422,
              code: "VALIDATION_FAILED",
              errors: [{ pointer: "/password", code: "TOO_SHORT", detail: "Short." }],
            },
            { status: 422, headers: { "Content-Type": "application/problem+json" } },
          );
        }),
      );
      renderRoute("/users");

      fireEvent.click(
        within(await listed("Bayu Santoso")).getByRole("button", { name: "Reset password" }),
      );
      const dialog = screen.getByRole("dialog", { name: "Reset password for Bayu Santoso" });
      fireEvent.click(within(dialog).getByRole("button", { name: "Reset password" }));
      expect(await within(dialog).findByText("Enter a new password.")).toBeInTheDocument();
      expect(requests).toBe(0);

      fireEvent.change(within(dialog).getByLabelText("New password"), {
        target: { value: "short" },
      });
      fireEvent.click(within(dialog).getByRole("button", { name: "Reset password" }));
      await waitFor(() => {
        expect(within(dialog).getByLabelText("New password")).toHaveAccessibleDescription(
          "This is too short. At least 8 characters.",
        );
      });
      expect(within(dialog).queryByRole("alert")).not.toBeInTheDocument();
    });
  });

  describe("Disable and enable", () => {
    it("disables a user once they confirm, and the list then offers Enable", async () => {
      let accounts = [bayu, rinaAccount];
      const sent: Sent[] = [];
      server.use(
        listing(() => accounts),
        http.post(`${USERS}/:id/disable`, async ({ request }) => {
          await noted(request, sent);
          accounts = [{ ...bayu, version: 4, isDisabled: true }, rinaAccount];
          return HttpResponse.json(accounts[0], { headers: { ETag: '"4"' } });
        }),
      );
      const { container } = renderRoute("/users");

      fireEvent.click(
        within(await listed("Bayu Santoso")).getByRole("button", { name: "Disable" }),
      );
      const dialog = screen.getByRole("dialog", { name: "Disable Bayu Santoso?" });
      expect(dialog).toHaveTextContent("They are logged out at once");
      await expectNoAxeViolations(container);
      fireEvent.click(within(dialog).getByRole("button", { name: "Disable" }));

      expect(await screen.findByText("Disabled Bayu Santoso.")).toBeInTheDocument();
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
      expect(within(rowOf("Bayu Santoso")).getByRole("button", { name: "Enable" })).toBeVisible();
      expect(sent).toEqual([
        {
          url: `/api/identity/users/${bayu.id}/disable`,
          ifMatch: '"3"',
          idempotencyKey: expect.stringMatching(/^[0-9a-f-]{36}$/) as unknown,
          body: null,
        },
      ]);
    });

    it("enables a disabled user once they confirm", async () => {
      let accounts = [{ ...bayu, isDisabled: true }, rinaAccount];
      const sent: Sent[] = [];
      server.use(
        listing(() => accounts),
        http.post(`${USERS}/:id/enable`, async ({ request }) => {
          await noted(request, sent);
          accounts = [{ ...bayu, version: 4 }, rinaAccount];
          return HttpResponse.json(accounts[0]);
        }),
      );
      renderRoute("/users");

      fireEvent.click(within(await listed("Bayu Santoso")).getByRole("button", { name: "Enable" }));
      const dialog = screen.getByRole("dialog", { name: "Enable Bayu Santoso?" });
      fireEvent.click(within(dialog).getByRole("button", { name: "Enable" }));

      expect(await screen.findByText("Enabled Bayu Santoso.")).toBeInTheDocument();
      expect(within(rowOf("Bayu Santoso")).getByRole("button", { name: "Disable" })).toBeVisible();
      expect(sent[0]?.ifMatch).toBe('"3"');
    });

    it("shows a refusal as an alert above the buttons", async () => {
      server.use(
        listing(() => [bayu, rinaAccount]),
        http.post(`${USERS}/:id/disable`, () => problem(500, "INTERNAL_SERVER_ERROR")),
      );
      renderRoute("/users");

      fireEvent.click(
        within(await listed("Bayu Santoso")).getByRole("button", { name: "Disable" }),
      );
      const dialog = screen.getByRole("dialog", { name: "Disable Bayu Santoso?" });
      fireEvent.click(within(dialog).getByRole("button", { name: "Disable" }));

      expect(await within(dialog).findByRole("alert")).toHaveTextContent(
        "Something went wrong on the server.",
      );
    });
  });

  describe("When someone else changed the account meanwhile", () => {
    it("keeps what was typed, says who changed it and when, and loads the latest on request", async () => {
      let latest = bayu;
      const sent: Sent[] = [];
      server.use(
        listing(() => [latest, rinaAccount]),
        reading(() => latest),
        http.post(`${USERS}/:id/reset-password`, async ({ request }) => {
          await noted(request, sent);
          return sent.length === 1 ? changedMeanwhile() : HttpResponse.json(latest);
        }),
      );
      const { container } = renderRoute("/users");

      fireEvent.click(
        within(await listed("Bayu Santoso")).getByRole("button", { name: "Reset password" }),
      );
      const dialog = screen.getByRole("dialog", { name: "Reset password for Bayu Santoso" });
      fireEvent.change(within(dialog).getByLabelText("New password"), {
        target: { value: "a brand new password" },
      });
      // Sari changes Bayu's account before Rina presses the button.
      latest = { ...bayu, version: 4, isDisabled: true };
      fireEvent.click(within(dialog).getByRole("button", { name: "Reset password" }));

      expect(await within(dialog).findByRole("alert")).toHaveTextContent(
        "Sari Dewi changed this at 22 Nov 2026 14:05, after you opened it.",
      );
      expect(within(dialog).getByLabelText("New password")).toHaveValue("a brand new password");
      // Nothing is retried by itself, and the change waits for the latest version.
      expect(sent).toHaveLength(1);
      expect(within(dialog).getByRole("button", { name: "Reset password" })).toBeDisabled();
      await expectNoAxeViolations(container);

      fireEvent.click(within(dialog).getByRole("button", { name: "Load the latest version" }));
      await waitFor(() => {
        expect(within(dialog).queryByRole("alert")).not.toBeInTheDocument();
      });
      expect(within(dialog).getByLabelText("New password")).toHaveValue("a brand new password");
      expect(sent).toHaveLength(1);

      fireEvent.click(within(dialog).getByRole("button", { name: "Reset password" }));
      expect(await screen.findByText("Bayu Santoso's password is reset.")).toBeInTheDocument();
      expect(sent[1]?.ifMatch).toBe('"4"');
      expect(sent[1]?.body).toEqual({ password: "a brand new password" });
      expect(sent[1]?.idempotencyKey).not.toBe(sent[0]?.idempotencyKey);
    });

    it("names no one when no user made the change", async () => {
      server.use(
        listing(() => [bayu, rinaAccount]),
        http.post(`${USERS}/:id/disable`, () => changedMeanwhile(null)),
      );
      renderRoute("/users");

      fireEvent.click(
        within(await listed("Bayu Santoso")).getByRole("button", { name: "Disable" }),
      );
      const dialog = screen.getByRole("dialog", { name: "Disable Bayu Santoso?" });
      fireEvent.click(within(dialog).getByRole("button", { name: "Disable" }));

      expect(await within(dialog).findByRole("alert")).toHaveTextContent(
        "This was changed at 22 Nov 2026 14:05, after you opened it.",
      );
    });

    it("says so when the latest version is already what was asked", async () => {
      let latest = bayu;
      server.use(
        listing(() => [latest, rinaAccount]),
        reading(() => latest),
        http.post(`${USERS}/:id/disable`, () => changedMeanwhile()),
      );
      renderRoute("/users");

      fireEvent.click(
        within(await listed("Bayu Santoso")).getByRole("button", { name: "Disable" }),
      );
      const dialog = screen.getByRole("dialog", { name: "Disable Bayu Santoso?" });
      latest = { ...bayu, version: 4, isDisabled: true };
      fireEvent.click(within(dialog).getByRole("button", { name: "Disable" }));
      fireEvent.click(
        await within(dialog).findByRole("button", { name: "Load the latest version" }),
      );

      expect(await within(dialog).findByText("Bayu Santoso is already disabled.")).toBeVisible();
      expect(within(dialog).queryByRole("button", { name: "Disable" })).not.toBeInTheDocument();
      fireEvent.click(within(dialog).getByRole("button", { name: "Done" }));
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
  });

  describe("Change your own password", () => {
    it("changes it from the sidebar, giving the current one, and says so", async () => {
      const sent: Sent[] = [];
      server.use(
        listing(() => [rinaAccount]),
        reading(() => ({ ...rinaAccount, version: 2 })),
        http.post(`${USERS}/:id/change-password`, async ({ request }) => {
          await noted(request, sent);
          return HttpResponse.json({ ...rinaAccount, version: 3 });
        }),
      );
      const { container } = renderRoute("/users");

      const sidebar = await screen.findByRole("complementary");
      fireEvent.click(await within(sidebar).findByRole("button", { name: "Change password" }));
      const dialog = screen.getByRole("dialog", { name: "Change your password" });
      // The dialog reads the account first, for the version the change is based on.
      const current = await within(dialog).findByLabelText("Current password");
      const next = within(dialog).getByLabelText("New password");
      expect(current).toHaveAttribute("autocomplete", "current-password");
      expect(next).toHaveAttribute("autocomplete", "new-password");
      await expectNoAxeViolations(container);
      fireEvent.change(current, { target: { value: "correct horse battery" } });
      fireEvent.change(next, { target: { value: "a brand new password" } });
      fireEvent.click(within(dialog).getByRole("button", { name: "Change password" }));

      expect(await screen.findByText("Your password is changed.")).toBeInTheDocument();
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
      expect(sent).toEqual([
        {
          url: `/api/identity/users/${rina.id}/change-password`,
          ifMatch: '"2"',
          idempotencyKey: expect.stringMatching(/^[0-9a-f-]{36}$/) as unknown,
          body: { currentPassword: "correct horse battery", newPassword: "a brand new password" },
        },
      ]);
    });

    it("shows a wrong current password on its field", async () => {
      server.use(
        listing(() => [rinaAccount]),
        reading(() => rinaAccount),
        http.post(`${USERS}/:id/change-password`, () =>
          HttpResponse.json(
            {
              status: 422,
              code: "VALIDATION_FAILED",
              errors: [{ pointer: "/currentPassword", code: "WRONG_PASSWORD", detail: "Wrong." }],
            },
            { status: 422, headers: { "Content-Type": "application/problem+json" } },
          ),
        ),
      );
      renderRoute("/users");

      const sidebar = await screen.findByRole("complementary");
      fireEvent.click(await within(sidebar).findByRole("button", { name: "Change password" }));
      const dialog = screen.getByRole("dialog", { name: "Change your password" });
      fireEvent.change(await within(dialog).findByLabelText("Current password"), {
        target: { value: "not my password" },
      });
      fireEvent.change(within(dialog).getByLabelText("New password"), {
        target: { value: "a brand new password" },
      });
      fireEvent.click(within(dialog).getByRole("button", { name: "Change password" }));

      await waitFor(() => {
        expect(within(dialog).getByLabelText("Current password")).toHaveAccessibleDescription(
          "This is not your current password.",
        );
      });
    });
  });
});
