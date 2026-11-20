import { useQuery } from "@tanstack/react-query";
import { Link } from "@tanstack/react-router";
import Inbox from "lucide-react/icons/inbox";
import { useTranslation } from "react-i18next";
import { Button } from "@/ui/button";
import { EmptyState } from "@/ui/empty-state";
import { LoadFailed } from "@/ui/load-failed";
import { Loading, Skeleton } from "@/ui/skeleton";
import { homeQuery } from "./home";
import { PageHeader } from "@/ui/page-header";

/**
 * Home, the overview of FE-9. Until the first import it shows only its empty state, with the
 * next action: importing the BigSeller exports (FE-52).
 */
export function HomeScreen() {
  const { t } = useTranslation();
  const home = useQuery(homeQuery);

  return (
    <>
      <PageHeader title={t("home.title")} />
      {home.isPending ? (
        <Loading>
          <Skeleton className="h-48 w-full rounded-card" />
        </Loading>
      ) : home.isError ? (
        <LoadFailed
          error={home.error}
          onRetry={() => {
            void home.refetch();
          }}
        />
      ) : (
        <EmptyState
          icon={Inbox}
          title={t("home.empty.title")}
          description={t("home.empty.description")}
          action={
            <Button asChild>
              <Link to="/imports">{t("home.empty.action")}</Link>
            </Button>
          }
        />
      )}
    </>
  );
}
