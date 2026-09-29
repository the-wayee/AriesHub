import { expect, test } from "@playwright/test";

test("landing page leads through the case preview to the checkout prototype", async ({
  page,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/");
  await expect(page.getByRole("heading", { name: /探索 AI/ })).toBeVisible();
  await page.getByRole("link", { name: "探索这篇内容" }).first().click();
  await expect(page).toHaveURL(/\/cases\/website-from-zero$/);
  await expect(
    page.getByRole("heading", { name: "从零做一个可上线的网站", level: 1 }),
  ).toBeVisible();
  await page.getByRole("link", { name: "查看购买信息" }).click();
  await expect(
    page.getByRole("heading", { name: "确认这份案例" }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "支付功能准备中" }),
  ).toBeDisabled();
  expect(errors).toEqual([]);
});

test("category and search filters work without the backend", async ({
  page,
}) => {
  await page.goto("/cases");
  await expect(page.locator(".archive-case")).toHaveCount(4);
  await page.getByRole("link", { name: "自动化", exact: true }).first().click();
  await expect(page.locator(".archive-case")).toHaveCount(1);
  await page.getByRole("searchbox", { name: "搜索案例" }).fill("工作流");
  await page.getByRole("button", { name: "搜索" }).click();
  await expect(page.locator(".archive-case")).toHaveCount(1);
  await page.goto("/cases?q=不存在的案例");
  await expect(
    page.getByRole("heading", { name: "没有找到匹配的案例。" }),
  ).toBeVisible();
});

test("free case opens the reading prototype", async ({ page }) => {
  await page.goto("/cases/personal-knowledge");
  await page.getByRole("link", { name: "开始阅读" }).click();
  await expect(page).toHaveURL(/\/learn\/personal-knowledge$/);
  await expect(
    page.getByRole("heading", { name: "从一个具体问题开始" }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});

test("concept routes render on desktop and mobile", async ({ page }) => {
  for (const path of ["/community", "/members", "/my-content", "/studio"]) {
    const response = await page.goto(path);
    expect(response?.status()).toBe(200);
    await expect(page.locator("h1").first()).toBeVisible();
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true);
  }
});

test("missing case shows the not-found view", async ({ page }) => {
  await page.goto("/cases/does-not-exist");
  await expect(
    page.getByRole("heading", { name: "这份内容暂时找不到了" }),
  ).toBeVisible();
});
