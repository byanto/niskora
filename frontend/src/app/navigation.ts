import type { LinkProps } from "@tanstack/react-router";
import type { LucideIcon } from "lucide-react";
// One module per icon: the package index pulls in all of Lucide, which slows tests and dev.
import House from "lucide-react/icons/house";
import type { common } from "@/lib/i18n/en/common";

type Group = keyof typeof common.nav.groups;
type Screen = keyof typeof common.nav.screens;

interface NavItem {
  screen: Screen;
  to: NonNullable<LinkProps["to"]>;
  icon: LucideIcon;
}

/**
 * The sidebar: the screens grouped by job, in the order of FE-2. A screen is listed once its route
 * exists, and a group shows once it has a screen.
 */
export const navigation: readonly { group: Group; items: readonly NavItem[] }[] = [
  { group: "daily", items: [{ screen: "home", to: "/", icon: House }] },
  { group: "catalogue", items: [] },
  { group: "buying", items: [] },
  { group: "analysis", items: [] },
  { group: "setup", items: [] },
];
