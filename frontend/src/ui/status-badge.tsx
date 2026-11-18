import type { LucideIcon } from "lucide-react";
// One module per icon: the package index pulls in all of Lucide, which slows tests and dev.
import CircleCheck from "lucide-react/icons/circle-check";
import Info from "lucide-react/icons/info";
import OctagonAlert from "lucide-react/icons/octagon-alert";
import TriangleAlert from "lucide-react/icons/triangle-alert";
import type * as React from "react";
import { Badge } from "./badge";

type BadgeProps = React.ComponentProps<typeof Badge>;

/**
 * The states niskora marks: a restock level (FE-103), data freshness (FE-104) and the severity
 * of a "needs attention" item (FE-105). Each has its own icon, so it reads without colour (FE-58).
 */
export type Status = "late" | "now" | "fine" | "info";

/** Each state's icon (FE-103) and badge tone: the one place they are paired. */
const looks: Record<Status, { icon: LucideIcon; tone: NonNullable<BadgeProps["tone"]> }> = {
  late: { icon: OctagonAlert, tone: "late" },
  now: { icon: TriangleAlert, tone: "now" },
  fine: { icon: CircleCheck, tone: "fine" },
  info: { icon: Info, tone: "neutral" },
};

/** The icon of a state, beside text that names it. */
export function StatusIcon({
  status,
  ...props
}: { status: Status } & React.ComponentProps<LucideIcon>) {
  const Icon = looks[status].icon;
  return <Icon aria-hidden="true" {...props} />;
}

/** A state as a soft badge: its icon and its word, which the caller translates. */
export function StatusBadge({
  status,
  children,
  ...props
}: { status: Status; children: React.ReactNode } & Omit<BadgeProps, "tone">) {
  return (
    <Badge tone={looks[status].tone} {...props}>
      <StatusIcon status={status} />
      {children}
    </Badge>
  );
}
