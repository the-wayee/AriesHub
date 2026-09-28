import { expect, test } from "@playwright/test";

test("database-backed homepage opens a free tutorial", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/");
  await page
    .getByRole("link", { name: /把一个主题，整理成一份演示提纲/ })
    .click();
  await expect(
    page.getByRole("heading", { name: "完整教程", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "01 / 写出任务说明" }),
  ).toBeVisible();
  await page.getByRole("link", { name: "阅读完整教程" }).click();
  await expect(page).toHaveURL(/#read$/);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  expect(errors).toEqual([]);
});

test("keyword, category and access filters combine and can be cleared", async ({
  page,
}) => {
  await page.goto("/cases");
  await page.getByLabel("关键词").fill("Codex");
  await page.getByLabel("内容方向").selectOption("coding");
  await page.getByLabel("阅读方式").selectOption("PAID");
  await page.getByRole("button", { name: "筛选案例" }).click();
  await expect(page.locator(".case-card")).toHaveCount(1);
  await expect(
    page.getByRole("heading", { name: "用 Codex 做一个专注计时页面" }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.getByRole("link", { name: "清除筛选" }).click();
  await expect(page.locator(".case-card")).toHaveCount(3);
});

test("pagination preserves search and changes the case", async ({ page }) => {
  await page.goto("/cases?access=FREE&size=1");
  await expect(
    page.getByRole("heading", { name: "把一个主题，整理成一份演示提纲" }),
  ).toBeVisible();
  await page.getByRole("link", { name: "下一页" }).click();
  await expect(page).toHaveURL(/access=FREE.*page=2.*size=1/);
  await expect(
    page.getByRole("heading", { name: "给散落的素材，建立一套整理规则" }),
  ).toBeVisible();
});

test("empty and malformed searches have useful states", async ({ page }) => {
  await page.goto("/cases?q=not-a-real-case");
  await expect(
    page.getByRole("heading", { name: "暂时没有符合条件的案例" }),
  ).toBeVisible();
  await page.goto("/cases?page=0");
  await expect(
    page.getByRole("heading", { name: "筛选条件不正确" }),
  ).toBeVisible();
});

test("paid preview never contains the private body", async ({
  page,
  request,
}) => {
  await page.goto("/cases/codex-focus-page");
  await expect(
    page.getByRole("heading", { name: "完整内容，稍后开放" }),
  ).toBeVisible();
  expect(await page.content()).not.toContain("PRIVATE_DEMO_BODY");
  const detail = await request.get("/api/v1/cases/codex-focus-page");
  expect(detail.ok()).toBe(true);
  const data = await detail.json();
  const body = await request.get(`/api/v1/cases/${data.caseInfo.id}/content`);
  expect(body.status()).toBe(403);
  expect(await body.text()).not.toContain("PRIVATE_DEMO_BODY");
});

test("missing cases show the not-found view", async ({ page }) => {
  await page.goto("/cases/does-not-exist");
  await expect(
    page.getByRole("heading", { name: "这份内容暂时找不到了" }),
  ).toBeVisible();
  await expect(page.getByRole("link", { name: /返回案例库/ })).toBeVisible();
});

test("development proxy reaches the real Java database health endpoint", async ({
  request,
}) => {
  const response = await request.get("/api/v1/health");
  expect(response.ok()).toBe(true);
  expect(await response.json()).toEqual({ status: "UP", database: "UP" });
  expect(response.headers()["x-request-id"]).toBeTruthy();
});
