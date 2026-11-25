import { afterEach, expect, it, vi } from "vitest";
import { loadOwnedVergers } from "./vergers";
import { harvestsApi } from "./harvests";
afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});
it("loads all pages and excludes inactive and other-owner orchards, including mixed-role responses", async () => {
  vi.stubEnv("VITE_API_ENABLED", "true");
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          page: 0,
          totalPages: 2,
          items: [
            { id: "a", actif: true, proprietaireId: "u" },
            { id: "b", actif: true, proprietaireId: "other" },
          ],
        }),
      ),
    )
    .mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          page: 1,
          totalPages: 2,
          items: [
            { id: "c", actif: false, proprietaireId: "u" },
            { id: "d", actif: true, proprietaireId: "u" },
          ],
        }),
      ),
    );
  vi.stubGlobal("fetch", fetch);
  expect((await loadOwnedVergers("test-token", "u")).map((v) => v.id)).toEqual([
    "a",
    "d",
  ]);
  expect(fetch).toHaveBeenCalledTimes(2);
  expect(fetch.mock.calls[0][1].headers.get("Authorization")).toBe(
    "Bearer test-token",
  );
});
it("never calls unimplemented harvest endpoints", async () => {
  const fetch = vi.fn();
  vi.stubGlobal("fetch", fetch);
  await expect(harvestsApi.list()).rejects.toMatchObject({
    code: "HARVEST_API_UNAVAILABLE",
  });
  await expect(harvestsApi.get("id")).rejects.toMatchObject({
    code: "HARVEST_API_UNAVAILABLE",
  });
  expect(fetch).not.toHaveBeenCalled();
});
