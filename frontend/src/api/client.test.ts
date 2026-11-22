import { afterEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "./client";
afterEach(() => {
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
});
describe("API integration boundary", () => {
  it("never sends a request when the API is disabled", async () => {
    vi.stubEnv("VITE_API_ENABLED", "false");
    const fetch = vi.fn();
    vi.stubGlobal("fetch", fetch);
    await expect(apiRequest("/vergers")).rejects.toMatchObject({
      code: "API_DISABLED",
    });
    expect(fetch).not.toHaveBeenCalled();
  });
  it("uses the configured base and returns JSON", async () => {
    vi.stubEnv("VITE_API_ENABLED", "true");
    vi.stubEnv("VITE_API_BASE_URL", "/api/");
    const fetch = vi
      .fn()
      .mockResolvedValue(new Response(JSON.stringify({ items: [] })));
    vi.stubGlobal("fetch", fetch);
    expect(await apiRequest("/vergers")).toEqual({ items: [] });
    expect(fetch.mock.calls[0][0]).toBe("/api/vergers");
  });
  it("maps a conflict without exposing the server response", async () => {
    vi.stubEnv("VITE_API_ENABLED", "true");
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          new Response("private stack trace", { status: 409 }),
        ),
    );
    await expect(apiRequest("/tournees")).rejects.toMatchObject({
      status: 409,
      message: "Les données ont changé. Rechargez avant de réessayer.",
    });
  });
  it("supports responses without content", async () => {
    vi.stubEnv("VITE_API_ENABLED", "true");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response(null, { status: 204 })),
    );
    expect(await apiRequest("/resource")).toBeUndefined();
  });
  it("reports network failures", async () => {
    vi.stubEnv("VITE_API_ENABLED", "true");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockRejectedValue(new TypeError("Failed to fetch")),
    );
    await expect(apiRequest("/vergers")).rejects.toMatchObject({
      code: "NETWORK_ERROR",
    });
  });
});
