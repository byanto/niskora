import { clsx, type ClassValue } from "clsx";
import { extendTailwindMerge } from "tailwind-merge";

// tailwind-merge only knows Tailwind's default names. Without this it would read `text-h1` as a
// colour and drop it next to `text-ink`, or keep two radii or paddings side by side.
const twMerge = extendTailwindMerge({
  extend: {
    theme: {
      text: ["h1", "h2"],
      radius: ["control", "card"],
      // The named sizes of index.css (FE-95 to FE-97), beside the numbered 4 px steps.
      spacing: [
        "control",
        "control-sm",
        "row",
        "row-header",
        "cell",
        "card",
        "photo-sku",
        "photo-spu",
        "photo-hero",
        "sidebar",
      ],
    },
  },
});

/** Joins class names, letting a later Tailwind class win over an earlier one of the same kind. */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
