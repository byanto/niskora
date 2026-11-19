import Inbox from "lucide-react/icons/inbox";
import { useTranslation } from "react-i18next";
import { EmptyState } from "@/ui/empty-state";
import { PageHeader } from "./PageHeader";

/**
 * Home, the overview of FE-9. Nothing is imported in step 1, so it shows only its empty state; its
 * figures and the loading and failed-load states arrive with its query in step 3. The next action,
 * the Imports screen, gets its link once that screen exists in step 2.
 */
export function HomeScreen() {
  const { t } = useTranslation();
  return (
    <>
      <PageHeader title={t("home.title")} />
      <EmptyState
        icon={Inbox}
        title={t("home.empty.title")}
        description={t("home.empty.description")}
      />
    </>
  );
}
