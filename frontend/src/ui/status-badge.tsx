import type { LucideIcon } from "lucide-react";
// One module per icon: the package index pulls in all of Lucide, which slows tests and dev.
import CircleCheck from "lucide-react/icons/circle-check";
import Info from "lucide-react/icons/info";
import OctagonAlert from "lucide-react/icons/octagon-alert";
import TriangleAlert from "lucide-react/icons/triangle-alert";
import type * as React from "react";
import { Badge } from "./badge";

/**
 * The states niskora marks: a restock level (FE-103), data freshness (FE-104) and the severity
 * of a "needs attention" item (FE-105). Each has its own icon, so it reads without colour (FE-58).
 */
export type Status = "late" | "now" | "fine" | "info";

const icons: Record<Status, LucideIcon> = {
  late: OctagonAlert,
  now: TriangleAlert,
  fine: CircleCheck,
  info: Info,
};

const tones = { late: "late", now: "now", fine: "fine", info: "neutral" } as const;

/** The icon of a state, beside text that names it. */
export function StatusIcon({
  status,
  ...props
}: { status: Status } & React.ComponentProps<LucideIcon>) {
  const Icon = icons[status];
  return <Icon aria-hidden="true" {...props} />;
}

/** A state as a soft badge: its icon and its word, which the caller translates. */
export function StatusBadge({
  status,
  children,
  ...props
}: { status: Status; children: React.ReactNode } & Omit<
  React.ComponentProps<typeof Badge>,
  "tone"
>) {
  return (
    <Badge tone={tones[status]} {...props}>
      <StatusIcon status={status} />
      {children}
    </Badge>
  );
}
