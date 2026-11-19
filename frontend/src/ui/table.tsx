import ArrowDown from "lucide-react/icons/arrow-down";
import ArrowUp from "lucide-react/icons/arrow-up";
import ArrowUpDown from "lucide-react/icons/arrow-up-down";
import type * as React from "react";
import { cn } from "@/lib/utils";

/**
 * A data table: `surface` with a light `border` and 8 px corners (FE-97). It scrolls sideways
 * inside its own box, so the page never does (FE-108). Rows are 48 px and the header row 40 px,
 * with 12 px cell padding (FE-95).
 */
function Table({ className, ...props }: React.ComponentProps<"table">) {
  return (
    <div
      data-slot="table-container"
      className="w-full overflow-x-auto rounded-card border bg-surface"
    >
      <table data-slot="table" className={cn("w-full caption-bottom", className)} {...props} />
    </div>
  );
}

function TableHeader({ className, ...props }: React.ComponentProps<"thead">) {
  return <thead data-slot="table-header" className={cn("[&_tr]:border-b", className)} {...props} />;
}

function TableBody({ className, ...props }: React.ComponentProps<"tbody">) {
  return (
    <tbody
      data-slot="table-body"
      className={cn("[&_tr:last-child]:border-0", className)}
      {...props}
    />
  );
}

function TableRow({ className, ...props }: React.ComponentProps<"tr">) {
  return (
    <tr
      data-slot="table-row"
      className={cn("h-row border-b transition-colors hover:bg-surface-2", className)}
      {...props}
    />
  );
}

/** A header cell: 40 px, 12 px text in `muted` (FE-92, FE-95). */
function TableHead({ className, ...props }: React.ComponentProps<"th">) {
  return (
    <th
      data-slot="table-head"
      className={cn(
        "h-row-header px-cell text-left align-middle text-xs font-medium whitespace-nowrap text-muted",
        className,
      )}
      {...props}
    />
  );
}

function TableCell({ className, ...props }: React.ComponentProps<"td">) {
  return (
    <td
      data-slot="table-cell"
      className={cn("px-cell align-middle whitespace-nowrap", className)}
      {...props}
    />
  );
}

/** How a column is sorted, as `aria-sort` names it, or `undefined` when it is not. */
type SortDirection = "ascending" | "descending" | undefined;

/**
 * The header of a column the server can sort by (FE-59): a button that sorts by it, with an
 * arrow that shows which way. The `th` carries `aria-sort`, so a screen reader hears the order
 * without the arrow (FE-58).
 */
function SortableTableHead({
  sorted,
  onSort,
  children,
  className,
  ...props
}: Omit<React.ComponentProps<"th">, "aria-sort"> & {
  sorted: SortDirection;
  onSort: () => void;
}) {
  const Arrow =
    sorted === "ascending" ? ArrowUp : sorted === "descending" ? ArrowDown : ArrowUpDown;
  return (
    <TableHead aria-sort={sorted} className={cn("px-1", className)} {...props}>
      <button
        type="button"
        onClick={onSort}
        className={cn(
          "inline-flex h-control-sm cursor-pointer items-center gap-1 rounded-control px-2 hover:bg-surface-2 hover:text-ink",
          sorted !== undefined && "text-ink",
        )}
      >
        {children}
        <Arrow aria-hidden="true" className={cn("size-3", sorted === undefined && "opacity-50")} />
      </button>
    </TableHead>
  );
}

export { SortableTableHead, Table, TableBody, TableCell, TableHead, TableHeader, TableRow };
export type { SortDirection };
