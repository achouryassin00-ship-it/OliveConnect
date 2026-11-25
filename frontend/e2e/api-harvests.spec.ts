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

const createdRecolte = {
  id: "rec-1",
  reference: "REC-2099-0001",
  proprietaireId: user.id,
  vergerId: orchard.id,
  vergerNom: orchard.nom,
  vergerCode: orchard.code,
  dateSouhaitee: "2099-11-28",
  quantiteEstimeeKg: 1234,
  urgent: false,
  motifUrgence: null,
  statut: "DEMANDEE",
  annulable: true,
  historique: [
    {
      ancienStatut: null,
      nouveauStatut: "DEMANDEE",
      acteurId: user.id,
      acteurNom: user.name,
      acteurRole: "AGRICULTEUR",
      motif: null,
      horodatage: "2026-11-25T12:00:00Z",
    },
  ],
  version: 0,
  createdAt: "2026-11-25T12:00:00Z",
  updatedAt: "2026-11-25T12:00:00Z",
};

async function mockSession(
  page: Page,
  roles = ["AGRICULTEUR"],
  initialItems: typeof createdRecolte[] = [],
) {
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
  await page.route("**/api/recoltes?*", (route) =>
    route.fulfill({
      json: {
        items: initialItems,
        page: 0,
        size: 20,
        totalElements: initialItems.length,
        totalPages: initialItems.length ? 1 : 0,
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

test("real mode creates harvest with Bearer and Idempotency-Key and blocks double click", async ({
  page,
}) => {
  const posts: { headers: Record<string, string>; body: unknown }[] = [];
  await page.route("**/api/recoltes", async (route) => {
    if (route.request().method() === "POST") {
      posts.push({
        headers: route.request().headers(),
        body: route.request().postDataJSON(),
      });
      await new Promise((resolve) => setTimeout(resolve, 150));
      await route.fulfill({ status: 201, json: createdRecolte });
      return;
    }
    await route.continue();
  });

  await mockSession(page);
  await fillDraft(page);
  expect(await page.locator("#orchardId option").count()).toBe(2);

  await page
    .getByRole("button", { name: "Envoyer la demande" })
    .evaluate((button: HTMLButtonElement) => {
      button.click();
      button.click();
    });

  await expect(
    page.getByText("Demande de collecte enregistrée (REC-2099-0001)."),
  ).toBeVisible();
  expect(posts).toHaveLength(1);
  expect(posts[0].headers["authorization"]).toBe("Bearer test-only-token");
  expect(posts[0].headers["idempotency-key"]).toBeTruthy();
  expect(posts[0].body).toEqual({
    vergerId: "own",
    dateSouhaitee: "2099-11-28",
    quantiteEstimeeKg: 1234,
    urgent: false,
    motifUrgence: null,
  });
  expect(
    await page.evaluate(() => ({
      local: localStorage.length,
      session: sessionStorage.length,
    })),
  ).toEqual({ local: 0, session: 0 });
});

for (const status of [400, 403, 404, 409])
  test(`HTTP ${status} keeps the farmer draft on harvest creation`, async ({
    page,
  }) => {
    await page.route("**/api/recoltes", (route) => {
      if (route.request().method() === "POST") {
        return route.fulfill({
          status,
          json: {
            code: status === 409 ? "RESOURCE_CONFLICT" : "TEST_ERROR",
            message: "internal stack trace",
            fieldErrors: status === 400 ? { dateSouhaitee: "bad" } : undefined,
          },
        });
      }
      return route.continue();
    });
    await mockSession(page);
    await fillDraft(page);
    await page.getByRole("button", { name: "Envoyer la demande" }).click();
    await expect(page.getByRole("alert")).toBeVisible();
    await expect(page.getByLabel("Quantité estimée")).toHaveValue("1234");
    await expect(page.getByText("internal stack trace")).toHaveCount(0);
  });

test("401 keeps draft and reauthentication resumes for same farmer", async ({
  page,
}) => {
  await page.route("**/api/recoltes", (route) => {
    if (route.request().method() === "POST") {
      return route.fulfill({ status: 401, json: { code: "UNAUTHORIZED" } });
    }
    return route.continue();
  });
  await mockSession(page);
  await fillDraft(page);
  await page.getByRole("button", { name: "Envoyer la demande" }).click();
  await expect(
    page.getByRole("heading", { name: "Session expirée" }),
  ).toBeVisible();
  await expect(page.getByLabel("Quantité estimée")).toHaveValue("1234");
  await page.getByLabel("Adresse e-mail").fill(user.email);
  await page.getByLabel("Mot de passe").fill("test-only-password");
  await page.getByRole("button", { name: "Se connecter" }).click();
  await expect(
    page.getByRole("button", { name: "Envoyer la demande" }),
  ).toBeEnabled();
  await expect(page.getByLabel("Quantité estimée")).toHaveValue("1234");
});

test("network failure preserves draft and retry sends same Idempotency-Key", async ({
  page,
}) => {
  const keys: string[] = [];
  await page.route("**/api/recoltes", (route) => {
    if (route.request().method() === "POST") {
      keys.push(route.request().headers()["idempotency-key"] || "");
      return route.abort();
    }
    return route.continue();
  });
  await mockSession(page);
  await fillDraft(page);
  await page.getByRole("button", { name: "Envoyer la demande" }).click();
  await expect(page.getByRole("alert")).toContainText("Impossible de joindre");
  await expect(page.getByLabel("Quantité estimée")).toHaveValue("1234");

  await page.unroute("**/api/recoltes");
  await page.route("**/api/recoltes", (route) => {
    if (route.request().method() === "POST") {
      keys.push(route.request().headers()["idempotency-key"] || "");
      return route.fulfill({ status: 201, json: createdRecolte });
    }
    return route.continue();
  });
  await page.getByRole("button", { name: "Envoyer la demande" }).click();
  await expect(
    page.getByText("Demande de collecte enregistrée (REC-2099-0001)."),
  ).toBeVisible();
  expect(keys).toHaveLength(2);
  expect(keys[0]).toBeTruthy();
  expect(keys[0]).toBe(keys[1]);
});

test("detail view shows real history and confirms cancellation", async ({
  page,
}) => {
  const cancelledRecolte = {
    ...createdRecolte,
    statut: "ANNULEE",
    annulable: false,
    version: 1,
    historique: [
      ...createdRecolte.historique,
      {
        ancienStatut: "DEMANDEE",
        nouveauStatut: "ANNULEE",
        acteurId: user.id,
        acteurNom: user.name,
        acteurRole: "AGRICULTEUR",
        motif: "Pluie",
        horodatage: "2026-11-25T13:00:00Z",
      },
    ],
  };

  await page.route("**/api/recoltes/rec-1", (route) =>
    route.fulfill({ json: createdRecolte }),
  );
  await page.route("**/api/recoltes/rec-1/annuler", (route) =>
    route.fulfill({ json: cancelledRecolte }),
  );

  await mockSession(page, ["AGRICULTEUR"], [createdRecolte]);
  await page.getByRole("link", { name: "Voir REC-2099-0001" }).click();
  await expect(
    page.getByRole("region", { name: "Historique des statuts" }),
  ).toContainText("À valider");
  await page.getByRole("button", { name: "Annuler cette demande" }).click();
  await page
    .getByLabel("Motif d’annulation (facultatif)")
    .fill("Pluie");
  await page.getByRole("button", { name: "Confirmer l’annulation" }).click();
  await expect(page.getByRole("status")).toContainText("annulée");
  await expect(
    page.getByRole("region", { name: "Historique des statuts" }),
  ).toContainText("Annulée");
  await expect(
    page.getByRole("button", { name: "Annuler cette demande" }),
  ).toHaveCount(0);
});

test("non farmer cannot load the farmer data", async ({ page }) => {
  const calls: string[] = [];
  page.on("request", (r) => {
    const pathname = new URL(r.url()).pathname;
    if (pathname.startsWith("/api/vergers") || pathname.startsWith("/api/recoltes"))
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
    await new Promise((resolve) => setTimeout(resolve, 400));
    await route.fulfill({ status: 503, json: {} });
  });
  await page.reload();
  await page.getByLabel("Adresse e-mail").fill(user.email);
  await page.getByLabel("Mot de passe").fill("test-only-password");
  await page.getByRole("button", { name: "Se connecter" }).click();
  await page.getByRole("link", { name: "Nouvelle demande" }).click();
  await expect(page.getByText("Chargement de vos vergers…")).toBeVisible();
  await expect(page.getByRole("alert")).toContainText(
    "Service momentanément indisponible",
  );
  await page.route("**/api/vergers?*", (route) =>
    route.fulfill({ json: { items: [], page: 0, totalPages: 0, totalElements: 0, size: 100 } }),
  );
  await page
    .getByRole("button", { name: "Réessayer le chargement des vergers" })
    .click();
  await expect(
    page.getByText("Aucun verger actif disponible", { exact: false }),
  ).toBeVisible();
});
