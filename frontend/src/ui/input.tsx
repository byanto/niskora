import type * as React from "react";
import { cn } from "@/lib/utils";

/** A 36 px control (FE-95) whose edge is `input-border` (FE-101). */
function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        "h-control w-full min-w-0 rounded-control border border-input-border bg-surface px-3 text-sm text-ink selection:bg-primary selection:text-primary-ink file:inline-flex file:h-control-sm file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-ink placeholder:text-muted disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50",
        "aria-invalid:border-late",
        className,
      )}
      {...props}
    />
  );
}

export { Input };
