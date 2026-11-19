import { Toaster as Sonner, type ToasterProps } from "sonner";
import { StatusIcon } from "./status-badge";

/**
 * Where toasts appear (FE-55): bottom right, drawn with niskora's own tokens rather than Sonner's,
 * light only (FE-99) and with no shadow (FE-98). Each kind has its state's icon, so it reads
 * without colour (FE-58). The app has one, beside every screen; `toast` from `sonner` shows one.
 */
export function Toaster(props: ToasterProps) {
  return (
    <Sonner
      theme="light"
      position="bottom-right"
      icons={{
        error: <ToastIcon status="late" />,
        success: <ToastIcon status="fine" />,
        info: <ToastIcon status="info" />,
        warning: <ToastIcon status="now" />,
      }}
      toastOptions={{
        unstyled: true,
        classNames: {
          toast:
            "flex w-(--width) items-start gap-2 rounded-card border bg-surface p-3 text-sm text-ink",
          error: "border-late-line bg-late-bg text-late",
          success: "border-fine-line bg-fine-bg text-fine",
          warning: "border-now-line bg-now-bg text-now",
          content: "min-w-0 flex-1",
        },
      }}
      {...props}
    />
  );
}

/** A state's icon, centred on the first line of the toast's text. */
function ToastIcon({ status }: { status: React.ComponentProps<typeof StatusIcon>["status"] }) {
  return (
    <span className="flex h-5 items-center">
      <StatusIcon status={status} className="size-4 shrink-0" />
    </span>
  );
}
