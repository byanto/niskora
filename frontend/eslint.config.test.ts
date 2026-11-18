// @vitest-environment node
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { ESLint } from "eslint";
import tseslint from "typescript-eslint";
import { afterAll, describe, expect, it } from "vitest";

// The project's own ESLint config, run on a throwaway copy of the folder layout (FE-49), so a rule
// can be shown failing without any such code in the app. The boundary rule only judges an import
// it can resolve, so the imported files exist there. Rules that need type information are left
// out: they need the app's tsconfig, and the rules under test need no types.
const project = mkdtempSync(join(tmpdir(), "niskora-lint-"));
const files = {
  "src/features/purchasing/index.ts": 'export { supplierQuery } from "./queries";\n',
  "src/features/purchasing/queries.ts": "export const supplierQuery = 1;\n",
  "src/features/restock/queries.ts": "export const restockQuery = 1;\n",
  "src/features/restock/RestockScreen.ts": "export const RestockScreen = 1;\n",
};
for (const [path, code] of Object.entries(files)) {
  mkdirSync(dirname(join(project, path)), { recursive: true });
  writeFileSync(join(project, path), code);
}
afterAll(() => {
  rmSync(project, { recursive: true, force: true });
});

const eslint = new ESLint({
  cwd: project,
  overrideConfigFile: resolve("eslint.config.js"),
  overrideConfig: [
    tseslint.configs.disableTypeChecked,
    // The boundary patterns are read from the root of the copy, not from where the tests run.
    { settings: { "boundaries/root-path": project } },
  ],
});

async function ruleIdsFor(code: string, filePath: string) {
  const [result] = await eslint.lintText(code, { filePath: join(project, filePath) });
  return result?.messages.map((message) => message.ruleId ?? message.message) ?? [];
}

describe("feature boundaries (FE-50)", () => {
  it("forbids a feature from importing another feature's internals", async () => {
    const code = 'export { supplierQuery } from "../purchasing/queries";\n';

    expect(await ruleIdsFor(code, "src/features/restock/RestockScreen.ts")).toEqual([
      "boundaries/dependencies",
    ]);
  });

  it("allows a feature to import another feature's public index", async () => {
    const code = 'export { supplierQuery } from "../purchasing";\n';

    expect(await ruleIdsFor(code, "src/features/restock/RestockScreen.ts")).toEqual([]);
  });

  it("allows a feature to import its own files", async () => {
    const code = 'export { restockQuery } from "./queries";\n';

    expect(await ruleIdsFor(code, "src/features/restock/RestockScreen.ts")).toEqual([]);
  });

  it("allows a route to import a feature's screen", async () => {
    const code = 'export { RestockScreen } from "../features/restock/RestockScreen";\n';

    expect(await ruleIdsFor(code, "src/routes/restock.ts")).toEqual([]);
  });
});

describe("user-facing text (FE-64)", () => {
  it("forbids text written straight into a component", async () => {
    const code = "export function Title() {\n  return <h1>Restock advice</h1>;\n}\n";

    expect(await ruleIdsFor(code, "src/features/restock/Title.tsx")).toEqual([
      "i18next/no-literal-string",
    ]);
  });
});
