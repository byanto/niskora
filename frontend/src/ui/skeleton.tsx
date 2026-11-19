import type * as React from "react";
import { useTranslation } from "react-i18next";
import { cn } from "@/lib/utils";

/** One block of a skeleton. The caller sizes it to the content it stands for. */
function Skeleton({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="skeleton"
      className={cn("rounded-control bg-surface-2 motion-safe:animate-pulse", className)}
      {...props}
    />
  );
}

/**
 * A screen or a part of one while it loads: a skeleton in the shape of the content, never a blank
 * page or a spinner (FE-51). A screen reader hears "Loading…" instead of the blocks.
 */
function Loading({ className, children, ...props }: React.ComponentProps<"div">) {
  const { t } = useTranslation();
  return (
    <div role="status" className={cn("flex flex-col gap-4", className)} {...props}>
      <span className="sr-only">{t("loading")}</span>
      <div aria-hidden="true" className="contents">
        {children}
      </div>
    </div>
  );
}

export { Loading, Skeleton };
