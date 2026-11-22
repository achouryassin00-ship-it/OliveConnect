import { test, expect } from "@playwright/test";
test("desktop navigation, state previews and honest demo boundaries", async ({
  page,
}) => {
  const errors: string[] = [];
  const apiCalls: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("request", (request) => {
    if (request.url().includes("/api/")) apiCalls.push(request.url());
  });
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: "Bonjour, Yassine" }),
  ).toBeVisible();
  for (const label of [
    "Vergers",
    "Collectes",
    "Alertes terrain",
    "Tournées",
    "Ressources",
    "Carte des vergers",
    "Pressoir",
    "Statistiques",
    "Administration",
    "Vue générale",
  ]) {
    await page
      .getByRole("navigation")
      .getByRole("link", { name: label, exact: true })
      .click();
    await expect(page.locator("main h1")).toBeVisible();
  }
  await page.goto("/collectes");
  await page
    .getByRole("textbox", { name: "Rechercher une collecte" })
    .fill("introuvable");
  await expect(page.getByText("Aucun élément à afficher")).toBeVisible();
  await page.getByRole("combobox").selectOption("error");
  await expect(page.getByRole("alert")).toBeVisible();
  await page
    .getByRole("button", { name: "Réessayer la démonstration" })
    .click();
  await expect(page.getByRole("combobox")).toHaveValue("ready");
  await page.getByRole("combobox").selectOption("loading");
  await expect(page.getByText("Chargement en cours")).toBeVisible();
  await page.goto("/carte");
  await page.getByRole("button", { name: /Mahrès 09/ }).click();
  await expect(
    page.getByText("Mahrès 09 · Position illustrative"),
  ).toBeVisible();
  await page.reload();
  await expect(
    page.getByRole("heading", { name: "Carte des vergers" }),
  ).toBeVisible();
  await page.goto("/connexion");
  await expect(
    page.getByRole("button", { name: /Connexion disponible/ }),
  ).toBeDisabled();
  await page.getByRole("link", { name: "Demander mon inscription" }).click();
  await expect(
    page.getByRole("heading", { name: "Rejoindre la coopérative" }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Explorer la démonstration" }).click();
  await page.goto("/adresse-inconnue");
  await expect(
    page.getByRole("heading", { name: "Page introuvable" }),
  ).toBeVisible();
  expect(errors).toEqual([]);
  expect(apiCalls).toEqual([]);
});
test("320px mobile menu and every route fit the viewport", async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 740 });
  await page.goto("/dashboard");
  await page.getByRole("button", { name: "Ouvrir le menu" }).click();
  await page
    .getByRole("navigation")
    .getByRole("link", { name: "Vergers", exact: true })
    .click();
  await expect(
    page.getByRole("button", { name: "Ouvrir le menu" }),
  ).toBeVisible();
  for (const path of [
    "dashboard",
    "vergers",
    "collectes",
    "alertes",
    "tournees",
    "ressources",
    "carte",
    "pressoir",
    "statistiques",
    "administration",
    "connexion",
    "inscription",
  ]) {
    await page.goto("/" + path);
    await expect(page.locator("h1").last()).toBeVisible();
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
      path,
    ).toBeTruthy();
  }
});
