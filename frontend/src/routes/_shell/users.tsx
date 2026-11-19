import { createFileRoute } from "@tanstack/react-router";
import { PlaceholderScreen } from "@/app/PlaceholderScreen";

// PLACEHOLDER: the sidebar lists Users in step 1. #49 builds the Users screen (FE-37).
export const Route = createFileRoute("/_shell/users")({
  component: () => <PlaceholderScreen screen="users" />,
});
