import { expect, test } from "@playwright/test";
import { mockMemberSession } from "./member-session";
import { fulfillResult } from "./api-result";

test.beforeEach(async ({ page }) => {
  let requests = 0;
  await page.route("**/api/v1/inspiration/quote", async (route) => {
    requests++;
    await fulfillResult(route, {
      json: {
        text: `接口返回的哲学文案${requests}`,
        source: "测试作者 · 测试出处",
      },
    });
  });
});

test("document reload fills Aries before revealing the page, soft navigation stays immediate", async ({
  page,
}, info) => {
  await page.goto("/login", { waitUntil: "domcontentloaded" });
  const overlay = page.getByRole("status", {
    name: "正在加载页面",
    exact: true,
  });
  await expect(overlay).toBeVisible();
  await expect(page.locator(".app-boot-content")).toHaveCSS("opacity", "0");
  await page.screenshot({ path: info.outputPath("aries-loading.png") });
  await expect(page.locator(".animated-caption")).toHaveAttribute(
    "aria-label",
    "接口返回的哲学文案1",
  );
  await expect(page.locator(".animated-caption-character").last()).toHaveCSS(
    "opacity",
    "1",
  );
  // 快接口也应在填充阶段读到文案，不追加阅读停留。
  expect(
    Number(await page.locator(".app-boot-fill").getAttribute("y")),
  ).toBeGreaterThan(0);
  await page.screenshot({
    path: info.outputPath("aries-philosophy-complete.png"),
  });
  await expect(overlay).toHaveCount(0);
  await expect(page.locator(".app-boot-content")).toHaveCSS("opacity", "1");
  await expect(page.locator(".app-boot-content")).not.toHaveAttribute(
    "inert",
    "",
  );
  await page.reload({ waitUntil: "domcontentloaded" });
  await expect(overlay).toBeVisible();
  await expect(page.locator(".animated-caption")).toHaveAttribute(
    "aria-label",
    "接口返回的哲学文案2",
  );
  await expect(overlay).toHaveCount(0);
  await page.locator('a[href="/register"]').first().click();
  await expect(page).toHaveURL(/\/register/);
  await expect(overlay).toHaveCount(0);
});

test("landing entry replays Aries and requests a fresh philosophy quote", async ({
  page,
}) => {
  await mockMemberSession(page);
  await page.route("**/api/v1/home", (route) =>
    fulfillResult(route, {
      json: { categories: [], featured: [], latest: [], continueReading: [] },
    }),
  );
  await page.goto("/");
  await expect(page.locator(".app-boot-overlay")).toHaveCount(0);
  await page.locator('a[href="/home"]').first().click();
  await expect(page).toHaveURL(/\/home/);
  await expect(page.locator(".app-boot-overlay")).toBeVisible();
  await expect(page.locator(".animated-caption")).toHaveAttribute(
    "aria-label",
    "接口返回的哲学文案2",
  );
  await expect(page.locator(".app-boot-overlay")).toHaveCount(0);
});

test("unavailable quote API does not invent copy or block the page", async ({
  page,
}) => {
  await page.route("**/api/v1/inspiration/quote", (route) => route.abort());
  await page.goto("/login", { waitUntil: "domcontentloaded" });
  await expect(page.locator(".app-boot-overlay")).toBeVisible();
  await expect(page.locator(".animated-caption")).toHaveCount(0);
  await expect(page.locator(".app-boot-overlay")).toHaveCount(0);
  await expect(page.locator(".app-boot-content")).toHaveCSS("opacity", "1");
});

test("slow page data holds the symbol and a failed request reveals its actual error", async ({
  page,
}) => {
  await mockMemberSession(page);
  let release!: () => void;
  const pending = new Promise<void>((resolve) => {
    release = resolve;
  });
  await page.route("**/api/v1/home", async (route) => {
    await pending;
    await fulfillResult(route, {
      status: 503,
      json: { code: "SERVICE_UNAVAILABLE", msg: "请稍后重新加载" },
    });
  });
  await page.goto("/home", { waitUntil: "domcontentloaded" });
  const overlay = page.locator(".app-boot-overlay");
  await expect(page.locator(".app-boot-fill")).toHaveAttribute("y", "24");
  await expect(overlay).toBeVisible();
  await expect(page.locator(".app-boot-content")).toHaveCSS("opacity", "0");
  release();
  await expect(overlay).toHaveCount(0);
  await expect(
    page.getByRole("heading", { name: "首页暂时无法读取" }),
  ).toBeVisible();
  await expect(page.locator(".app-boot-content")).not.toHaveAttribute(
    "inert",
    "",
  );
});
