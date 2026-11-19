import { createFileRoute, stripSearchParams } from "@tanstack/react-router";
import { UsersScreen, usersSearch } from "@/features/identity";

/** Users (FE-37), with its page and sort in the URL (FE-41). */
export const Route = createFileRoute("/_shell/users")({
  validateSearch: usersSearch,
  // The first page is the default, so a link to it reads /users.
  search: { middlewares: [stripSearchParams({ page: 1 })] },
  component: function Users() {
    const search = Route.useSearch();
    const navigate = Route.useNavigate();
    return (
      <UsersScreen
        search={search}
        onSearchChange={(next) => {
          void navigate({ search: next });
        }}
      />
    );
  },
});
