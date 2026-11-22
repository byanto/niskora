import { useTranslation } from "react-i18next";
import type { VersionConflict as Conflict } from "@/lib/api/problem";
import { formatMoment } from "@/lib/format";
import { Button } from "./button";
import { StatusIcon } from "./status-badge";

/**
 * A change refused because someone else changed the thing first (FE-81), as a warning next to the
 * action, just above its buttons: who changed it and when, and the one button that loads the
 * latest version. What the user typed stays in the form, and nothing is merged or sent again by
 * itself.
 */
export function VersionConflict({
  conflict,
  onLoadLatest,
  loading,
}: {
  conflict: Conflict;
  onLoadLatest: () => void;
  /** Whether the latest version is loading. */
  loading: boolean;
}) {
  const { t } = useTranslation();
  const moment = formatMoment(conflict.changedAt);
  return (
    <div
      role="alert"
      className="flex flex-col items-start gap-3 rounded-control border border-now-line bg-now-bg p-3 text-sm text-now"
    >
      <p className="flex items-start gap-2">
        <span className="flex h-5 shrink-0 items-center">
          <StatusIcon status="now" className="size-4" />
        </span>
        {conflict.changedBy === null
          ? t("conflict.changed", { moment })
          : t("conflict.changedBy", { fullName: conflict.changedBy.fullName, moment })}
      </p>
      <Button type="button" variant="outline" size="sm" disabled={loading} onClick={onLoadLatest}>
        {loading ? t("conflict.loading") : t("conflict.loadLatest")}
      </Button>
    </div>
  );
}
