import { createFileRoute } from "@tanstack/react-router";
import { StatusPage } from "../app/StatusPage";

export const Route = createFileRoute("/")({
  component: StatusPage,
});
