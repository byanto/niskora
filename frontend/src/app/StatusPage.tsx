import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Card } from "@/ui/card";
import { StatusBadge, type Status } from "@/ui/status-badge";
import { healthQuery, type BackendStatus } from "./health";

const statuses: Record<"checking" | BackendStatus, Status> = {
  checking: "info",
  up: "fine",
  down: "late",
};

/** Shows whether the backend answers on /api/health, through the same origin as the app. */
export function StatusPage() {
  const { t } = useTranslation();
  const { data, isPending } = useQuery(healthQuery);
  const status = isPending ? "checking" : (data ?? "down");

  return (
    <main className="mx-auto my-16 max-w-xl px-4">
      <h1 className="mb-4 text-h1 font-semibold">{t("appName")}</h1>
      <Card className="flex-row items-center justify-between">
        <p className="text-muted">{t("status.backend")}</p>
        <StatusBadge status={statuses[status]}>{t(`status.${status}`)}</StatusBadge>
      </Card>
    </main>
  );
}
