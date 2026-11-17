import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// The dev server forwards /api to the backend started by `docker compose up`, so the browser
// sees one origin as it does behind Caddy (FE-83). The health path is routed like the Caddyfile
// routes it: to Actuator on the internal management port. It is listed first so it wins.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api/health": {
        target: "http://localhost:8081",
        rewrite: () => "/actuator/health",
      },
      "/api": "http://localhost:8080",
    },
  },
});
