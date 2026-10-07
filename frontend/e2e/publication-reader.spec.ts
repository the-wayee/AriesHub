import { test, expect } from "@playwright/test";
import { mockMemberSession } from "./member-session";
import { mockReaderApi } from "./reader-api";
import { fulfillResult } from "./api-result";
test("free detail saves real reading position and exposes it in personal history", async ({
  page,
  request,
}) => {
  const content = await (
    await request.get("/api/v1/publications/4/content")
  ).json();
  await mockMemberSession(page);
  const api = await mockReaderApi(page);
  await page.goto("/publications/4");
  await expect(
    page.getByText("阅读位置随账号保存", { exact: true }),
  ).toBeVisible();
  // 封面高度与视口有关，先进入真实正文，再验证正文内的阅读位置。
  await page.locator(".hub-detail-body h2").first().scrollIntoViewIfNeeded();
  await page.mouse.wheel(0, 700);
  await expect
    .poll(() => api.progress.get("4")?.percent ?? 0)
    .toBeGreaterThan(0);
  expect(api.progress.get("4")?.version).toBe(content.data.version);
  await page.goto("/my-content");
  await page.getByRole("button", { name: "阅读记录", exact: true }).click();
  await expect(
    page.getByRole("link", { name: /继续阅读 · 上次读到/ }),
  ).toBeVisible();
  await page.getByRole("link", { name: /继续阅读 · 上次读到/ }).click();
  await expect(page).toHaveURL(/resume=1/);
  await expect(
    page.getByRole("button", { name: /继续上次位置/ }),
  ).toBeVisible();
  await expect.poll(() => page.evaluate(() => scrollY)).toBeGreaterThan(300);
});
test("failed relation changes preserve the previous state and can retry", async ({
  page,
}) => {
  await mockMemberSession(page);
  await mockReaderApi(page);
  let fail = true;
  await page.route("**/api/v1/publications/4/bookmark", (route) =>
    fail
      ? fulfillResult(route, {
          status: 503,
          json: { code: "SERVICE_UNAVAILABLE", msg: "收藏暂时不可用" },
        })
      : fulfillResult(route, {
          json: {
            publicationId: "4",
            likeCount: 0,
            liked: false,
            bookmarked: true,
          },
        }),
  );
  await page.goto("/discover");
  const card = page.locator(".hub-content-card").first();
  await card.getByRole("button", { name: "收藏文章", exact: true }).click();
  await expect(card.getByRole("alert")).toContainText("收藏暂时不可用");
  await expect(
    card.getByRole("button", { name: "收藏文章", exact: true }),
  ).toHaveAttribute("aria-pressed", "false");
  fail = false;
  await card.getByRole("button", { name: "收藏文章", exact: true }).click();
  await expect(
    card.getByRole("button", { name: "取消文章收藏", exact: true }),
  ).toBeVisible();
});
test("discovery form and price filters remain in the URL with genuine empty states", async ({
  page,
}) => {
  await mockMemberSession(page);
  await mockReaderApi(page);
  await page.goto("/discover");
  await page.getByLabel("内容形式筛选").selectOption("ARTICLE");
  await expect(page).toHaveURL(/type=ARTICLE/);
  await expect(page.locator(".hub-content-card")).toHaveCount(1);
  await page.getByLabel("阅读方式筛选").selectOption("CREDIT");
  await expect(
    page.getByRole("heading", { name: "没有找到匹配的内容" }),
  ).toBeVisible();
  await page.reload();
  await expect(page.getByLabel("阅读方式筛选")).toHaveValue("CREDIT");
});
test("a changed body version never restores the old percentage as current", async ({
  page,
}) => {
  await mockMemberSession(page);
  await mockReaderApi(page);
  await page.route("**/api/v1/publications/4/reading-progress", (r) =>
    fulfillResult(r, {
      json: {
        publicationId: "4",
        version: "old",
        position: "read-old-0",
        percent: 90,
        updatedAt: new Date().toISOString(),
      },
    }),
  );
  await page.goto("/publications/4?resume=1");
  await expect(
    page.getByText("正文已更新，旧阅读位置不再适用；请从新版正文开始。"),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "从新版开始阅读" }),
  ).toBeVisible();
  expect(await page.evaluate(() => scrollY)).toBeLessThan(200);
});
