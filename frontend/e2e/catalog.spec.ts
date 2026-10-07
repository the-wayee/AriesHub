import { mockReaderApi } from "./reader-api";
import { expect, test } from "@playwright/test";
import { mockMemberSession } from "./member-session";
test.beforeEach(async ({ page }) => {
  await mockMemberSession(page);
  await mockReaderApi(page);
});

test("landing uses static examples and opens their labeled previews", async ({
  page,
}) => {
  await page.goto("/");
  await page
    .locator(".reader-landing-gallery")
    .getByRole("heading", { name: "从零做一个可上线的网站", exact: true })
    .click();
  await expect(page).toHaveURL(/\/preview\/publications\/1$/);
  await expect(
    page.getByText("界面预览 · 示例文章", { exact: true }),
  ).toBeVisible();
});

test("category and search filters use the server card contract", async ({
  page,
}) => {
  await page.goto("/discover");
  await expect(page.locator(".hub-content-card")).toHaveCount(4);
  await page.getByRole("button", { name: "日常自动化", exact: true }).click();
  await expect(page.locator(".hub-content-card")).toHaveCount(2);
  await page.goto("/discover?q=工作流");
  await expect(page.locator(".hub-content-card")).toHaveCount(1);
  await page.goto("/publications?q=不存在的案例");
  await expect(
    page.getByRole("heading", { name: "没有找到匹配的内容" }),
  ).toBeVisible();
});

test("free publication opens the reading prototype", async ({ page }) => {
  await page.goto("/preview/publications/4");
  await page.getByRole("link", { name: "开始阅读" }).click();
  await expect(page).toHaveURL(/\/learn\/4$/);
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
