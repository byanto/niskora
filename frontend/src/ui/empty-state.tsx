import type { LucideIcon } from "lucide-react";
import type * as React from "react";
import { cn } from "@/lib/utils";

interface EmptyStateProps extends Omit<React.ComponentProps<"section">, "title"> {
  icon: LucideIcon;
  /** What is missing, as a short heading. */
  title: string;
  /** Why it is missing, or what fills it. */
  description: string;
  /** The next action, such as a button or a link to the screen where it is done. */
  action?: React.ReactNode;
}

/**
 * The empty state of a screen or a part of one: it says what is missing and offers the next
 * action (FE-52). The caller translates the text.
 */
export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  className,
  ...props
}: EmptyStateProps) {
  return (
    <section
      data-slot="empty-state"
      className={cn(
        "flex flex-col items-center gap-2 rounded-card border bg-surface px-card py-12 text-center",
        className,
      )}
      {...props}
    >
      <Icon aria-hidden="true" className="mb-2 size-8 text-muted" />
      <h2 className="text-h2 font-semibold">{title}</h2>
      <p className="max-w-md text-muted">{description}</p>
      {action === undefined ? null : <div className="mt-2">{action}</div>}
    </section>
  );
}
