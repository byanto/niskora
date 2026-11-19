import { useQuery } from "@tanstack/react-query";
import {
  createColumnHelper,
  rowSortingFeature,
  type SortingState,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import ChevronLeft from "lucide-react/icons/chevron-left";
import ChevronRight from "lucide-react/icons/chevron-right";
import SearchX from "lucide-react/icons/search-x";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { formatMoment, formatNumber } from "@/lib/format";
import { Button } from "@/ui/button";
import { EmptyState } from "@/ui/empty-state";
import { LoadFailed } from "@/ui/load-failed";
import { Loading, Skeleton } from "@/ui/skeleton";
import { SortableTableHead, Table, TableBody, TableCell, TableHeader, TableRow } from "@/ui/table";
import { PageHeader } from "@/app/PageHeader";
import { AddUserDialog } from "./AddUserDialog";
import {
  defaultUserSort,
  type User,
  type UserSortField,
  type UsersSearch,
  usersQuery,
} from "./users";

const features = tableFeatures({ rowSortingFeature });
const column = createColumnHelper<typeof features, User>();

/**
 * Users (FE-37): every account as a paged list the server sorts (FE-59), with "Add user". The
 * page and the sort live in the URL (FE-41), which the route owns.
 */
export function UsersScreen({
  search,
  onSearchChange,
}: {
  search: UsersSearch;
  onSearchChange: (search: UsersSearch) => void;
}) {
  const { t } = useTranslation("identity");
  const users = useQuery(usersQuery(search));

  return (
    <>
      <PageHeader title={t("users.title")} actions={<AddUserDialog />} />
      {users.isPending ? (
        <UsersLoading />
      ) : users.isError ? (
        <LoadFailed
          error={users.error}
          onRetry={() => {
            void users.refetch();
          }}
        />
      ) : users.data.data.length === 0 ? (
        <EmptyState
          icon={SearchX}
          title={t("users.emptyPage.title")}
          description={t("users.emptyPage.description")}
          action={
            <Button
              variant="outline"
              onClick={() => {
                onSearchChange({ ...search, page: 1 });
              }}
            >
              {t("users.emptyPage.action")}
            </Button>
          }
        />
      ) : (
        <div className="flex flex-col gap-4">
          <UsersTable
            users={users.data.data}
            sort={search.sort}
            onSortChange={(sort) => {
              // A new order starts again from its first page.
              onSearchChange({ ...search, page: 1, sort });
            }}
          />
          <Pages
            {...users.data.pagination}
            onPageChange={(page) => {
              onSearchChange({ ...search, page });
            }}
          />
        </div>
      )}
    </>
  );
}

function UsersTable({
  users,
  sort,
  onSortChange,
}: {
  users: User[];
  sort: UsersSearch["sort"];
  onSortChange: (sort: UsersSearch["sort"]) => void;
}) {
  const { t } = useTranslation("identity");
  const columns = useMemo(
    () =>
      column.columns([
        column.accessor("fullName", {
          header: t("users.columns.fullName"),
          cell: (info) => <span className="font-medium">{info.getValue()}</span>,
        }),
        column.accessor("loginName", { header: t("users.columns.loginName") }),
        column.accessor("createdAt", {
          header: t("users.columns.createdAt"),
          cell: (info) => <span className="tabular-nums">{formatMoment(info.getValue())}</span>,
        }),
      ]),
    [t],
  );
  const sorting = useMemo(() => sortingOf(sort), [sort]);
  const table = useTable({
    features,
    columns,
    data: users,
    getRowId: (user) => user.id,
    // The server sorts (FE-59): the table only says which column, and which way.
    manualSorting: true,
    enableMultiSort: false,
    enableSortingRemoval: false,
    state: { sorting },
    onSortingChange: (updater) => {
      const next = typeof updater === "function" ? updater(sorting) : updater;
      onSortChange(sortOf(next));
    },
  });

  return (
    // Fixed columns, so they keep their widths from page to page; below 640 px the table scrolls
    // sideways inside its box (FE-108).
    <Table className="min-w-160 table-fixed">
      <TableHeader>
        {table.getHeaderGroups().map((group) => (
          <TableRow key={group.id} className="h-row-header hover:bg-transparent">
            {group.headers.map((header) => {
              const sorted = header.column.getIsSorted();
              return (
                <SortableTableHead
                  key={header.id}
                  sorted={
                    sorted === "asc" ? "ascending" : sorted === "desc" ? "descending" : undefined
                  }
                  onSort={() => {
                    header.column.toggleSorting();
                  }}
                >
                  <table.FlexRender header={header} />
                </SortableTableHead>
              );
            })}
          </TableRow>
        ))}
      </TableHeader>
      <TableBody>
        {table.getRowModel().rows.map((row) => (
          <TableRow key={row.id}>
            {row.getAllCells().map((cell) => (
              <TableCell key={cell.id}>
                <table.FlexRender cell={cell} />
              </TableCell>
            ))}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

/** The URL's sort as the table's state: the default column when the URL names none. */
function sortingOf(sort: UsersSearch["sort"]): SortingState {
  const field = sort ?? defaultUserSort;
  return field.startsWith("-")
    ? [{ id: field.slice(1), desc: true }]
    : [{ id: field, desc: false }];
}

/** The table's sorting as the URL's sort, left out when it is the default. */
function sortOf(sorting: SortingState): UsersSearch["sort"] {
  const first = sorting[0];
  if (first === undefined) {
    return undefined;
  }
  const field = first.id as UserSortField;
  if (!first.desc && field === defaultUserSort) {
    return undefined;
  }
  return first.desc ? `-${field}` : field;
}

/** How long the list is, and the way to its other pages when it has more than one (FE-59). */
function Pages({
  page,
  totalItems,
  totalPages,
  onPageChange,
}: {
  page: number;
  totalItems: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}) {
  const { t } = useTranslation("identity");
  return (
    <div className="flex flex-wrap items-center justify-between gap-4 text-sm text-muted">
      <p>{t("users.count", { count: totalItems, formattedCount: formatNumber(totalItems) })}</p>
      {totalPages > 1 ? (
        <nav aria-label={t("users.title")} className="flex items-center gap-2">
          <p className="tabular-nums">{t("users.page", { page, totalPages })}</p>
          <Button
            variant="outline"
            size="icon-sm"
            aria-label={t("users.previousPage")}
            disabled={page <= 1}
            onClick={() => {
              onPageChange(page - 1);
            }}
          >
            <ChevronLeft aria-hidden="true" />
          </Button>
          <Button
            variant="outline"
            size="icon-sm"
            aria-label={t("users.nextPage")}
            disabled={page >= totalPages}
            onClick={() => {
              onPageChange(page + 1);
            }}
          >
            <ChevronRight aria-hidden="true" />
          </Button>
        </nav>
      ) : null}
    </div>
  );
}

/** The list while it loads: a skeleton in the shape of the table (FE-51). */
function UsersLoading() {
  return (
    <Loading>
      <div className="flex flex-col gap-1 rounded-card border bg-surface p-cell">
        <Skeleton className="h-6 w-1/3" />
        {[0, 1, 2].map((row) => (
          <Skeleton key={row} className="mt-4 h-6 w-full" />
        ))}
      </div>
    </Loading>
  );
}
