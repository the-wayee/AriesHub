import { test, expect } from "@playwright/test";
import { mockMemberSession } from "./member-session";
import { fulfillResult } from "./api-result";

test("admin creates categories and duplicate errors preserve the input", async ({
  page,
}) => {
  await mockMemberSession(page, "ADMIN");
  const categories = [
    {
      id: "1",
      slug: "coding",
      name: "AI 编程",
      color: "#4967A9",
      sortOrder: 0,
    },
  ];
  await page.route("**/api/v1/admin/categories", (route) => {
    if (route.request().method() === "POST") {
      const body = route.request().postDataJSON();
      if (categories.some((category) => category.name === body.name))
        return fulfillResult(route, {
          status: 409,
          json: {
            code: "CATEGORY_NAME_CONFLICT",
            msg: "分类名称已存在，请使用其他名称",
          },
        });
      const category = {
        id: "3",
        slug: "category-test",
        name: body.name,
        color: body.color,
        sortOrder: body.sortOrder,
      };
      categories.push(category);
      return fulfillResult(route, { status: 201, json: category });
    }
    return fulfillResult(route, { json: categories });
  });
  await page.goto("/admin/categories");
  await page.getByLabel("新分类名称").fill("AI 视频创作");
  await page.getByLabel("分类显示排序").fill("5");
  await page.getByLabel("分类颜色", { exact: true }).fill("#cc6633");
  await page.getByRole("button", { name: "创建分类", exact: true }).click();
  await expect(page.locator(".admin-category-list")).toContainText(
    "AI 视频创作",
  );
  await expect(
    page.getByRole("status").filter({ hasText: "已创建" }),
  ).toBeVisible();
  expect(categories.at(-1)?.color).toBe("#cc6633");
  await expect(page.getByRole("link", { name: "编写内容 →" })).toHaveCount(0);
  await page.getByLabel("新分类名称").fill("AI 视频创作");
  await page.getByRole("button", { name: "创建分类", exact: true }).click();
  await expect(
    page.locator(".admin-category-create").getByRole("alert"),
  ).toContainText("分类名称已存在");
  await expect(page.getByLabel("新分类名称")).toHaveValue("AI 视频创作");
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});

test("writer creates and selects a category without losing the draft", async ({
  page,
}) => {
  await mockMemberSession(page, "ADMIN");
  const saved: Record<string, unknown>[] = [];
  await page.route("**/api/v1/admin/**", (route) => {
    if (route.request().url().endsWith("/categories"))
      return fulfillResult(route, {
        json:
          route.request().method() === "POST"
            ? { id: "3", slug: "category-video", name: "AI 视频创作" }
            : [
                {
                  id: "1",
                  slug: "coding",
                  name: "AI 编程",
                  color: "#4967A9",
                  sortOrder: 0,
                },
              ],
      });
    const body = route.request().postDataJSON();
    if (body) saved.push(body);
    return fulfillResult(route, {
      json: { ...(body ?? saved.at(-1)), id: "41", status: "DRAFT" },
    });
  });
  await page.goto("/admin/publications/new");
  await page
    .getByRole("textbox", { name: "标题", exact: true })
    .fill("视频实践记录");
  await page
    .getByRole("textbox", { name: "完整正文", exact: true })
    .fill("需要保留的完整正文");
  await page.getByLabel("内容摘要", { exact: true }).fill("视频创作实践摘要");
  await page.locator(".writer-category-create summary").click();
  await page.getByLabel("新分类名称").fill("AI 视频创作");
  await page.getByRole("button", { name: "创建分类", exact: true }).click();
  await expect(page.getByLabel("所属主题", { exact: true })).toHaveValue("3");
  await expect(
    page.getByRole("textbox", { name: "标题", exact: true }),
  ).toHaveValue("视频实践记录");
  await expect(
    page.getByRole("textbox", { name: "完整正文", exact: true }),
  ).toContainText("需要保留的完整正文");
  await page.getByRole("button", { name: "保存草稿", exact: true }).click();
  await expect
    .poll(() =>
      saved.some((body) => body.categoryId === "3" || body.categoryId === 3),
    )
    .toBe(true);
});

