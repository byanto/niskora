import type * as React from "react";

/**
 * The header of a screen: its title, and the screen's actions on the right. On a narrow window the
 * actions wrap under the title (FE-107).
 */
export function PageHeader({ title, actions }: { title: string; actions?: React.ReactNode }) {
  return (
    <header className="mb-6 flex flex-wrap items-center justify-between gap-4">
      <h1 className="text-h1 font-semibold">{title}</h1>
      {actions === undefined ? null : <div className="flex flex-wrap gap-2">{actions}</div>}
    </header>
  );
}
