import { queryOptions } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { dataOf } from "@/lib/api/problem";

/**
 * PLACEHOLDER until step 3 gives Home its own endpoint (FE-9). Until then Home asks /api/health,
 * so it already loads, fails and retries through the shared states (FE-51, FE-53), and its answer
 * carries the build id the shell compares (FE-82). Nothing is imported before step 2, so an
 * answer always means Home is empty. Step 3 replaces the query function and the answer's shape.
 */
export const homeQuery = queryOptions({
  queryKey: ["home"],
  queryFn: async ({ signal }) => {
    await dataOf(api.GET("/api/health", { signal }));
    return { imported: false };
  },
});
