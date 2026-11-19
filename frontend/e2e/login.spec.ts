import { expect, test } from "@playwright/test";
import { account } from "./account";
import { expectNoAxeViolations } from "./axe";

test("log in, see Home's empty state, log out", async ({ page }) => {
  // Nobody is logged in, so the app sends the user to Login, to come back afterwards (FE-72).
  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1, name: "Log in" })).toBeVisible();
  await expect(page).toHaveURL("/login?redirect=%2F");
  await expectNoAxeViolations(page);

  await page.getByLabel("Login name").fill(account.loginName);
  await page.getByLabel("Password").fill(account.password);
  await page.getByRole("button", { name: "Log in" }).click();

  await expect(page.getByRole("heading", { name: "Nothing imported yet" })).toBeVisible();
  await expect(page).toHaveURL("/");
  const sidebar = page.getByRole("complementary");
  await expect(sidebar.getByText(account.fullName)).toBeVisible();
  await expectNoAxeViolations(page);

  await sidebar.getByRole("button", { name: "Log out" }).click();
  await expect(page.getByRole("heading", { level: 1, name: "Log in" })).toBeVisible();
  await expect(page).toHaveURL("/login");

  // The session ended on the server: opening Home again asks to log in.
  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1, name: "Log in" })).toBeVisible();
});
