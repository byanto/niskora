import type * as React from "react";
import { useTranslation } from "react-i18next";
import { errorKey } from "@/lib/api/problem";
import { cn } from "@/lib/utils";
import { Button } from "./button";
import { StatusIcon } from "./status-badge";

interface LoadFailedProps extends React.ComponentProps<"div"> {
  /** What the query threw: a `ProblemError`, or the TypeError of a request that got no answer. */
  error: unknown;
  /** Loads again, usually the query's `refetch`. */
  onRetry: () => void;
}

/**
 * A screen or a part of one that failed to load: the translated Problem Details message with a
 * retry button, inside the shell (FE-53).
 */
export function LoadFailed({ error, onRetry, className, ...props }: LoadFailedProps) {
  const { t } = useTranslation();
  return (
    <div
      role="alert"
      data-slot="load-failed"
      className={cn(
        "flex flex-wrap items-center gap-4 rounded-card border border-late-line bg-late-bg p-card text-late",
        className,
      )}
      {...props}
    >
      <StatusIcon status="late" className="size-5 shrink-0" />
      <div className="min-w-0 flex-1">
        <p className="font-semibold">{t("loadFailed.title")}</p>
        <p>{t(`errors.${errorKey(error)}`)}</p>
      </div>
      <Button variant="outline" onClick={onRetry}>
        {t("loadFailed.retry")}
      </Button>
    </div>
  );
}
