import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { healthQuery } from "./health";

/** Shows whether the backend answers on /api/health, through the same origin as the app. */
export function StatusPage() {
  const { t } = useTranslation();
  const { data, isPending } = useQuery(healthQuery);
  const status = isPending ? "checking" : (data ?? "down");

  return (
    <main className="status">
      <h1>{t("appName")}</h1>
      <p>
        {t("status.backend")} <strong data-status={status}>{t(`status.${status}`)}</strong>
      </p>
    </main>
  );
}
