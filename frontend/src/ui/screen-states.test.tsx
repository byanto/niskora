import { fireEvent, render, screen } from "@testing-library/react";
import Inbox from "lucide-react/icons/inbox";
import { describe, expect, it, vi } from "vitest";
import { ProblemError } from "@/lib/api/problem";
import { expectNoAxeViolations } from "@/test/axe";
import { Button } from "./button";
import { EmptyState } from "./empty-state";
import { LoadFailed } from "./load-failed";
import { Loading, Skeleton } from "./skeleton";

// The states every screen uses (FE-51 to FE-53), shown and checked here as there is no Storybook.

describe("Loading", () => {
  it("shows a skeleton in the shape of the content and says it is loading", async () => {
    const { container } = render(
      <Loading>
        <Skeleton className="h-6 w-48" />
        <Skeleton className="h-row w-full" />
      </Loading>,
    );

    expect(screen.getByRole("status")).toHaveTextContent("Loading…");
    await expectNoAxeViolations(container);
  });
});

describe("EmptyState", () => {
  it("says what is missing and offers the next action", async () => {
    const { container } = render(
      <EmptyState
        icon={Inbox}
        title="No suppliers yet"
        description="Add the first supplier to start buying."
        action={<Button>New supplier</Button>}
      />,
    );

    expect(screen.getByRole("heading", { name: "No suppliers yet" })).toBeInTheDocument();
    expect(screen.getByText("Add the first supplier to start buying.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "New supplier" })).toBeInTheDocument();
    await expectNoAxeViolations(container);
  });
});

describe("LoadFailed", () => {
  it("shows the translated message of a Problem Details code, with a retry", async () => {
    const onRetry = vi.fn();
    const { container } = render(
      <LoadFailed
        error={new ProblemError({ status: 404, code: "NOT_FOUND", detail: "No such supplier." })}
        onRetry={onRetry}
      />,
    );

    expect(screen.getByRole("alert")).toHaveTextContent(
      "This was not found. It may have been removed.",
    );
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));
    expect(onRetry).toHaveBeenCalledOnce();
    await expectNoAxeViolations(container);
  });

  it("shows a general message for a code it has no text for", () => {
    render(
      <LoadFailed
        error={new ProblemError({ status: 500, code: "SOMETHING_NEW" })}
        onRetry={vi.fn()}
      />,
    );

    expect(screen.getByRole("alert")).toHaveTextContent("Something went wrong.");
  });

  it("says the server could not be reached when the request never got an answer", () => {
    render(<LoadFailed error={new TypeError("Failed to fetch")} onRetry={vi.fn()} />);

    expect(screen.getByRole("alert")).toHaveTextContent("The server could not be reached.");
  });
});
