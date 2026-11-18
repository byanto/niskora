import { cva, type VariantProps } from "class-variance-authority";
// One module per primitive: the package index pulls in all of Radix, which slows tests and dev.
import * as Slot from "radix-ui/slot";
import type * as React from "react";
import { cn } from "@/lib/utils";

/**
 * Controls are 36 px high and small buttons 28 px (FE-95), with 6 px corners (FE-97). The edge of
 * an outline button is `input-border` (FE-101). Keyboard focus is the outline set in index.css.
 */
const buttonVariants = cva(
  "inline-flex shrink-0 cursor-pointer items-center justify-center gap-2 rounded-control text-sm font-medium whitespace-nowrap transition-colors disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        primary: "bg-primary text-primary-ink hover:brightness-110",
        outline: "border border-input-border bg-surface text-ink hover:bg-surface-2",
        ghost: "text-ink hover:bg-surface-2",
        link: "text-link underline-offset-4 hover:underline",
      },
      size: {
        default: "h-control px-4",
        sm: "h-control-sm px-3 text-xs",
        icon: "size-control",
        "icon-sm": "size-control-sm",
      },
    },
    defaultVariants: {
      variant: "primary",
      size: "default",
    },
  },
);

function Button({
  className,
  variant = "primary",
  size = "default",
  asChild = false,
  ...props
}: React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean;
  }) {
  const Comp = asChild ? Slot.Root : "button";

  return (
    <Comp
      data-slot="button"
      data-variant={variant}
      data-size={size}
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  );
}

export { Button, buttonVariants };
