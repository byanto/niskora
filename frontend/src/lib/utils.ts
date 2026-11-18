import { clsx, type ClassValue } from "clsx";
import { extendTailwindMerge } from "tailwind-merge";

// tailwind-merge only knows Tailwind's default names. Without this it would read `text-h1` as a
// colour and drop it next to `text-ink`, or keep two radii side by side.
const twMerge = extendTailwindMerge({
  extend: {
    theme: {
      text: ["h1", "h2"],
      radius: ["control", "card"],
    },
  },
});

/** Joins class names, letting a later Tailwind class win over an earlier one of the same kind. */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
