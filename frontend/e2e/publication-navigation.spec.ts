import { expect, test } from "@playwright/test";
import { mockMemberSession } from "./member-session";
import { mockReaderApi } from "./reader-api";
import { fulfillResult } from "./api-result";

test.beforeEach(async ({ page }) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await mockMemberSession(page);
  await mockReaderApi(page);
  await page.route("**/api/v1/inspiration/quote", (route) =>
    fulfillResult(route, { json: null }),
  );
});

test("home article keeps the home navigation and returns home after an article reload", async ({
  page,
}) => {
  await page.goto("/home");
  await expect(page.locator(".app-boot-overlay")).toHaveCount(0);
  await page
    .locator('.home-practice-gallery a[href="/publications/4"]')
    .first()
    .click();
  await expect(page).toHaveURL(/\/publications\/4$/);
  const back = page.getByRole("link", { name: "返回首页" });
  await expect(back).toHaveAttribute("href", "/home");
  await expect(
    page
      .getByRole("navigation", { name: "社区导航" })
      .getByRole("link", { name: "首页", exact: true }),
  ).toHaveAttribute("aria-current", "page");
  await page.reload();
  await expect(page.locator(".app-boot-overlay")).toHaveCount(0);
  await expect(back).toHaveAttribute("href", "/home");
  await back.click();
  await expect(page).toHaveURL(/\/home$/);
});

test("explore article returns to the original query and category", async ({
  page,
}) => {
  const source = "/discover?q=AI&category=coding&page=1";
  await page.goto(source);
  await expect(page.locator(".app-boot-overlay")).toHaveCount(0);
  await page
    .locator('.hub-discover-grid a[href="/publications/4"]')
    .first()
    .click();
  const back = page.getByRole("link", { name: "返回探索" });
  await expect(back).toHaveAttribute("href", source);
  await back.click();
  await expect
    .poll(() => new URL(page.url()).pathname + new URL(page.url()).search)
    .toBe(source);
});

test("a direct article defaults to explore and ignores an external return target", async ({
  page,
}) => {
  await page.addInitScript(() => {
    sessionStorage.setItem(
      "arieshub:publication-entry:/publications/4",
      "//example.com",
    );
  });
  await page.goto("/publications/4");
  await expect(page.locator(".app-boot-overlay")).toHaveCount(0);
  await expect(page.getByRole("link", { name: "返回探索" })).toHaveAttribute(
    "href",
    "/discover",
  );
});
