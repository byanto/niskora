import { useTranslation } from "react-i18next";
import { StatusBadge, type Status } from "@/ui/status-badge";

/**
 * The three levels of restock advice (RST-35), spelled as an API enum value (FE-57). The restock
 * endpoint will name them in docs/api/openapi.yaml; this type then gives way to the generated one.
 */
export type RestockLevel = "LATE" | "ORDER_NOW" | "FINE";

const statuses: Record<RestockLevel, Status> = { LATE: "late", ORDER_NOW: "now", FINE: "fine" };

/** A restock level with its icon and its word (FE-103). */
export function RestockLevelBadge({ level }: { level: RestockLevel }) {
  const { t } = useTranslation("restock");
  return <StatusBadge status={statuses[level]}>{t(`level.${level}`)}</StatusBadge>;
}
