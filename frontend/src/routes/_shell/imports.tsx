import { createFileRoute } from "@tanstack/react-router";
import { PlaceholderScreen } from "@/app/PlaceholderScreen";

// PLACEHOLDER: Home's next action links here. Step 2 builds the Imports screen (FE-12) and lists
// it in the sidebar.
export const Route = createFileRoute("/_shell/imports")({
  component: () => <PlaceholderScreen screen="imports" />,
});
