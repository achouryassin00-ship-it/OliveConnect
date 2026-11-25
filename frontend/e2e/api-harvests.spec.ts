import { test, expect, type Page } from "@playwright/test";
test.use({ baseURL: "http://127.0.0.1:5174" });
const user = {
  id: "farmer",
  name: "Agriculteur Test",
  email: "farmer@example.test",
  roles: ["AGRICULTEUR"],
  active: true,
};
const orchard = {
  id: "own",
  nom: "Mon verger",
  code: "OWN",
  proprietaireId: user.id,
  actif: true,
};
async function mockSession(page: Page, roles = ["AGRICULTEUR"]) {
  await page.route("**/api/auth/login", (route) =>
    route.fulfill({
      json: { token: "test-only-token", user: { ...user, roles } },
    }),
  );
  await page.route("**/api/auth/me", (route) =>
    route.fulfill({ json: { ...user, roles } }),
  );
  await page.route("**/api/vergers?*", (route) =>
    route.fulfill({
      json: {
        items: [
          orchard,
          { ...orchard, id: "other", proprietaireId: "someone-else" },
          { ...orchard, id: "inactive", actif: false },
        ],
        page: 0,
        totalPages: 1,
        totalElements: 3,
        size: 100,
      },
    }),
  );
  await page.goto("/collectes");
  await page.getByLabel("Adresse e-mail").fill(user.email);
  await page.getByLabel("Mot de passe").fill("test-only-password");
  await page.getByRole("button", { name: "Se connecter" }).click();
}
async function fillDraft(page: Page) {
  await page.getByRole("link", { name: "Nouvelle demande" }).click();
  await page.getByLabel("Votre verger").selectOption("own");
  await page.getByLabel("Date souhaitée").fill("2099-11-28");
  await page.getByLabel("Quantité estimée").fill("1234");
}
test("real mode uses Bearer, filters owners, checks a draft without harvest POST", async ({
  page,
}) => {
  const requests: string[] = [];
  page.on("request", (r) => requests.push(r.method() + " " + r.url()));
  await page.route("**/api/vergers/own", async (route) => {
    expect(route.request().headers()["authorization"]).toBe(
      "Bearer test-only-token",
    );
    await new Promise((resolve) => setTimeout(resolve, 150));
    await route.fulfill({ json: orchard });
  });
  await mockSession(page);
  await fillDraft(page);
  expect(await page.locator("#orchardId option").count()).toBe(2);
  await page
    .getByRole("button", { name: "Vérifier le brouillon" })
    .evaluate((button: HTMLButtonElement) => {
      button.click();
      button.click();
    });
  await expect(page.getByRole("status")).toContainText("Aucun envoi effectué");
  expect(requests.filter((r) => r.includes("/api/vergers/own"))).toHaveLength(
    1,
  );
  expect(
    requests.filter((r) => /\/api\/(recoltes|harvests)/.test(r)),
  ).toHaveLength(0);
  expect(
    await page.evaluate(() => ({
      local: localStorage.length,
      session: sessionStorage.length,
    })),
  ).toEqual({ local: 0, session: 0 });
});
for (const status of [400, 403, 404, 409])
  test(`HTTP ${status} keeps the farmer draft`, async ({ page }) => {
    await page.route("**/api/vergers/own", (route) =>
      route.fulfill({
        status,
        json: {
          code: "TEST_ERROR",
          message: "internal stack trace",
          fieldErrors: status === 400 ? { orchardId: "bad" } : undefined,
        },
      }),
    );
    await mockSession(page);
    await fillDraft(page);
    await page.getByRole("button", { name: "Vérifier le brouillon" }).click();
    await expect(page.getByRole("alert")).toBeVisible();
    await expect(page.getByLabel("Quantité estimée")).toHaveValue("1234");
    await expect(page.getByText("internal stack trace")).toHaveCount(0);
  });
test("401 keeps draft and reauthentication resumes for same farmer", async ({
  page,
}) => {
  await page.route("**/api/vergers/own", (route) =>
    route.fulfill({ status: 401, json: { code: "UNAUTHORIZED" } }),
  );
  await mockSession(page);
  await fillDraft(page);
  await page.getByRole("button", { name: "Vérifier le brouillon" }).click();
  await expect(
    page.getByRole("heading", { name: "Session expirée" }),
  ).toBeVisible();
  await expect(page.getByLabel("Quantité estimée")).toHaveValue("1234");
  await page.getByLabel("Adresse e-mail").fill(user.email);
  await page.getByLabel("Mot de passe").fill("test-only-password");
  await page.getByRole("button", { name: "Se connecter" }).click();
  await expect(
    page.getByRole("button", { name: "Vérifier le brouillon" }),
  ).toBeEnabled();
  await expect(page.getByLabel("Quantité estimée")).toHaveValue("1234");
});
test("network failure preserves draft and retry works", async ({ page }) => {
  await page.route("**/api/vergers/own", (route) => route.abort());
  await mockSession(page);
  await fillDraft(page);
  await page.getByRole("button", { name: "Vérifier le brouillon" }).click();
  await expect(page.getByRole("alert")).toContainText("Impossible de joindre");
  await expect(page.getByLabel("Quantité estimée")).toHaveValue("1234");
  await page.unroute("**/api/vergers/own");
  await page.route("**/api/vergers/own", (route) =>
    route.fulfill({ json: orchard }),
  );
  await page.getByRole("button", { name: "Vérifier le brouillon" }).click();
  await expect(page.getByRole("status")).toContainText("Brouillon vérifié");
});
test("non farmer cannot load the farmer data", async ({ page }) => {
  const calls: string[] = [];
  page.on("request", (r) => {
    if (new URL(r.url()).pathname.startsWith("/api/vergers"))
      calls.push(r.url());
  });
  await mockSession(page, ["RESPONSABLE_COLLECTE"]);
  await expect(page.getByRole("alert")).toContainText(
    "ne possède pas le rôle AGRICULTEUR",
  );
  expect(calls).toEqual([]);
});
test("orchard loading and failure states are real and retryable", async ({
  page,
}) => {
  await mockSession(page);
  await page.route("**/api/vergers?*", async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 500));
    await route.fulfill({ status: 503, json: {} });
  });
  // Start a fresh session on reload to exercise initial orchard loading.
  await page.reload();
  await page.getByLabel("Adresse e-mail").fill(user.email);
  await page.getByLabel("Mot de passe").fill("test-only-password");
  await page.getByRole("button", { name: "Se connecter" }).click();
  await expect(page.getByText("Chargement de vos vergers…")).toBeVisible();
  await expect(page.getByRole("alert")).toContainText(
    "Service momentanément indisponible",
  );
  await page.route("**/api/vergers?*", (route) =>
    route.fulfill({ json: { items: [], page: 0, totalPages: 0 } }),
  );
  await page
    .getByRole("button", { name: "Réessayer le chargement des vergers" })
    .click();
  await expect(
    page.getByText("0 verger(s) actif(s)", { exact: false }),
  ).toBeVisible();
});
