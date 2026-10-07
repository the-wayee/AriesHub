import { expect, test } from "@playwright/test";
import type {
  PublicationPage,
  PublicationContent,
} from "../src/lib/catalog-types";
import type { Result } from "../src/lib/api";

/** 浏览器到本机 Java 的只读联调；验证真实 ID 内容而非静态概念稿。 */
test("published ID details show the stored body and old slug addresses are rejected", async ({
  page,
  request,
}, info) => {
  const response = await request.get("/api/v1/publications?access=FREE");
  expect(response.ok()).toBe(true);
  const list = (await response.json()) as Result<PublicationPage>;
  expect(list.data.items.length).toBeGreaterThan(0);
  const article = list.data.items[0];
  const content = (await (
    await request.get(`/api/v1/publications/${article.id}/content`)
  ).json()) as Result<PublicationContent>;
  await page.goto(`/publications/${article.id}`);
  await expect(
    page.getByRole("heading", { name: article.title, exact: true }),
  ).toBeVisible();
  const firstLine = content.data.markdown
    .split("\n")
    .find((line) => line.trim())!
    .replace(/^#+\s*/, "");
  await expect(page.getByLabel("完整正文", { exact: true })).toContainText(
    firstLine,
  );
  await expect(page.getByText("界面预览 · 示例文章")).toHaveCount(0);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: info.outputPath("published-id.png"),
    fullPage: true,
  });
  await page.goto("/publications/free-case");
  await expect(
    page.getByRole("heading", { name: "这份内容暂时找不到了" }),
  ).toBeVisible();
});

test("paid ID detail exposes only the preview and unknown IDs stay unavailable", async ({
  page,
  request,
}, info) => {
  const response = await request.get("/api/v1/publications?access=CREDIT");
  const list = (await response.json()) as Result<PublicationPage>;
  expect(list.data.items.length).toBeGreaterThan(0);
  const article = list.data.items[0];
  const locked = await request.get(
    `/api/v1/publications/${article.id}/content`,
  );
  expect(locked.status()).toBe(403);
  await page.goto(`/publications/${article.id}`);
  await expect(page.getByLabel("免费试读", { exact: true })).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "解锁后，继续阅读完整内容" }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "积分解锁 · 暂未开放" }),
  ).toBeDisabled();
  const excerpt = page.locator(".publication-trial-excerpt");
  if (await excerpt.count()) {
    expect(
      await excerpt.evaluate((node) => getComputedStyle(node).maskImage),
    ).toContain("linear-gradient");
  }
  await expect(page.getByLabel("完整正文", { exact: true })).toHaveCount(0);
  await page
    .getByRole("heading", { name: "解锁后，继续阅读完整内容" })
    .scrollIntoViewIfNeeded();
  await page.screenshot({
    path: info.outputPath("paid-trial.png"),
    fullPage: true,
  });
  await page.goto("/publications/999999999999999999");
  await expect(
    page.getByRole("heading", { name: "这份内容暂时找不到了" }),
  ).toBeVisible();
});
