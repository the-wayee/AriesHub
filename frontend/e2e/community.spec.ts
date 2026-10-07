import { mockReaderApi } from "./reader-api";
import { expect, test } from "@playwright/test";
import { mockMemberSession } from "./member-session";

test.beforeEach(async ({ page }) => {
  await mockMemberSession(page);
  await mockReaderApi(page);
  await page.goto("/");
  await page.evaluate(() => localStorage.clear());
});

test("member home combines real articles with community activity", async ({
  page,
}) => {
  await page.goto("/home");
  await expect(
    page.getByRole("heading", { name: "从一个想法，开始新的实践" }),
  ).toBeVisible();
  await expect(page.getByRole("heading", { name: "主理人精选" })).toBeVisible();
  await expect(
    page.getByRole("button", { name: "最新发布", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "继续阅读", exact: true }),
  ).toHaveCount(0);
  await expect(page.getByText("Aries 刚发布了一篇文章")).toHaveCount(0);
  await expect(page.getByRole("heading", { name: "社区的此刻" })).toBeVisible();
  // 动态已使用真实接口；尚未接入的讨论推荐保留示例标记。
  await expect(page.getByText("动态示例", { exact: true })).toHaveCount(0);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});

test("community widget filters and answer preview remain distinct from publication", async ({
  page,
}, testInfo) => {
  await page.goto("/home");
  await expect(page.getByRole("heading", { name: "遇见同路人" })).toHaveCount(0);
  await expect(page.getByRole("heading", { name: "大家在聊" })).toBeVisible();
  await page.getByRole("button", { name: "评论与回复", exact: true }).click();
  await expect(page.locator(".community-activity-list article")).toHaveCount(2);
  await page.getByRole("button", { name: "新文章", exact: true }).click();
  await expect(page.locator(".community-activity-list article")).toHaveCount(1);
  await page
    .getByLabel("预览你的回答")
    .fill("我把每周的资料整理做成了一个脚本。");
  await page.getByRole("button", { name: "预览回答", exact: true }).click();
  await expect(page.locator(".community-own-preview")).toContainText(
    "仅在当前页面展示，尚未发布",
  );
  await page.getByRole("button", { name: "全部动态", exact: true }).click();
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.screenshot({
    path: testInfo.outputPath("member-home.png"),
    fullPage: true,
  });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});
test("server bookmarks and likes persist across reload and can be removed", async ({
  page,
}) => {
  await page.goto("/discover");
  const first = page.locator(".hub-content-card").first();
  await first.getByRole("button", { name: "收藏文章", exact: true }).click();
  await expect(
    first.getByRole("button", { name: "取消文章收藏", exact: true }),
  ).toBeVisible();
  await first.getByRole("button", { name: "点赞文章", exact: true }).click();
  await expect(
    first.getByRole("button", { name: "取消文章点赞", exact: true }),
  ).toBeVisible();
  await page.reload();
  await expect(
    page
      .locator(".hub-content-card")
      .first()
      .getByRole("button", { name: "取消文章收藏", exact: true }),
  ).toBeVisible();
  await page.goto("/my-content");
  await expect(
    page.getByRole("heading", { name: "AI 需求实践", exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "取消文章收藏", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "给喜欢的内容，留一个位置" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "我的点赞", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "AI 需求实践", exact: true }),
  ).toBeVisible();
});
test("prototype chapter choices are not imported as real reading history", async ({
  page,
}) => {
  await page.goto("/learn/1");
  await page.getByRole("button", { name: /网站设计与页面实现/ }).click();
  await expect(
    page.getByRole("heading", { name: "这一节属于完整内容" }),
  ).toBeVisible();
  await page.goto("/my-content");
  await page.getByRole("button", { name: "阅读记录", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "还没有阅读记录" }),
  ).toBeVisible();
});

test("a local discussion can be created without implying server publication", async ({
  page,
}) => {
  await page.goto("/community");
  await page.getByRole("button", { name: "发起讨论" }).click();
  await page.getByLabel("讨论标题").fill("我完成了第一个自动化流程");
  await page
    .getByLabel("讨论内容")
    .fill("整理了输入、节点和失败重试，想听听大家的改进建议。");
  await page.getByRole("button", { name: "发布到当前浏览器" }).click();
  await expect(
    page.getByRole("heading", { name: "我完成了第一个自动化流程" }),
  ).toBeVisible();
  await page.reload();
  await expect(
    page.getByRole("heading", { name: "我完成了第一个自动化流程" }),
  ).toBeVisible();
});
