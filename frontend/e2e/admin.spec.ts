import { expect, test } from "@playwright/test";

test("administrator can browse cases and open the editor", async ({ page }) => {
  await page.route("**/api/v1/admin/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/categories")) {
      await route.fulfill({
        json: [{ id: "1", slug: "coding", name: "Codex 编程" }],
      });
      return;
    }
    if (path.endsWith("/cases/41")) {
      await route.fulfill({
        json: {
          id: "41",
          categoryId: "1",
          slug: "codex-workflow",
          title: "Codex 实战工作流",
          summary: "把重复任务整理成可复现的工作流。",
          accessType: "FREE",
          priceMinor: 0,
          currency: "CNY",
          status: "DRAFT",
          deliveryStatus: "AVAILABLE",
          isDemo: false,
          publishedAt: null,
          previewMarkdown: "公开预览",
          fullMarkdown: "完整正文",
          requirements: "准备 Codex",
          deliverables: "教程与源码",
          version: "1.0",
          createdAt: "2026-09-28T10:00:00Z",
          updatedAt: "2026-09-28T10:00:00Z",
        },
      });
      return;
    }
    await route.fulfill({
      json: [
        {
          id: "41",
          slug: "codex-workflow",
          title: "Codex 实战工作流",
          categoryName: "Codex 编程",
          accessType: "FREE",
          priceMinor: 0,
          status: "DRAFT",
          deliveryStatus: "AVAILABLE",
          publishedAt: null,
          updatedAt: "2026-09-28T10:00:00Z",
        },
      ],
    });
  });

  await page.goto("/admin");
  await expect(page.getByRole("heading", { name: "内容后台" })).toBeVisible();
  await expect(page.getByText("Codex 实战工作流")).toBeVisible();
  await page.getByRole("link", { name: "编辑" }).click();
  await expect(page.getByRole("heading", { name: "编辑案例" })).toBeVisible();
  await expect(page.getByLabel("标题")).toHaveValue("Codex 实战工作流");
  await expect(page.getByRole("button", { name: "发布" })).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});
