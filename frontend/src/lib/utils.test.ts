import { describe, expect, it } from "vitest";
import { cn } from "./utils";

describe("cn", () => {
  it("keeps a type size token beside a colour token", () => {
    expect(cn("text-h1", "text-ink")).toBe("text-h1 text-ink");
  });

  it("lets a later type size token win over an earlier one", () => {
    expect(cn("text-sm", "text-h2")).toBe("text-h2");
  });

  it("lets a later radius token win over an earlier one", () => {
    expect(cn("rounded-control", "rounded-card")).toBe("rounded-card");
  });

  it("lets a later colour token win over an earlier one", () => {
    expect(cn("bg-surface", "bg-surface-2")).toBe("bg-surface-2");
  });
});