// 验证指针拖动真正调用排序接口，失败恢复旧顺序；键盘使用同一条保存路径。
test("category drag saves order and failed reorder rolls back", async ({
  page,
}) => {
  await mockMemberSession(page, "ADMIN");
  let categories = [
    {
      id: "1",
      slug: "coding",
      name: "AI 编程",
      color: "#4967A9",
      sortOrder: 0,
    },
    { id: "2", slug: "video", name: "AI 视频", color: "#CC6633", sortOrder: 1 },
    {
      id: "3",
      slug: "design",
      name: "AI 设计",
      color: "#337766",
      sortOrder: 2,
    },
  ];
  let fail = false;
  const submitted: string[][] = [];
  await page.route("**/api/v1/admin/categories", (route) =>
    fulfillResult(route, { json: categories }),
  );
  await page.route("**/api/v1/admin/categories/order", (route) => {
    const ids: string[] = route.request().postDataJSON().categoryIds;
    submitted.push(ids);
    if (fail)
      return fulfillResult(route, {
        status: 409,
        json: { code: "CATEGORY_ORDER_CHANGED", msg: "分类列表已变化" },
      });
    categories = ids.map((id, sortOrder) => ({
      ...categories.find((category) => category.id === id)!,
      sortOrder,
    }));
    return fulfillResult(route, { json: categories });
  });
  await page.goto("/admin/categories");
  const handle = page.getByRole("button", { name: "拖动排序 AI 编程" });
  await page
    .locator(".admin-category-list")
    .evaluate((list) =>
      list.scrollIntoView({ block: "center", behavior: "instant" }),
    );
  const from = await handle.boundingBox();
  const target = await page.locator('[data-category-id="3"]').boundingBox();
  expect(from).not.toBeNull();
  expect(target).not.toBeNull();
  await page.mouse.move(from!.x + from!.width / 2, from!.y + from!.height / 2);
  await page.mouse.down();
  await page.mouse.move(target!.x + 30, target!.y + target!.height * 0.8, {
    steps: 8,
  });
  await expect(page.locator(".category-drag-overlay")).toContainText("AI 编程");
  await expect(page.locator(".category-insertion-slot")).toContainText(
    "第 3 位",
  );
  await expect
    .poll(() =>
      page
        .locator(".category-sort-stage")
        .evaluate(
          (stage) =>
            stage
              .getAnimations({ subtree: true })
              .filter((animation) => animation.playState === "running").length,
        ),
    )
    .toBe(0);
  await page.screenshot({ path: test.info().outputPath("category-drag.png") });
  await page.mouse.up();
  await expect(page.locator(".category-drag-overlay")).toHaveCount(0);
  await expect.poll(() => submitted[0]).toEqual(["2", "3", "1"]);
  await expect(page.locator(".admin-category-list li").first()).toContainText(
    "AI 视频",
  );
  await page.reload();
  await expect(page.locator(".admin-category-list li").first()).toContainText(
    "AI 视频",
  );
  const cancelledDrag = page.getByRole("button", { name: "拖动排序 AI 视频" });
  await cancelledDrag.scrollIntoViewIfNeeded();
  const cancelPoint = await cancelledDrag.boundingBox();
  await page.mouse.move(cancelPoint!.x + 8, cancelPoint!.y + 8);
  await page.mouse.down();
  await expect(page.locator(".category-drag-overlay")).toBeVisible();
  await page.keyboard.press("Escape");
  await page.mouse.up();
  await expect(page.locator(".category-drag-overlay")).toHaveCount(0);
  expect(submitted).toHaveLength(1);
  fail = true;
  await page
    .getByRole("button", { name: "拖动排序 AI 视频" })
    .press("ArrowDown");
  await expect(
    page.locator(".admin-category-panel").getByRole("alert"),
  ).toContainText("未保存本次排序");
  await expect(page.locator(".admin-category-list li").first()).toContainText(
    "AI 视频",
  );
});

