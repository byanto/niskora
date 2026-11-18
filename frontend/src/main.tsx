import { RouterProvider } from "@tanstack/react-router";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { createAppRouter } from "./app/router";
import "./lib/i18n";
import "./index.css";

const root = document.getElementById("root");
if (root === null) {
  throw new Error("The page has no #root element.");
}

const router = createAppRouter();

createRoot(root).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
);
