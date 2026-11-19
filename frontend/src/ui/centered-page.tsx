import type * as React from "react";

/** A page with no shell and one thing in its middle: Login, or the app failing to start. */
export function CenteredPage({ children }: { children: React.ReactNode }) {
  return <main className="flex min-h-screen items-center justify-center p-4">{children}</main>;
}
