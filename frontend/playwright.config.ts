import { defineConfig } from "@playwright/test";

const npmCmd = process.platform === "win32" ? "npm.cmd" : "npm";

export default defineConfig({
  testDir: "./e2e",
  use: {
    baseURL: "http://127.0.0.1:5173",
    browserName: "chromium",
    ...(process.platform === "win32" ? { channel: "msedge" as const } : {}),
  },
  webServer: [
    {
      command: `${npmCmd} run dev -- --port 5173 --strictPort`,
      url: "http://127.0.0.1:5173",
      reuseExistingServer: false,
      env: { VITE_API_ENABLED: "false" },
    },
    {
      command: `${npmCmd} run dev -- --port 5174 --strictPort`,
      url: "http://127.0.0.1:5174",
      reuseExistingServer: false,
      env: {
        VITE_API_ENABLED: "true",
        VITE_API_BASE_URL: "/api",
        API_PROXY_TARGET: "http://127.0.0.1:8085",
      },
    },
  ],
  reporter: "list",
});
