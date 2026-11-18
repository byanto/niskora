import js from "@eslint/js";
import pluginQuery from "@tanstack/eslint-plugin-query";
import pluginRouter from "@tanstack/eslint-plugin-router";
import prettier from "eslint-config-prettier";
import boundaries from "eslint-plugin-boundaries";
import i18next from "eslint-plugin-i18next";
import reactHooks from "eslint-plugin-react-hooks";
import { defineConfig, globalIgnores } from "eslint/config";
import globals from "globals";
import tseslint from "typescript-eslint";

export default defineConfig([
  // Generated files are checked for drift by CI, not linted.
  globalIgnores(["dist", "src/lib/api/schema.d.ts", "src/routeTree.gen.ts"]),
  {
    files: ["**/*.{ts,tsx,js}"],
    extends: [
      js.configs.recommended,
      tseslint.configs.strictTypeChecked,
      tseslint.configs.stylisticTypeChecked,
      reactHooks.configs.flat["recommended-latest"],
      pluginQuery.configs["flat/recommended"],
      pluginRouter.configs["flat/recommended"],
    ],
    languageOptions: {
      ecmaVersion: 2023,
      globals: globals.browser,
      parserOptions: { projectService: true, tsconfigRootDir: import.meta.dirname },
    },
  },
  {
    // The tool config at the root runs in Node.
    files: ["*.{js,ts}"],
    languageOptions: { globals: globals.node },
  },
  {
    // This file is plain JavaScript in no tsconfig, so rules that need types cannot read it.
    files: ["eslint.config.js"],
    extends: [tseslint.configs.disableTypeChecked],
  },
  {
    // One feature may use another only through its index.ts (FE-50). Imports inside a feature,
    // and imports by app/, routes/, ui/ and lib/, are not limited by this rule.
    files: ["src/**/*.{ts,tsx}"],
    plugins: { boundaries },
    settings: {
      "import/resolver": { typescript: { alwaysTryTypes: true } },
      "boundaries/elements": [
        { type: "feature", pattern: "src/features/*", capture: ["name"], partialMatch: false },
      ],
    },
    rules: {
      "boundaries/dependencies": [
        "error",
        {
          default: "allow",
          policies: [
            {
              from: { element: { type: "feature" } },
              disallow: { to: { element: { type: "feature", fileInternalPath: "!index.ts" } } },
              message:
                "A feature uses another feature only through its index.ts, never its internals (FE-50).",
            },
          ],
        },
      ],
    },
  },
  {
    // No user-facing text is written straight into a component (FE-64).
    files: ["src/**/*.tsx"],
    ignores: ["src/**/*.test.tsx"],
    extends: [i18next.configs["flat/recommended"]],
  },
  // Last, so formatting is left to Prettier alone.
  prettier,
]);
