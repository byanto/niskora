import { useId } from "react";
import type { FieldError, UseFormRegisterReturn } from "react-hook-form";
import { cn } from "@/lib/utils";
import { Input } from "./input";
import { Label } from "./label";

/**
 * A form field: its label above its input, then its error, then a hint. The error and the hint
 * describe the input, so a screen reader reads both (FE-55). The caller translates the text.
 */
export function Field({
  label,
  error,
  hint,
  input,
  className,
  ...props
}: {
  label: string;
  /** What is wrong with the value, from the form's own check or from the server (FE-56). */
  error: FieldError | undefined;
  /** What the value should be, shown under the input. */
  hint?: string;
  input: UseFormRegisterReturn;
  className?: string;
} & Pick<React.ComponentProps<"input">, "type" | "autoComplete" | "autoFocus" | "spellCheck">) {
  const id = useId();
  const errorId = `${id}-error`;
  const hintId = `${id}-hint`;
  const describedBy = [error === undefined ? null : errorId, hint === undefined ? null : hintId]
    .filter((part) => part !== null)
    .join(" ");
  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <Label htmlFor={id}>{label}</Label>
      <Input
        id={id}
        aria-invalid={error === undefined ? undefined : true}
        aria-describedby={describedBy === "" ? undefined : describedBy}
        {...input}
        {...props}
      />
      {error === undefined ? null : (
        <p id={errorId} className="text-xs text-late">
          {error.message}
        </p>
      )}
      {hint === undefined ? null : (
        <p id={hintId} className="text-xs text-muted">
          {hint}
        </p>
      )}
    </div>
  );
}
