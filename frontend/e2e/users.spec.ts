import { expect, type Page, test } from "@playwright/test";
import { account } from "./account";
import { expectNoAxeViolations } from "./axe";

async function logIn(page: Page, loginName: string, password: string) {
  await page.getByLabel("Login name").fill(loginName);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Log in" }).click();
}

test("add a user, who then logs in with their own account", async ({ page }) => {
  // Each run adds a user of its own: accounts are never deleted (PLT-45).
  const added = {
    loginName: `added${String(Date.now())}`,
    fullName: "Added Journey User",
    password: "added user password",
  };

  await page.goto("/users");
  await expect(page.getByRole("heading", { level: 1, name: "Log in" })).toBeVisible();
  await logIn(page, account.loginName, account.password);

  await expect(page.getByRole("heading", { level: 1, name: "Users" })).toBeVisible();
  await expect(page.getByRole("cell", { name: account.fullName, exact: true })).toBeVisible();
  await expectNoAxeViolations(page);

  await page.getByRole("button", { name: "Add user" }).click();
  const dialog = page.getByRole("dialog", { name: "Add user" });
  await dialog.getByLabel("Full name").fill(added.fullName);
  await dialog.getByLabel("Login name").fill(added.loginName);
  await dialog.getByLabel("Password").fill(added.password);
  await expectNoAxeViolations(page);
  await dialog.getByRole("button", { name: "Add user" }).click();

  await expect(page.getByText(`Added ${added.fullName}.`)).toBeVisible();
  await expect(dialog).toBeHidden();
  await expect(page.getByRole("cell", { name: added.loginName, exact: true })).toBeVisible();

  // The same login name again is a field error, whatever its case.
  await page.getByRole("button", { name: "Add user" }).click();
  await dialog.getByLabel("Full name").fill("Someone Else");
  await dialog.getByLabel("Login name").fill(added.loginName.toUpperCase());
  await dialog.getByLabel("Password").fill(added.password);
  await dialog.getByRole("button", { name: "Add user" }).click();
  await expect(dialog.getByText("Another user has this login name.")).toBeVisible();
  await dialog.getByRole("button", { name: "Cancel" }).click();

  const sidebar = page.getByRole("complementary");
  await sidebar.getByRole("button", { name: "Log out" }).click();
  await expect(page.getByRole("heading", { level: 1, name: "Log in" })).toBeVisible();

  await logIn(page, added.loginName, added.password);
  await expect(page.getByRole("heading", { name: "Nothing imported yet" })).toBeVisible();
  await expect(sidebar.getByText(added.fullName)).toBeVisible();
  await sidebar.getByRole("button", { name: "Log out" }).click();
  await expect(page.getByRole("heading", { level: 1, name: "Log in" })).toBeVisible();
});
