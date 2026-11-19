import Construction from "lucide-react/icons/construction";
import { useTranslation } from "react-i18next";
import type { common } from "@/lib/i18n/en/common";
import { EmptyState } from "@/ui/empty-state";
import { PageHeader } from "@/ui/page-header";

/**
 * PLACEHOLDER for a screen whose build step has not landed, so the shell can already link to it.
 * The ticket that builds the screen replaces its route's component with the real one, and this
 * goes once no route uses it. Go-live comes after them, so no user meets one.
 */
export function PlaceholderScreen({ screen }: { screen: keyof typeof common.nav.screens }) {
  const { t } = useTranslation();
  return (
    <>
      <PageHeader title={t(`nav.screens.${screen}`)} />
      <EmptyState
        icon={Construction}
        title={t("placeholder.title")}
        description={t("placeholder.description")}
      />
    </>
  );
}
