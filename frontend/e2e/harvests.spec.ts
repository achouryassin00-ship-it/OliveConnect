import { test, expect } from "@playwright/test";
test("filters combine status, urgency and empty results", async ({ page }) => {
  await page.goto("/collectes");
  await page.getByLabel("Statut", { exact: true }).selectOption("PLANIFIEE");
  await expect(page.locator("tbody tr")).toHaveCount(1);
  await expect(page.locator("tbody")).toContainText("COL-DEMO-205");
  await page.getByLabel("Type", { exact: true }).selectOption("urgent");
  await expect(page.getByText("Aucun élément à afficher")).toBeVisible();
  await page.getByLabel("Statut", { exact: true }).selectOption("");
  await expect(page.locator("tbody tr")).toHaveCount(1);
  await expect(page.locator("tbody")).toContainText("COL-DEMO-204");
});
test("demo: validations, own active orchards, one local creation, filters and details", async ({
  page,
}) => {
  await page.goto("/collectes/nouvelle");
  await page.getByRole("button", { name: "Ajouter un exemple local" }).click();
  await expect(
    page.getByText("Sélectionnez un de vos vergers actifs."),
  ).toBeVisible();
  const options = page.locator("#orchardId option");
  expect(await options.count()).toBe(3);
  await page.getByLabel("Votre verger").selectOption("demo-v1");
  await page.getByLabel("Date souhaitée").fill("2099-11-28");
  await page.getByLabel("Quantité estimée").fill("-5");
  await page.getByLabel("Type de demande").selectOption("urgent");
  await page.getByRole("button", { name: "Ajouter un exemple local" }).click();
  await expect(
    page.getByText("Indiquez un poids positif", { exact: false }),
  ).toBeVisible();
  await expect(page.getByText("Précisez le motif de l’urgence.")).toBeVisible();
  await page.getByLabel("Quantité estimée").fill("1250,5");
  await page.getByLabel("Motif de l’urgence").fill("Olives mûres");
  await page
    .getByRole("button", { name: "Ajouter un exemple local" })
    .evaluate((button: HTMLButtonElement) => {
      button.click();
      button.click();
    });
  await expect(page.getByRole("status")).toContainText("Exemple local ajouté");
  await expect(page.locator("tbody tr")).toHaveCount(4);
  await page.getByLabel("Rechercher une collecte").fill("LOCAL-");
  await expect(page.locator("tbody tr")).toHaveCount(1);
  await page.getByRole("link", { name: /^Voir LOCAL-/ }).click();
  await expect(page.getByText("Olives mûres", { exact: false })).toBeVisible();
  await expect(
    page.getByText("Historique non disponible", { exact: false }),
  ).toBeVisible();
  await expect(page.getByRole("button", { name: /Annuler/ })).toHaveCount(0);
  await page.goto("/collectes/COL-OTHER");
  await expect(page.getByText("Demande introuvable")).toBeVisible();
});
test("320px harvest form and details remain usable", async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 740 });
  for (const path of ["/collectes/nouvelle", "/collectes/COL-DEMO-205"]) {
    await page.goto(path);
    await expect(page.locator("main h1")).toBeVisible();
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBeTruthy();
  }
});
