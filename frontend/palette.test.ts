// @vitest-environment node
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

// The palette as the theme declares it, read from the CSS so a changed token is checked as it is.
// Read from disk: Vitest hands a test an empty module for any CSS import, `?raw` included.
const css = readFileSync(join(import.meta.dirname, "src/index.css"), "utf8");
const palette = new Map(
  [...css.matchAll(/--color-([a-z0-9-]+):\s*(#[0-9a-f]{6});/gi)].map(([, name, hex]) => [
    name ?? "",
    hex ?? "",
  ]),
);

function colour(token: string) {
  const hex = palette.get(token);
  if (hex === undefined) {
    throw new Error(`The theme has no colour token "${token}".`);
  }
  return hex;
}

/** WCAG 2.1 relative luminance of an sRGB colour such as `#0f6e66`. */
function luminance(hex: string) {
  const [r = 0, g = 0, b = 0] = [1, 3, 5].map((start) => {
    const channel = parseInt(hex.slice(start, start + 2), 16) / 255;
    return channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contrast(foreground: string, background: string) {
  const [light, dark] = [luminance(colour(foreground)), luminance(colour(background))].sort(
    (a, b) => b - a,
  );
  return ((light ?? 0) + 0.05) / ((dark ?? 0) + 0.05);
}

describe("palette (FE-99)", () => {
  // Every pair of text and background the design uses (FE-100).
  const textPairs = [
    ...["ink", "muted", "link"].flatMap((text) =>
      ["bg", "surface", "surface-2"].map((background) => [text, background] as const),
    ),
    ["primary-ink", "primary"],
    ["nav-active-ink", "nav-active"],
    ...["late", "now", "fine"].flatMap((tone) => [
      [tone, `${tone}-bg`] as const,
      [tone, "surface"] as const,
    ]),
  ] as const;

  it.each(textPairs)("%s text on %s reaches 4.5:1", (text, background) => {
    expect(contrast(text, background)).toBeGreaterThanOrEqual(4.5);
  });

  // A control's edge and the focus outline must stand out from what they sit on (FE-101, FE-102).
  const edgePairs = ["input-border", "primary"].flatMap((edge) =>
    ["bg", "surface", "surface-2"].map((background) => [edge, background] as const),
  );

  it.each(edgePairs)("%s edge on %s reaches 3:1", (edge, background) => {
    expect(contrast(edge, background)).toBeGreaterThanOrEqual(3);
  });

  it("holds the palette of FE-99 and nothing else", () => {
    expect([...palette.keys()].sort()).toEqual(
      [
        "bg",
        "surface",
        "surface-2",
        "border",
        "input-border",
        "ink",
        "muted",
        "primary",
        "primary-ink",
        "link",
        "nav-active",
        "nav-active-ink",
        "late",
        "late-bg",
        "late-line",
        "now",
        "now-bg",
        "now-line",
        "fine",
        "fine-bg",
        "fine-line",
      ].sort(),
    );
  });
});
