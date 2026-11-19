import { screen, within } from "@testing-library/react";

/** A toast with this text (FE-55), in the notifications region the app always has. */
export async function findToast(text: string) {
  const notifications = await screen.findByRole("region", { name: /^Notifications/ });
  return within(notifications).findByText(text);
}
