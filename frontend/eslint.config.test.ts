// @vitest-environment node
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { ESLint } from "eslint";
import tseslint from "typescript-eslint";
import { afterAll, beforeAll, describe, expect, it } from "vitest";

// The project's own ESLint config, run on a throwaway copy of the folder layout (FE-49), so a rule
// can be shown failing without any such code in the app. The boundary rule only judges an import
// it can resolve, so the imported files exist there. Rules that need type information are left
// out: they need the app's tsconfig, and the rules under test need no types.
const project = mkdtempSync(join(tmpdir(), "niskora-lint-"));
const files = {
  // The `@/` alias as the app's tsconfig.json declares it, so an aliased import resolves too.
  "tsconfig.json": JSON.stringify({ compilerOptions: { paths: { "@/*": ["./src/*"] } } }),
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
    // The boundary patterns and the alias are read from the root of the copy, not from where the
    // tests run.
    {
      settings: {
        "boundaries/root-path": project,
        "import/resolver": {
          typescript: { alwaysTryTypes: true, project: join(project, "tsconfig.json") },
        },
      },
    },
  ],
});

// The first run loads every plugin and the import resolver, which can take seconds on a busy
// machine. It is paid here, under its own limit, so each test times only its own linting.
beforeAll(async () => {
  await eslint.lintText("export {};\n", { filePath: join(project, "src/warm-up.ts") });
}, 60_000);

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

  it("forbids it through the @/ alias too", async () => {
    const code = 'export { supplierQuery } from "@/features/purchasing/queries";\n';

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

describe("design tokens (FE-46, FE-94)", () => {
  it.each([
    ["a hex colour", 'export const look = "bg-[#0f6e66]";\n'],
    ["a colour function", 'export const look = { color: "rgb(15 110 102)" };\n'],
    ["an oklch colour", 'export const look = "oklch(0.5 0.1 180)";\n'],
    ["a step off the 4 px grid", 'export const look = "gap-1.5";\n'],
    ["an arbitrary length", 'export const look = "w-[13px]";\n'],
  ])("forbids %s in a component", async (_, code) => {
    expect(await ruleIdsFor(code, "src/ui/look.ts")).toEqual(["no-restricted-syntax"]);
  });

  it("forbids a raw value in a template literal", async () => {
    const code = "export const look = (on: boolean) => `p-4 ${on ? 'x' : 'y'} size-3.5`;\n";

    expect(await ruleIdsFor(code, "src/ui/look.ts")).toEqual(["no-restricted-syntax"]);
  });

  it("allows tokens, grid steps and arbitrary variants", async () => {
    const code =
      'export const look = "h-control rounded-control border-input-border bg-surface p-4 text-sm text-ink data-[state=open]:bg-surface-2 [&>svg]:size-3 grid-cols-[1fr_auto] w-[50%]";\n';

    expect(await ruleIdsFor(code, "src/ui/look.ts")).toEqual([]);
  });
});

describe("icon and primitive imports", () => {
  it.each([
    ['import { Info } from "lucide-react";\nexport { Info };\n'],
    ['import { Slot } from "radix-ui";\nexport { Slot };\n'],
  ])("forbids the package index: %s", async (code) => {
    expect(await ruleIdsFor(code, "src/ui/look.ts")).toEqual([
      "@typescript-eslint/no-restricted-imports",
    ]);
  });

  it("allows one icon, one primitive and the icon type", async () => {
    const code =
      'import type { LucideIcon } from "lucide-react";\nimport Info from "lucide-react/icons/info";\nimport * as Slot from "radix-ui/slot";\nexport type { LucideIcon };\nexport { Info, Slot };\n';

    expect(await ruleIdsFor(code, "src/ui/look.ts")).toEqual([]);
  });
});
