import { expect, test } from "@playwright/test";
import { mockMemberSession } from "./member-session";
test.beforeEach(async ({ page }) => {
  await mockMemberSession(page);
});

test("landing page leads through the publication preview to the checkout prototype", async ({
  page,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/");
  await expect(page.getByRole("heading", { name: /探索 AI/ })).toBeVisible();
  await page.getByRole("link", { name: "探索这篇内容" }).first().click();
  await expect(page).toHaveURL(/\/publications\/website-from-zero$/);
  await expect(
    page.getByRole("heading", { name: "从零做一个可上线的网站", level: 1 }),
  ).toBeVisible();
  await page.getByRole("link", { name: "查看积分解锁信息" }).click();
  await expect(
    page.getByRole("heading", { name: /为下一次实践/ }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "积分解锁准备中" }),
  ).toBeDisabled();
  expect(errors).toEqual([]);
});

test("category and search filters work without the backend", async ({
  page,
}) => {
  await page.goto("/discover");
  await expect(page.locator(".hub-content-card")).toHaveCount(4);
  await page.getByRole("button", { name: "自动化", exact: true }).click();
  await expect(page.locator(".hub-content-card")).toHaveCount(1);
  await page.goto("/discover?q=工作流");
  await expect(page.locator(".hub-content-card")).toHaveCount(1);
  await page.goto("/publications?q=不存在的案例");
  await expect(
    page.getByRole("heading", { name: "没有找到匹配的内容" }),
  ).toBeVisible();
});

test("free publication opens the reading prototype", async ({ page }) => {
  await page.goto("/publications/personal-knowledge");
  await page.getByRole("link", { name: "开始阅读" }).click();
  await expect(page).toHaveURL(/\/learn\/personal-knowledge$/);
  await expect(
    page.getByRole("heading", { name: "梳理已有资料" }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});

test("concept routes render on desktop and mobile", async ({ page }) => {
  for (const path of [
    "/home",
    "/discover",
    "/community",
    "/members",
    "/my-content",
    "/studio",
  ]) {
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

test("missing publication shows the not-found view", async ({ page }) => {
  await page.goto("/publications/does-not-exist");
  await expect(
    page.getByRole("heading", { name: "这份内容暂时找不到了" }),
  ).toBeVisible();
});
