import { expect, test, type Page } from "@playwright/test";
import { fulfillResult } from "./api-result";
import { mockMemberSession } from "./member-session";

const draft = {
  id: "41",
  categoryId: "1",
  slug: "ai-workflow",
  title: "从想法到作品：AI 工作流",
  summary: "把实践过程写下来，让更多人能够复现。",
  publicationType: "ARTICLE",
  accessType: "FREE",
  creditPrice: 0,
  status: "DRAFT",
  deliveryStatus: "AVAILABLE",
  publishedAt: null,
  previewMarkdown: "公开介绍",
  fullMarkdown: "先找到一个值得解决的问题。",
  requirements: "",
  deliverables: "",
  version: "1.0",
  coverFileId: null as string | null,
  featured: false,
  createdAt: "2026-10-01T10:00:00Z",
  updatedAt: "2026-10-01T10:00:00Z",
};
async function editorApi(page: Page, failSave = false) {
  await mockMemberSession(page, "ADMIN");
  let current = { ...draft };
  const saved: Record<string, unknown>[] = [];
  await page.route("**/api/v1/admin/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path.endsWith("/categories"))
      return fulfillResult(route, {
        json: [{ id: "1", slug: "coding", name: "AI 编程" }],
      });
    if (request.method() === "PUT") {
      saved.push(request.postDataJSON());
      if (failSave)
        return fulfillResult(route, {
          status: 503,
          json: { code: "SERVICE_UNAVAILABLE", msg: "保存暂时不可用" },
        });
      current = { ...current, ...request.postDataJSON() };
    }
    if (path.endsWith("/publish"))
      current = { ...current, status: "PUBLISHED" };
    return fulfillResult(route, { json: current });
  });
  return saved;
}
test("workbench shows real activity and responsive navigation", async ({
  page,
}, info) => {
  await mockMemberSession(page, "ADMIN");
  await page.route("**/api/v1/admin/operations/overview", (r) =>
    fulfillResult(r, {
      json: {
        summary: {
          members: 128,
          activeMembers: 43,
          newMembers: 12,
          published: 24,
          drafts: 6,
          paid: 8,
          comments: 86,
          unlocks: 32,
          creditsSpent: 1680,
          creditBalance: 3400,
        },
        days: Array.from({ length: 7 }, (_, i) => ({
          date: `2026-10-0${i + 1}`,
          registrations: i + 1,
          publications: i % 3,
          comments: i * 2,
        })),
      },
    }),
  );
  await page.goto("/admin");
  await expect(
    page.getByRole("heading", { name: "工作台", exact: true }),
  ).toBeVisible();
  await expect(page.getByText("128", { exact: true })).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "社区正在生长" }),
  ).toBeVisible();
  if (info.project.name === "mobile") {
    await page.getByRole("button", { name: "展开后台导航" }).click();
    await expect(
      page.getByRole("navigation", { name: "后台导航" }),
    ).toBeVisible();
    await page.keyboard.press("Escape");
    await expect(
      page.getByRole("navigation", { name: "后台导航" }),
    ).not.toBeVisible();
  }
  await expect(page.locator("html")).toHaveJSProperty(
    "scrollWidth",
    await page.evaluate(() => innerWidth),
  );
  await page.screenshot({
    path: info.outputPath("workbench.png"),
    fullPage: true,
  });
});
test("writer formats, prices, previews, saves and publishes content", async ({
  page,
}, info) => {
  const saved = await editorApi(page);
  await page.goto("/admin/publications/41");
  await page
    .getByRole("textbox", { name: "完整正文", exact: true })
    .fill("我的 AI 实践记录");
  await page
    .getByRole("textbox", { name: "完整正文", exact: true })
    .press("Control+Home");
  await page.getByRole("button", { name: "二级标题", exact: true }).click();
  await page.getByRole("button", { name: "积分解锁", exact: true }).click();
  await page.getByLabel("积分价格", { exact: true }).fill("99");
  await page.getByRole("button", { name: "保存草稿", exact: true }).click();
  await expect.poll(() => saved.length).toBe(1);
  expect(saved[0]).toMatchObject({
    accessType: "CREDIT",
    creditPrice: 99,
  });
  expect(String(saved[0].fullMarkdown).trim()).toBe("## 我的 AI 实践记录");
  await page.getByRole("button", { name: "阅读预览", exact: true }).click();
  await expect(page.locator(".writer-reading-preview h2")).toHaveText(
    "我的 AI 实践记录",
  );
  await page.screenshot({
    path: info.outputPath("writer-preview.png"),
    fullPage: true,
  });
  await page.getByRole("button", { name: "继续编辑", exact: true }).click();
  await page.screenshot({
    path: info.outputPath("writer.png"),
    fullPage: true,
  });
  await page.getByRole("button", { name: "发布", exact: true }).click();
  await expect(
    page.getByRole("button", { name: "下架", exact: true }),
  ).toBeVisible();
  await expect(page.locator("html")).toHaveJSProperty(
    "scrollWidth",
    await page.evaluate(() => innerWidth),
  );
});
test("autosave failure keeps writing and warns before leaving", async ({
  page,
}) => {
  const saved = await editorApi(page, true);
  await page.goto("/admin/publications/41");
  await page
    .getByRole("textbox", { name: "完整正文", exact: true })
    .fill("这些修改必须保留");
  await expect.poll(() => saved.length, { timeout: 9000 }).toBe(1);
  await expect(
    page.getByText("自动保存失败：保存暂时不可用", { exact: false }),
  ).toBeVisible();
  await expect(
    page.getByRole("textbox", { name: "完整正文", exact: true }),
  ).toHaveText("这些修改必须保留");
  await page.locator(".writer-back").click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await page.getByRole("button", { name: "继续编辑", exact: true }).click();
  await expect(
    page.getByRole("textbox", { name: "完整正文", exact: true }),
  ).toHaveText("这些修改必须保留");
});
test("member status action uses accessible dialog and protected admin accounts", async ({
  page,
}, info) => {
  await mockMemberSession(page, "ADMIN");
  let status = "ACTIVE";
  await page.route("**/api/v1/admin/users**", async (r) => {
    const member = {
      id: "2",
      email: "member@example.com",
      nickname: "实践成员",
      role: "USER",
      status,
      emailVerified: true,
      creditBalance: 150,
      createdAt: "2026-10-01T10:00:00Z",
      lastLoginAt: null,
    };
    if (r.request().method() === "PUT") {
      status = r.request().postDataJSON().status;
      return fulfillResult(r, { json: { ...member, status } });
    }
    return fulfillResult(r, {
      json: {
        items: [
          { ...member, id: "1", nickname: "管理员", role: "ADMIN" },
          member,
        ],
        page: 1,
        size: 20,
        total: 2,
      },
    });
  });
  await page.goto("/admin/users");
  await expect(page.getByText("受保护账号")).toBeVisible();
  await page.getByRole("button", { name: "停用", exact: true }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await page.getByRole("button", { name: "确认", exact: true }).click();
  await expect(
    page.getByRole("button", { name: "恢复", exact: true }),
  ).toBeVisible();
  await page.screenshot({
    path: info.outputPath("members.png"),
    fullPage: true,
  });
  await expect(page.locator("html")).toHaveJSProperty(
    "scrollWidth",
    await page.evaluate(() => innerWidth),
  );
});

test("writer uploads cover and embeds image video and attachment without storing signed URLs", async ({
  page,
}, info) => {
  const saved = await editorApi(page);
  let uploads = 0;
  const kinds: string[] = [];
  const ids: string[] = [];
  await page.route("**/api/v1/admin/media?*", async (r) => {
    const body = r.request().postDataBuffer()!.toString();
    const kind = body.match(/name="kind"\r\n\r\n([A-Z]+)/)![1];
    kinds.push(kind);
    const id = `11111111-1111-4111-8111-${String(++uploads).padStart(12, "0")}`;
    ids.push(id);
    return fulfillResult(r, {
      status: 201,
      json: {
        id,
        kind,
        filename:
          kind === "ATTACHMENT"
            ? "资料.pdf"
            : kind === "VIDEO"
              ? "实践视频.mp4"
              : "实践图片.png",
      },
    });
  });
  await page.route("**/api/v1/admin/media/*/url", (r) =>
    fulfillResult(r, {
      json: {
        url: r.request().url().includes("000000000003")
          ? "/media/ai-film.mp4"
          : "/media/codex-workflow.svg",
        expiresAt: "2026-10-07T10:00:00Z",
      },
    }),
  );
  await page.goto("/admin/publications/41");
  await page.getByLabel("上传封面文件").setInputFiles({
    name: "cover.png",
    mimeType: "image/png",
    buffer: Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
  });
  await expect.poll(() => uploads).toBe(1);
  for (const [kind, name, mime] of [
    ["图片", "picture.png", "image/png"],
    ["视频", "video.mp4", "video/mp4"],
    ["附件", "notes.pdf", "application/pdf"],
  ]) {
    await page
      .getByRole("button", { name: `插入${kind}`, exact: true })
      .click();
    await page.getByLabel("完整正文素材上传").setInputFiles({
      name,
      mimeType: mime,
      buffer: Buffer.from("test content"),
    });
    await expect(page.getByText("正在上传素材，请稍候…")).not.toBeVisible();
  }
  await expect.poll(() => uploads).toBe(4);
  await page.getByRole("button", { name: "保存草稿", exact: true }).click();
  await expect.poll(() => saved.length).toBeGreaterThan(0);
  expect(kinds).toEqual(["COVER", "IMAGE", "VIDEO", "ATTACHMENT"]);
  const body = String(saved.at(-1)!.fullMarkdown);
  ids.slice(1).forEach((id) => expect(body).toContain(`media:${id}`));
  expect(saved.at(-1)!.coverFileId).toBe(ids[0]);
  expect(body).not.toContain("expiresAt");
  await page.getByRole("button", { name: "阅读预览", exact: true }).click();
  await expect(page.locator(".writer-reading-preview video")).toBeVisible();
  await expect(page.getByRole("link", { name: /资料.pdf/ })).toBeVisible();
  await page.screenshot({
    path: info.outputPath("writer-media.png"),
    fullPage: true,
  });
});
