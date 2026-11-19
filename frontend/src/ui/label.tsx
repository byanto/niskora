import type * as React from "react";
import { cn } from "@/lib/utils";

/** The label of a form field, set above its control. The caller ties it on with `htmlFor`. */
function Label({ className, ...props }: React.ComponentProps<"label">) {
  return (
    <label data-slot="label" className={cn("text-sm font-medium text-ink", className)} {...props} />
  );
}

export { Label };
