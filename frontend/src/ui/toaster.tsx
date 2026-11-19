import { Toaster as Sonner, toast } from "sonner";
import { StatusIcon } from "./status-badge";

/**
 * Where short success messages appear, away from the action, and disappear by themselves (FE-55).
 * A toast is never for an error. The shell holds the one toaster; a screen calls `toast.success`.
 */
function Toaster() {
  return (
    <Sonner
      position="bottom-right"
      icons={{ success: <StatusIcon status="fine" className="size-4" /> }}
      toastOptions={{
        unstyled: true,
        classNames: {
          // font-sans: the toaster sets a system font of its own on its container.
          toast:
            "flex w-full items-center font-sans gap-2 rounded-card border border-fine-line bg-fine-bg p-3 text-sm text-fine",
        },
      }}
    />
  );
}

export { toast, Toaster };
