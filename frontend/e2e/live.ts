import { test, expect } from "@playwright/test";
import process from "node:process";

test("Spring Boot + MongoDB: full harvest request creation, history, cancel, isolation and responsive UI", async ({
  page,
  request,
}) => {
  const password = process.env.J05_TEST_PASSWORD;
  if (!password)
    throw new Error(
      "Run scripts/verify-live.py to provision the isolated test backend.",
    );
  const api = "http://127.0.0.1:8085/api";

  expect((await request.get(`${api}/vergers`)).status()).toBe(401);
  expect((await request.get(`${api}/recoltes`)).status()).toBe(401);

  const loginA = await request.post(`${api}/auth/login`, {
    data: { email: "ali.benmbarek@sfax.tn", password },
  });
  expect(loginA.status()).toBe(200);
  const a = await loginA.json();

  const loginB = await request.post(`${api}/auth/login`, {
    data: { email: "fatma.triki@sfax.tn", password },
  });
  expect(loginB.status()).toBe(200);
  const b = await loginB.json();

  // Sécurité : l'inscription publique ne doit jamais attribuer un rôle privilégié
  const escalationAttempt = await request.post(`${api}/auth/register`, {
    data: {
      name: "Escalation Probe",
      email: `probe-${Date.now()}@sfax.tn`,
      password,
      roles: ["ADMINISTRATEUR"],
    },
  });
  expect(escalationAttempt.status()).toBe(403);

  const listA = await (
    await request.get(`${api}/vergers`, {
      headers: { Authorization: `Bearer ${a.token}` },
    })
  ).json();
  const listB = await (
    await request.get(`${api}/vergers`, {
      headers: { Authorization: `Bearer ${b.token}` },
    })
  ).json();
  expect(listA.items.length).toBeGreaterThan(0);
  expect(listB.items.length).toBeGreaterThan(0);
  expect(
    (
      await request.get(`${api}/vergers/${listB.items[0].id}`, {
        headers: { Authorization: `Bearer ${a.token}` },
      })
    ).status(),
  ).toBe(404);

  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));

  await page.goto("/connexion");
  await page.getByLabel("Adresse e-mail").fill("ali.benmbarek@sfax.tn");
  await page.getByLabel("Mot de passe").fill(password);
  await page.getByRole("button", { name: "Se connecter" }).click();

  await expect(
    page.getByRole("heading", { name: "Mes demandes de collecte" }),
  ).toBeVisible();
  await expect(page.locator("tbody")).toContainText("REC-SFX-001");

  // Création réelle d'une demande urgente dans MongoDB
  await page.getByRole("link", { name: "Nouvelle demande" }).click();
  await expect(page.locator("#orchardId option")).toHaveCount(
    listA.items.length + 1,
  );
  await page.getByLabel("Votre verger").selectOption(listA.items[0].id);
  await page.getByLabel("Date souhaitée").fill("2099-11-28");
  await page.getByLabel("Quantité estimée").fill("1600");
  await page.getByLabel("Type de demande").selectOption("urgent");
  await page
    .getByLabel("Motif de l’urgence")
    .fill("Vérification E2E réelle J05");
  await page.getByRole("button", { name: "Envoyer la demande" }).click();

  await expect(
    page.getByText("Demande de collecte enregistrée", { exact: false }),
  ).toBeVisible();
  const createdRow = page.locator("tbody tr", { hasText: "28 nov. 2099" });
  await expect(createdRow).toHaveCount(1);

  // Consultation du détail + historique réel + annulation avec confirmation
  await createdRow.getByRole("link", { name: /^Voir / }).click();
  await expect(
    page.getByRole("region", { name: "Historique des statuts" }),
  ).toContainText("À valider");
  await expect(
    page.getByText("Motif : Vérification E2E réelle J05"),
  ).toBeVisible();

  await page.setViewportSize({ width: 1440, height: 1050 });
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.screenshot({
    path: "../tmp/j05-connected-desktop.png",
    fullPage: true,
  });

  await page.getByRole("button", { name: "Annuler cette demande" }).click();
  await page
    .getByLabel("Motif d’annulation (facultatif)")
    .fill("Annulation test E2E");
  await page.getByRole("button", { name: "Confirmer l’annulation" }).click();

  await expect(
    page.getByText("a été annulée", { exact: false }),
  ).toBeVisible();
  await expect(
    page.getByRole("region", { name: "Historique des statuts" }),
  ).toContainText("Annulée");
  await expect(
    page.getByRole("button", { name: "Annuler cette demande" }),
  ).toHaveCount(0);

  const createdUrl = page.url();
  const createdId = createdUrl.split("/").pop()!;

  // Responsive 320px + déconnexion + vérification d'isolation avec Fatma
  await page.setViewportSize({ width: 320, height: 740 });
  await page.evaluate(() => window.scrollTo(0, 0));
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  await page.screenshot({
    path: "../tmp/j05-connected-mobile.png",
    fullPage: true,
  });

  await page.getByRole("button", { name: "Ouvrir le menu" }).click();
  await page.getByRole("button", { name: "Fermer ma session" }).click();
  await expect(page.getByLabel("Adresse e-mail")).toBeVisible();

  await page.getByLabel("Adresse e-mail").fill("fatma.triki@sfax.tn");
  await page.getByLabel("Mot de passe").fill(password);
  await page.getByRole("button", { name: "Se connecter" }).click();

  // Fatma est encore sur l'URL du détail d'Ali -> masquage IDOR dans l'UI
  await expect(page.getByText("Demande introuvable")).toBeVisible();
  await page.getByRole("link", { name: "Revenir aux collectes" }).click();

  await expect(page.locator("tbody")).toContainText("REC-SFX-003");
  await expect(page.locator("tbody")).not.toContainText("REC-SFX-001");

  // Isolation directe sur /api/recoltes/{createdId}
  expect(
    (
      await request.get(`${api}/recoltes/${createdId}`, {
        headers: { Authorization: `Bearer ${b.token}` },
      })
    ).status(),
  ).toBe(404);

  await page.reload();
  await expect(page.getByLabel("Adresse e-mail")).toBeVisible();
  expect(errors).toEqual([]);
});
