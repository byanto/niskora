import { useEffect, useState } from "react";

type BackendStatus = "checking" | "up" | "down";

const statusLabel: Record<BackendStatus, string> = {
  checking: "Checking…",
  up: "Up",
  down: "Down",
};

/** Shows whether the backend answers on /api/health, through the same origin as the app. */
export function StatusPage() {
  const [status, setStatus] = useState<BackendStatus>("checking");

  useEffect(() => {
    const controller = new AbortController();
    readBackendStatus(controller.signal)
      .then(setStatus)
      .catch(() => {
        if (!controller.signal.aborted) setStatus("down");
      });
    return () => controller.abort();
  }, []);

  return (
    <main className="status">
      <h1>niskora</h1>
      <p>
        Backend: <strong data-status={status}>{statusLabel[status]}</strong>
      </p>
    </main>
  );
}

async function readBackendStatus(signal: AbortSignal): Promise<BackendStatus> {
  const response = await fetch("/api/health", { signal });
  if (!response.ok) return "down";
  const body: unknown = await response.json();
  return isUp(body) ? "up" : "down";
}

function isUp(body: unknown): boolean {
  return typeof body === "object" && body !== null && "status" in body && body.status === "UP";
}
