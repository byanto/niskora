import { createFileRoute } from "@tanstack/react-router";
import { Shell } from "@/app/Shell";

/** Every screen except Login sits in the shell (FE-1). Login is a route outside it. */
export const Route = createFileRoute("/_shell")({
  component: Shell,
});
