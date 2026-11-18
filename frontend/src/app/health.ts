import { queryOptions } from "@tanstack/react-query";
import { api } from "@/lib/api/client";

/** Up only when the backend answers that it is; anything else, a failed request too, is down. */
export type BackendStatus = "up" | "down";

/** Whether the backend answers on /api/health (ENG-68). */
export const healthQuery = queryOptions({
  queryKey: ["health"],
  queryFn: async ({ signal }): Promise<BackendStatus> => {
    const { data } = await api.GET("/api/health", { signal });
    return data?.status === "UP" ? "up" : "down";
  },
  // The page answers at once; a down backend is the answer, not a reason to try again.
  retry: false,
});
