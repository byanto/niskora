import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { StatusPage } from "./StatusPage";
import "./index.css";

const root = document.getElementById("root");
if (root === null) {
  throw new Error("The page has no #root element.");
}

createRoot(root).render(
  <StrictMode>
    <StatusPage />
  </StrictMode>,
);
