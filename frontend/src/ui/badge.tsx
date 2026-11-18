import { cva, type VariantProps } from "class-variance-authority";
// One module per primitive: the package index pulls in all of Radix, which slows tests and dev.
import * as Slot from "radix-ui/slot";
import type * as React from "react";
import { cn } from "@/lib/utils";

/** A soft, fully round badge (FE-97, FE-103). The tone is one of the state colours of FE-99. */
const badgeVariants = cva(
  "inline-flex h-6 w-fit shrink-0 items-center gap-1 overflow-hidden rounded-full border px-2 text-xs font-semibold whitespace-nowrap [&>svg]:pointer-events-none [&>svg]:size-4 [&>svg]:shrink-0",
  {
    variants: {
      tone: {
        neutral: "border-border bg-surface-2 text-muted",
        late: "border-late-line bg-late-bg text-late",
        now: "border-now-line bg-now-bg text-now",
        fine: "border-fine-line bg-fine-bg text-fine",
      },
    },
    defaultVariants: {
      tone: "neutral",
    },
  },
);

function Badge({
  className,
  tone = "neutral",
  asChild = false,
  ...props
}: React.ComponentProps<"span"> & VariantProps<typeof badgeVariants> & { asChild?: boolean }) {
  const Comp = asChild ? Slot.Root : "span";

  return (
    <Comp
      data-slot="badge"
      data-tone={tone}
      className={cn(badgeVariants({ tone }), className)}
      {...props}
    />
  );
}

export { Badge, badgeVariants };
