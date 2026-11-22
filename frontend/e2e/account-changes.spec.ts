import { expect, type Page, test } from "@playwright/test";
import { account } from "./account";
import { expectNoAxeViolations } from "./axe";

async function logIn(page: Page, loginName: string, password: string) {
  await page.getByLabel("Login name").fill(loginName);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Log in" }).click();
}

test("reset a password, disable and enable an account, and change your own password", async ({
  page,
  browser,
}) => {
  // Each run adds a user of its own: accounts are never deleted (PLT-45).
  const other = {
    loginName: `changed${String(Date.now())}`,
    fullName: `Changed Journey User ${String(Date.now())}`,
  };

  await page.goto("/users");
  await logIn(page, account.loginName, account.password);
  await expect(page.getByRole("heading", { level: 1, name: "Users" })).toBeVisible();

  // The journey's own row offers no change to itself.
  const own = page.getByRole("row").filter({ hasText: account.loginName });
  await expect(own.getByText("You")).toBeVisible();
  await expect(own.getByRole("button")).toHaveCount(0);

  await page.getByRole("button", { name: "Add user" }).click();
  const add = page.getByRole("dialog", { name: "Add user" });
  await add.getByLabel("Full name").fill(other.fullName);
  await add.getByLabel("Login name").fill(other.loginName);
  await add.getByLabel("Password").fill("their first password");
  await add.getByRole("button", { name: "Add user" }).click();
  await expect(add).toBeHidden();

  const row = page.getByRole("row").filter({ hasText: other.loginName });
  await row.getByRole("button", { name: "Reset password" }).click();
  const reset = page.getByRole("dialog", { name: `Reset password for ${other.fullName}` });
  await reset.getByLabel("New password").fill("their reset password");
  await expectNoAxeViolations(page);
  await reset.getByRole("button", { name: "Reset password" }).click();
  await expect(page.getByText(`${other.fullName}'s password is reset.`)).toBeVisible();

  // The other user logs in with the reset password, in a browser of their own.
  const theirs = await browser.newContext();
  const them = await theirs.newPage();
  await them.goto("/");
  await logIn(them, other.loginName, "their reset password");
  await expect(them.getByRole("heading", { name: "Nothing imported yet" })).toBeVisible();

  await row.getByRole("button", { name: "Disable" }).click();
  const disable = page.getByRole("dialog", { name: `Disable ${other.fullName}?` });
  await expectNoAxeViolations(page);
  await disable.getByRole("button", { name: "Disable" }).click();
  await expect(page.getByText(`Disabled ${other.fullName}.`)).toBeVisible();
  await expect(row.getByText("Disabled")).toBeVisible();

  // Their open session ended at once, and they cannot log in again.
  await them.reload();
  await expect(them.getByRole("heading", { level: 1, name: "Log in" })).toBeVisible();
  await logIn(them, other.loginName, "their reset password");
  await expect(them.getByRole("alert")).toHaveText(
    "This account is disabled. Ask another user to enable it.",
  );

  await row.getByRole("button", { name: "Enable" }).click();
  await page
    .getByRole("dialog", { name: `Enable ${other.fullName}?` })
    .getByRole("button", { name: "Enable" })
    .click();
  await expect(page.getByText(`Enabled ${other.fullName}.`)).toBeVisible();

  // Enabled again, they log in and change their own password from the sidebar.
  await logIn(them, other.loginName, "their reset password");
  const sidebar = them.getByRole("complementary");
  await sidebar.getByRole("button", { name: "Change password" }).click();
  const change = them.getByRole("dialog", { name: "Change your password" });
  await change.getByLabel("Current password").fill("their reset password");
  await change.getByLabel("New password").fill("their very own password");
  await expectNoAxeViolations(them);
  await change.getByRole("button", { name: "Change password" }).click();
  await expect(them.getByText("Your password is changed.")).toBeVisible();

  await sidebar.getByRole("button", { name: "Log out" }).click();
  await logIn(them, other.loginName, "their very own password");
  await expect(sidebar.getByText(other.fullName)).toBeVisible();
  await theirs.close();
});
