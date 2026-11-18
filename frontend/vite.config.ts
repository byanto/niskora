import { fileURLToPath } from "node:url";
import tailwindcss from "@tailwindcss/vite";
import { tanstackRouter } from "@tanstack/router-plugin/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

// The dev server forwards /api to the backend started by `docker compose up`, so the browser
// sees one origin as it does behind Caddy (FE-83). The health path is routed like the Caddyfile
// routes it: to Actuator on the internal management port. It is listed first so it wins.
export default defineConfig({
  // The router plugin reads tsr.config.json, the same file `pnpm routes:generate` reads, and adds
  // code splitting, which only a build does. It must come before react().
  plugins: [tanstackRouter({ autoCodeSplitting: true }), react(), tailwindcss()],
  // `@/` is `src/`, as in tsconfig.json, so components added by the shadcn CLI resolve.
  resolve: { alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) } },
  server: {
    proxy: {
      "/api/health": {
        target: "http://localhost:8081",
        rewrite: () => "/actuator/health",
      },
      "/api": "http://localhost:8080",
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    include: ["src/**/*.test.{ts,tsx}", "*.test.ts"],
    restoreMocks: true,
  },
});
