import type * as React from "react";
import { useTranslation } from "react-i18next";
import { errorKey } from "@/lib/api/problem";
import { cn } from "@/lib/utils";
import { StatusIcon } from "./status-badge";

/**
 * Why an action failed, as an alert next to it, just above its buttons. The caller shows it until
 * the user tries again (FE-55). Field errors show on their fields instead.
 */
export function ActionFailed({
  error,
  className,
  ...props
}: { error: unknown } & React.ComponentProps<"p">) {
  const { t } = useTranslation();
  return (
    <p
      role="alert"
      className={cn(
        "flex items-start gap-2 rounded-control border border-late-line bg-late-bg p-3 text-sm text-late",
        className,
      )}
      {...props}
    >
      <span className="flex h-5 shrink-0 items-center">
        <StatusIcon status="late" className="size-4" />
      </span>
      {t(`errors.${errorKey(error)}`)}
    </p>
  );
}
