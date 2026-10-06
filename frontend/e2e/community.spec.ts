import { expect, test } from "@playwright/test";
import { mockMemberSession } from "./member-session";

test.beforeEach(async ({ page }) => {
  await mockMemberSession(page);
  await page.goto("/");
  await page.evaluate(() => localStorage.clear());
});

test("member home combines content and visible community activity", async ({
  page,
}) => {
  await page.goto("/home");
  await expect(
    page.getByRole("heading", { name: "晚上好，欢迎回来。" }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "社区正在发生" }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "第一版应该保留哪些功能？" }),
  ).toBeVisible();
  await page.getByLabel("回复讨论").fill("先完成一条可验证的主路径。");
  await page.getByRole("button", { name: "发送回复" }).click();
  await expect(page.getByText("我：先完成一条可验证的主路径。")).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});

test("save and like actions persist in my space", async ({ page }) => {
  await page.goto("/discover");
  await page
    .getByRole("button", { name: /收藏：从零做一个可上线的网站/ })
    .click();
  await page
    .getByRole("button", { name: /点赞：从零做一个可上线的网站/ })
    .click();
  await page.goto("/my-content");
  await expect(
    page.getByRole("heading", { name: "从零做一个可上线的网站" }),
  ).toBeVisible();
  await page.getByRole("button", { name: /我的点赞/ }).click();
  await expect(
    page.getByRole("heading", { name: "从零做一个可上线的网站" }),
  ).toBeVisible();
});

test("reader remembers the selected chapter and keeps paid chapters locked", async ({
  page,
}) => {
  await page.goto("/learn/website-from-zero");
  await page.getByRole("button", { name: /网站设计与页面实现/ }).click();
  await expect(
    page.getByRole("heading", { name: "这一节属于完整内容" }),
  ).toBeVisible();
  await page.goto("/my-content");
  await page.getByRole("button", { name: /阅读记录/ }).click();
  await expect(page.getByText("继续第 3 节")).toBeVisible();
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
