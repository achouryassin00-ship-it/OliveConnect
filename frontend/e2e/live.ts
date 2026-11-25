import { test, expect } from "@playwright/test";
import process from "node:process";
test("Spring Boot + MongoDB: ownership, authentication, draft check and logout", async ({
  page,
  request,
}) => {
  const password = process.env.J04_TEST_PASSWORD;
  if (!password)
    throw new Error(
      "Run scripts/verify-live.py to provision the isolated test backend.",
    );
  const api = "http://127.0.0.1:8085/api";
  expect((await request.get(`${api}/vergers`)).status()).toBe(401);
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
  for (const v of listA.items) {
    expect(v.proprietaireId).toBe(a.user.id);
    expect(v.actif).toBe(true);
  }
  expect(
    (
      await request.get(`${api}/vergers/${listB.items[0].id}`, {
        headers: { Authorization: `Bearer ${a.token}` },
      })
    ).status(),
  ).toBe(404);
  const errors: string[] = [];
  const harvestCalls: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  page.on("request", (r) => {
    if (/^\/api\/(recoltes|harvests)/.test(new URL(r.url()).pathname))
      harvestCalls.push(r.url());
  });
  await page.goto("/connexion");
  await page.getByLabel("Adresse e-mail").fill("ali.benmbarek@sfax.tn");
  await page.getByLabel("Mot de passe").fill(password);
  await page.getByRole("button", { name: "Se connecter" }).click();
  await expect(
    page.getByRole("heading", { name: "Mes demandes de collecte" }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Nouvelle demande" }).click();
  await expect(page.locator("#orchardId option")).toHaveCount(
    listA.items.length + 1,
  );
  await page.getByLabel("Votre verger").selectOption(listA.items[0].id);
  await page.getByLabel("Date souhaitée").fill("2099-11-28");
  await page.getByLabel("Quantité estimée").fill("1600");
  await page.getByRole("button", { name: "Vérifier le brouillon" }).click();
  await expect(page.getByRole("status")).toContainText("Brouillon vérifié");
  await page.setViewportSize({ width: 1440, height: 1050 });
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.screenshot({
    path: "../tmp/j04-connected-desktop.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 320, height: 740 });
  await page.evaluate(() => window.scrollTo(0, 0));
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  await page.screenshot({
    path: "../tmp/j04-connected-mobile.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "Ouvrir le menu" }).click();
  await page.getByRole("button", { name: "Fermer ma session" }).click();
  await expect(page.getByLabel("Adresse e-mail")).toBeVisible();
  await page.getByLabel("Adresse e-mail").fill("fatma.triki@sfax.tn");
  await page.getByLabel("Mot de passe").fill(password);
  await page.getByRole("button", { name: "Se connecter" }).click();
  await expect(page.locator("#orchardId option")).toHaveCount(
    listB.items.length + 1,
  );
  await expect(page.getByLabel("Quantité estimée")).toHaveValue("");
  await expect(
    page.locator(`#orchardId option[value="${listA.items[0].id}"]`),
  ).toHaveCount(0);
  await page.reload();
  await expect(page.getByLabel("Adresse e-mail")).toBeVisible();
  expect(errors).toEqual([]);
  expect(harvestCalls).toEqual([]);
});
