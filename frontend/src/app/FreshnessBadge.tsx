import { useTranslation } from "react-i18next";
import { StatusBadge, type Status } from "@/ui/status-badge";

/**
 * Whether a data set is fresh or stale (FND-83), spelled as an API enum value (FE-57). The imports
 * endpoint will name it in docs/api/openapi.yaml; this type then gives way to the generated one.
 */
export type Freshness = "FRESH" | "STALE";

const statuses: Record<Freshness, Status> = { FRESH: "fine", STALE: "late" };

/** A data set's freshness with its icon and its word (FE-104). */
export function FreshnessBadge({ freshness }: { freshness: Freshness }) {
  const { t } = useTranslation();
  return <StatusBadge status={statuses[freshness]}>{t(`freshness.${freshness}`)}</StatusBadge>;
}