test("category update and deletion preserve state on reference conflicts", async ({
  page,
}) => {
  await mockMemberSession(page, "ADMIN");
  let category = {
    id: "1",
    slug: "coding",
    name: "AI 编程",
    color: "#4967a9",
    sortOrder: 0,
  };
  let exists = true;
  let inUse = true;
  await page.route("**/api/v1/admin/categories", (route) =>
    fulfillResult(route, { json: exists ? [category] : [] }),
  );
  await page.route("**/api/v1/admin/categories/1", (route) => {
    if (route.request().method() === "PUT") {
      category = { ...category, ...route.request().postDataJSON() };
      return fulfillResult(route, { json: category });
    }
    if (inUse)
      return fulfillResult(route, {
        status: 409,
        json: { code: "CATEGORY_IN_USE", msg: "该分类仍有关联内容，请先迁移" },
      });
    exists = false;
    return fulfillResult(route, { json: null });
  });
  await page.goto("/admin/categories");
  await page.getByRole("button", { name: "编辑 AI 编程" }).click();
  await page.getByLabel("编辑分类名称").fill("创意编程");
  await page.getByLabel("编辑分类颜色").fill("#cc6633");
  await page.getByRole("button", { name: "保存修改" }).click();
  await expect(page.locator(".admin-category-list")).toContainText("创意编程");
  await expect(page.locator(".category-color-dot")).toHaveCSS(
    "background-color",
    "rgb(204, 102, 51)",
  );
  await page.reload();
  await page.getByRole("button", { name: "删除 创意编程" }).click();
  await page.getByRole("button", { name: "确认删除" }).click();
  await expect(
    page.getByRole("region", { name: "删除分类确认" }).getByRole("alert"),
  ).toContainText("先迁移");
  await expect(page.locator(".admin-category-list")).toContainText("创意编程");
  inUse = false;
  await page.getByRole("button", { name: "确认删除" }).click();
  await expect(page.locator(".admin-category-list")).toHaveCount(0);
  await page.reload();
  await expect(page.locator(".admin-category-list")).toHaveCount(0);
});

for (const reduced of [false, true]) {
  test(`arrow sorting animates movement and rollback (reduced=${reduced})`, async ({
    page,
  }) => {
    await page.emulateMedia({
      reducedMotion: reduced ? "reduce" : "no-preference",
    });
    await mockMemberSession(page, "ADMIN");
    const categories = [
      {
        id: "1",
        slug: "coding",
        name: "AI 编程",
        color: "#4967a9",
        sortOrder: 0,
      },
      {
        id: "2",
        slug: "video",
        name: "AI 视频",
        color: "#cc6633",
        sortOrder: 1,
      },
    ];
    let rejectOrder: (() => Promise<void>) | undefined;
    await page.route("**/api/v1/admin/categories", (route) =>
      fulfillResult(route, { json: categories }),
    );
    await page.route("**/api/v1/admin/categories/order", (route) => {
      rejectOrder = async () => {
        await fulfillResult(route, {
          status: 409,
          json: { code: "CATEGORY_ORDER_CHANGED", msg: "列表发生变化" },
        });
      };
    });
    await page.goto("/admin/categories");
    await page.getByRole("button", { name: "下移 AI 编程" }).waitFor();
    // 记录逐帧视觉偏移，验证真实过渡，而不只是最终 DOM 排序。
    const offsets = await page.evaluate(async () => {
      (
        document.querySelector(
          '[aria-label="下移 AI 编程"]',
        ) as HTMLButtonElement
      ).click();
      const samples: number[] = [];
      for (let frame = 0; frame < 30; frame++) {
        await new Promise(requestAnimationFrame);
        const row = document.querySelector<HTMLElement>(
          '[data-category-id="1"]',
        )!;
        samples.push(
          Number.parseFloat(
            getComputedStyle(row).translate.split(" ")[1] ?? "0",
          ) || 0,
        );
      }
      return samples;
    });
    expect(offsets.some((offset) => Math.abs(offset) > 1)).toBe(!reduced);
    expect(Math.abs(offsets.at(-1)!)).toBeLessThan(1);
    await expect(page.locator(".admin-category-list li").first()).toContainText(
      "AI 视频",
    );
    await expect.poll(() => Boolean(rejectOrder)).toBe(true);
    await rejectOrder!();
    await expect(page.locator(".admin-category-list li").first()).toContainText(
      "AI 编程",
    );
    await expect
      .poll(() =>
        page
          .locator('[data-category-id="1"]')
          .evaluate((row) => getComputedStyle(row).translate),
      )
      .toBe("none");
  });
}
