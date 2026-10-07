import { fulfillResult } from "./api-result";
import { expect, test } from "@playwright/test";
import { mockMemberSession } from "./member-session";

test("administrator can browse publications and open the editor", async ({
  page,
}, testInfo) => {
  await mockMemberSession(page, "ADMIN");
  await page.route("**/api/v1/admin/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/categories")) {
      await fulfillResult(route, {
        json: [{ id: "1", slug: "coding", name: "Codex 编程" }],
      });
      return;
    }
    if (path.endsWith("/publications/41")) {
      await fulfillResult(route, {
        json: {
          id: "41",
          categoryId: "1",
          slug: "codex-workflow",
          title: "Codex 实战工作流",
          summary: "把重复任务整理成可复现的工作流。",
          publicationType: "CASE_STUDY",
          accessType: "FREE",
          creditPrice: 0,
          status: "DRAFT",
          deliveryStatus: "AVAILABLE",
          publishedAt: null,
          previewMarkdown: "公开预览",
          fullMarkdown: "完整正文",
          version: "1.0",
          createdAt: "2026-09-28T10:00:00Z",
          updatedAt: "2026-09-28T10:00:00Z",
        },
      });
      return;
    }
    await fulfillResult(route, {
      json: [
        {
          id: "41",
          slug: "codex-workflow",
          title: "Codex 实战工作流",
          categoryName: "Codex 编程",
          publicationType: "CASE_STUDY",
          accessType: "FREE",
          creditPrice: 0,
          status: "DRAFT",
          deliveryStatus: "AVAILABLE",
          publishedAt: null,
          updatedAt: "2026-09-28T10:00:00Z",
        },
      ],
    });
  });

  await page.goto("/admin/publications");
  await expect(page.getByRole("heading", { name: "内容管理" })).toBeVisible();
  await expect(page.getByText("Codex 实战工作流")).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("admin-list.png"),
    fullPage: true,
  });
  await page.getByRole("link", { name: "编辑" }).click();
  await expect(page.getByRole("heading", { name: "编辑内容" })).toBeVisible();
  await expect(page.getByLabel("标题", { exact: true })).toHaveValue(
    "Codex 实战工作流",
  );
  await expect(
    page.getByRole("button", { name: "发布", exact: true }),
  ).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("admin-editor.png"),
    fullPage: true,
  });
  await page.getByRole("button", { name: "积分解锁", exact: true }).click();
  expect(
    await page.locator(".admin-editor").evaluate((form) => {
      const values = new FormData(form as HTMLFormElement);
      return [
        values.get("categoryId"),
        values.get("publicationType"),
        values.get("accessType"),
      ];
    }),
  ).toEqual(["1", "CASE_STUDY", "CREDIT"]);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});
