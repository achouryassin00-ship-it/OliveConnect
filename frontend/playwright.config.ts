import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./e2e",
  use: {
    baseURL: "http://127.0.0.1:5173",
    browserName: "chromium",
    channel: "msedge",
  },
  webServer: [
    {
      command: "npm.cmd run dev -- --port 5173 --strictPort",
      url: "http://127.0.0.1:5173",
      reuseExistingServer: false,
      env: { VITE_API_ENABLED: "false" },
    },
    {
      command: "npm.cmd run dev -- --port 5174 --strictPort",
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
