import { expect, test, type Page } from "@playwright/test";
import { mockMemberSession } from "./member-session";
import { fulfillResult } from "./api-result";

const draft = {
  id: "41",
  categoryId: "1",
  slug: "old-address",
  title: "我的 AI 实践",
  summary: "记录完整实践过程",
  publicationType: "ARTICLE",
  accessType: "FREE",
  creditPrice: 0,
  status: "DRAFT",
  deliveryStatus: "AVAILABLE",
  fullMarkdown: "原来的正文",
  previewMarkdown: "公开预览",
  requirements: "",
  deliverables: "",
  version: "1.0",
  coverFileId: null,
  featured: false,
  publishedAt: null,
  createdAt: "2026-10-01T10:00:00Z",
  updatedAt: "2026-10-01T10:00:00Z",
};
async function editor(page: Page, fullMarkdown = draft.fullMarkdown) {
  await mockMemberSession(page, "ADMIN");
  const saved: Record<string, unknown>[] = [];
  await page.route("**/api/v1/admin/**", async (route) => {
    if (route.request().url().endsWith("/categories"))
      return fulfillResult(route, {
        json: [{ id: "1", slug: "coding", name: "AI 编程" }],
      });
    if (route.request().method() === "PUT")
      saved.push(route.request().postDataJSON());
    return fulfillResult(route, { json: { ...draft, fullMarkdown } });
  });
  return saved;
}

test("article tables and task lists render safely in the reading preview", async ({
  page,
}) => {
  const saved = await editor(
    page,
    "## 检查表\n\n| 场景 | 结果 |\n| --- | --- |\n| 密码错误 | 保留邮箱并允许重试 |\n\n- [x] 正常路径\n- [ ] 失败重试\n\n<script>alert('unsafe')</script>",
  );
  await page.goto("/admin/publications/41");
  await page.getByRole("button", { name: "阅读预览", exact: true }).click();
  await expect(
    page.getByRole("cell", { name: "保留邮箱并允许重试" }),
  ).toBeVisible();
  await expect(page.locator(".prose input[type=checkbox]")).toHaveCount(2);
  await expect(
    page.locator(".prose input[type=checkbox]").first(),
  ).toBeDisabled();
  await expect(page.locator(".prose script")).toHaveCount(0);
  await page.getByRole("button", { name: "继续编辑", exact: true }).click();
  await page.getByRole("button", { name: "保存草稿", exact: true }).click();
  await expect.poll(() => saved.length).toBeGreaterThan(0);
  await expect(page.getByLabel("内容版本", { exact: true })).toHaveCount(0);
  expect(saved.at(-1)).not.toHaveProperty("version");
  const body = String(saved.at(-1)!.fullMarkdown);
  expect(body).toContain("保留邮箱并允许重试");
  expect(body).toMatch(/\|\s*场景\s*\|/);
  expect(body).toContain("- [x] 正常路径");
  expect(body).toContain("- [ ] 失败重试");
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});

// 用原生 ProgressEvent 精确控制慢速上传阶段；实际 XHR 网络上传另由素材交互回归覆盖。
async function controlledUploads(page: Page) {
  await page.addInitScript(() => {
    const pending: XMLHttpRequest[] = [];
    Object.assign(window, { __mediaUploads: pending });
    const NativeXHR = window.XMLHttpRequest;
    window.XMLHttpRequest = class extends NativeXHR {
      private media = false;
      override open(method: string, url: string | URL, async = true) {
        this.media = String(url).startsWith("/api/v1/admin/media?");
        super.open(method, url, async);
      }
      override send(body?: Document | XMLHttpRequestBodyInit | null) {
        if (this.media) pending.push(this);
        else super.send(body);
      }
      override abort() {
        if (this.media) this.dispatchEvent(new ProgressEvent("abort"));
        else super.abort();
      }
    };
  });
}
async function uploadEvent(
  page: Page,
  kind: "progress" | "processing" | "failure" | "success",
  loaded = 0,
) {
  await page.evaluate(
    ({ kind, loaded }) => {
      const xhr = (
        window as unknown as { __mediaUploads: XMLHttpRequest[] }
      ).__mediaUploads.at(-1)!;
      if (kind === "progress")
        xhr.upload.dispatchEvent(
          new ProgressEvent("progress", {
            lengthComputable: true,
            loaded,
            total: 100,
          }),
        );
      else if (kind === "processing")
        xhr.upload.dispatchEvent(new ProgressEvent("load"));
      else {
        Object.defineProperty(xhr, "status", {
          configurable: true,
          value: kind === "success" ? 200 : 503,
        });
        Object.defineProperty(xhr, "responseText", {
          configurable: true,
          value: JSON.stringify({
            code: kind === "success" ? "SUCCESS" : "SERVICE_UNAVAILABLE",
            msg: "存储暂时不可用",
            traceId: "upload-test",
            data:
              kind === "success"
                ? {
                    id: "00000000-0000-4000-8000-000000000001",
                    filename: "demo.mp4",
                  }
                : null,
          }),
        });
        xhr.dispatchEvent(new ProgressEvent("load"));
      }
    },
    { kind, loaded },
  );
}

test("editor uses generated ID links and explains content form versus topic", async ({
  page,
}) => {
  const saved = await editor(page);
  await page.goto("/admin/publications/41");
  await expect(page.getByLabel("页面地址", { exact: true })).toHaveCount(0);
  await expect(
    page.getByText("/publications/41", { exact: true }),
  ).toBeVisible();
  await expect(page.getByLabel("内容形式", { exact: true })).toHaveValue(
    "ARTICLE",
  );
  await expect(page.getByLabel("所属主题", { exact: true })).toHaveValue("1");
  await page
    .getByRole("textbox", { name: "标题", exact: true })
    .fill("修改后的标题");
  await page.getByRole("button", { name: "保存草稿", exact: true }).click();
  await expect.poll(() => saved.length).toBe(1);
  expect(saved[0]).not.toHaveProperty("slug");
});

test("upload shows actual progress, processing and failure; retry inserts the stable media ID", async ({
  page,
}, info) => {
  await controlledUploads(page);
  await editor(page);
  await page.goto("/admin/publications/41");
  await page.getByRole("button", { name: "插入视频", exact: true }).click();
  const input = page.getByLabel("完整正文素材上传", { exact: true });
  await input.setInputFiles({
    name: "demo.mp4",
    mimeType: "video/mp4",
    buffer: Buffer.from("video-bytes"),
  });
  await expect(
    page.getByRole("progressbar", { name: "文件传输进度" }),
  ).toBeVisible();
  await uploadEvent(page, "progress", 25);
  await expect(page.getByRole("progressbar")).toHaveAttribute("value", "25");
  await page.screenshot({
    path: info.outputPath("upload-progress.png"),
    fullPage: true,
  });
  await uploadEvent(page, "progress", 100);
  await uploadEvent(page, "processing");
  await expect(
    page.getByText("文件已传到服务器，等待 OSS 上传…"),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "发布", exact: true }),
  ).toBeDisabled();
  await page.route("**/api/v1/admin/media/uploads/*", (route) =>
    fulfillResult(route, {
      json: { phase: "OSS", loaded: 5242880, total: 20971520 },
    }),
  );
  await expect(page.getByText("正在上传到 OSS 25%")).toBeVisible();
  await expect(page.getByRole("progressbar")).toHaveAttribute("value", "25");
  await page.unroute("**/api/v1/admin/media/uploads/*");
  await page.route("**/api/v1/admin/media/uploads/*", (route) =>
    fulfillResult(route, {
      json: { phase: "FINALIZING", loaded: 20971520, total: 20971520 },
    }),
  );
  await expect(
    page.getByText("OSS 已接收全部字节，正在完成存储…"),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "发布", exact: true }),
  ).toBeDisabled();
  await uploadEvent(page, "failure");
  await expect(page.getByText("存储暂时不可用")).toBeVisible();
  await expect(
    page.getByRole("textbox", { name: "完整正文", exact: true }),
  ).toContainText("原来的正文");
  await page.getByRole("button", { name: "插入视频", exact: true }).click();
  await input.setInputFiles({
    name: "demo.mp4",
    mimeType: "video/mp4",
    buffer: Buffer.from("video-bytes"),
  });
  await uploadEvent(page, "success");
  await expect(page.getByRole("progressbar")).toHaveCount(0);
  await page.getByRole("button", { name: "切换 Markdown 源码" }).click();
  await expect(
    page.getByRole("textbox", { name: "完整正文 Markdown 源码" }),
  ).toHaveValue(/media:00000000-0000-4000-8000-000000000001/);
});

test("cancelling an attachment upload preserves the article and enables another upload", async ({
  page,
}) => {
  await controlledUploads(page);
  await editor(page);
  await page.goto("/admin/publications/41");
  await page.getByRole("button", { name: "插入附件", exact: true }).click();
  await page.getByLabel("完整正文素材上传", { exact: true }).setInputFiles({
    name: "notes.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from("file-bytes"),
  });
  await page.getByRole("button", { name: "取消上传", exact: true }).click();
  await expect(page.getByText("已取消上传")).toBeVisible();
  await expect(
    page.getByRole("textbox", { name: "完整正文", exact: true }),
  ).toContainText("原来的正文");
  await expect(
    page.getByRole("button", { name: "插入附件", exact: true }),
  ).toBeEnabled();
});

test("navigation keeps the header, renders a real loading skeleton, then reveals the editor", async ({
  page,
}, info) => {
  await editor(page);
  let release: () => void = () => {};
  const gate = new Promise<void>((resolve) => {
    release = resolve;
  });
  await page.route("**/api/v1/admin/publications/41", async (route) => {
    await gate;
    await fulfillResult(route, { json: draft });
  });
  await page.goto("/admin/publications/41");
  await expect(
    page.getByRole("status", { name: "正在准备写作空间" }),
  ).toBeVisible();
  await expect(page.locator(".admin-header")).toBeVisible();
  await page.screenshot({
    path: info.outputPath("writer-skeleton.png"),
    fullPage: true,
  });
  release();
  await expect(
    page.getByRole("textbox", { name: "标题", exact: true }),
  ).toHaveValue(draft.title);
  await expect(
    page.getByRole("status", { name: "正在准备写作空间" }),
  ).toHaveCount(0);
  await expect(page.locator(".page-motion-root")).toHaveCSS("opacity", "1");
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page
    .getByRole("link", { name: "内容管理", exact: true })
    .last()
    .click();
  await expect(page.locator(".page-motion-root")).toHaveCSS("opacity", "1");
});

test("writer focus preserves the draft and outline jumps to the active document", async ({
  page,
}, info) => {
  await editor(
    page,
    "## 实践步骤\n\n" +
      "记录验证过程。\n\n".repeat(35) +
      "## 复盘总结\n\n保留经验",
  );
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.goto("/admin/publications/41");
  await page.getByRole("button", { name: "切换发布设置" }).click();
  await expect(page.locator(".writer-settings")).toHaveCount(0);
  await expect(page.getByLabel("标题", { exact: true })).toHaveValue(
    "我的 AI 实践",
  );
  await page.getByRole("button", { name: "切换发布设置" }).click();
  await page
    .locator(".writer-outline")
    .getByRole("button", { name: "02 复盘总结" })
    .click();
  await expect(page.locator(".tiptap h2").last()).toBeInViewport();
  await page.getByRole("button", { name: "阅读预览", exact: true }).click();
  await page
    .locator(".writer-outline")
    .getByRole("button", { name: "01 实践步骤" })
    .click();
  await expect(
    page.locator(".writer-reading-preview h2").first(),
  ).toBeInViewport();
  await page.getByRole("button", { name: "继续编辑", exact: true }).click();
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.screenshot({
    path: info.outputPath("writer-refinement.png"),
    fullPage: false,
  });
});

test("settings animate out before unmounting and return without losing input", async ({
  page,
}) => {
  await editor(page);
  await page.emulateMedia({ reducedMotion: "no-preference" });
  await page.goto("/admin/publications/41");
  const toggle = page.getByRole("button", { name: "切换发布设置" });
  await page.getByLabel("内容摘要", { exact: true }).fill("保留写作中的摘要");
  await toggle.click();
  await expect(toggle).toBeDisabled();
  await expect(page.locator(".writer-settings")).toHaveCount(0);
  await expect(toggle).toBeEnabled();
  await expect(page.locator(".writer-canvas")).not.toHaveAttribute(
    "style",
    /width:/,
  );
  await toggle.click();
  await expect(toggle).toBeEnabled();
  await expect(page.locator(".writer-settings")).toBeVisible();
  await expect(page.getByLabel("内容摘要", { exact: true })).toHaveValue(
    "保留写作中的摘要",
  );
  await expect(page.locator(".writer-settings")).not.toHaveAttribute(
    "style",
    /opacity:/,
  );
});

test("editor and preview use the cover URL supplied by detail", async ({
  page,
}) => {
  await editor(page);
  let signRequests = 0;
  await page.route("**/api/v1/admin/publications/41", (route) =>
    fulfillResult(route, {
      json: {
        ...draft,
        coverFileId: "cover-41",
        cover: {
          url: "https://images.example.test/cover.png?signature=test",
          expiresAt: "2099-01-01T00:00:00Z",
        },
      },
    }),
  );
  await page.route("https://images.example.test/**", (route) =>
    route.fulfill({
      contentType: "image/svg+xml",
      body: '<svg xmlns="http://www.w3.org/2000/svg" width="100" height="60"><rect width="100" height="60" fill="blue"/></svg>',
    }),
  );
  await page.route("**/api/v1/admin/media/*/url", (route) => {
    signRequests++;
    return fulfillResult(route, {
      json: { url: "https://images.example.test/cover.png" },
    });
  });
  await page.goto("/admin/publications/41");
  await expect(page.locator(".writer-cover img")).toHaveAttribute(
    "src",
    /signature=test/,
  );
  await page.getByRole("button", { name: "阅读预览", exact: true }).click();
  await expect(page.locator(".writer-reading-preview img")).toHaveAttribute(
    "src",
    /signature=test/,
  );
  expect(signRequests).toBe(0);
});

test("opening and switching editor views do not mark untouched Markdown as dirty", async ({
  page,
}) => {
  const saved = await editor(page, "## 标题\n\n* 第一项\n* 第二项\n\n正文\n");
  await page.goto("/admin/publications/41");
  await expect(
    page.getByRole("textbox", { name: "完整正文", exact: true }),
  ).toBeVisible();
  await expect(page.locator(".writer-save-status")).toContainText(
    "所有修改已保存",
  );
  await page.getByRole("button", { name: "阅读预览", exact: true }).click();
  await page.getByRole("button", { name: "继续编辑", exact: true }).click();
  await expect(
    page.getByRole("tab", { name: "公开预览", exact: true }),
  ).toHaveCount(0);
  await expect(page.locator(".writer-save-status")).toContainText(
    "所有修改已保存",
  );
  await page.locator(".writer-back").click();
  await expect(page).toHaveURL(/\/admin\/publications$/);
  await expect(page.getByRole("dialog")).toHaveCount(0);
  expect(saved).toHaveLength(0);
});

test("real edits still show the leave confirmation", async ({ page }) => {
  await editor(page);
  await page.goto("/admin/publications/41");
  await page
    .getByRole("textbox", { name: "完整正文", exact: true })
    .fill("真正修改了正文");
  await page.locator(".writer-back").click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "保留这次创作" }),
  ).toBeVisible();
});

test("cursor selection and source focus stay clean; undo restores original Markdown", async ({
  page,
}) => {
  await editor(page, "## 标题\n\n* 第一项\n* 第二项\n\n正文\n");
  await page.goto("/admin/publications/41");
  const body = page.getByRole("textbox", { name: "完整正文", exact: true });
  await body.click();
  await page.keyboard.press("ArrowLeft");
  await page.keyboard.press("Shift+ArrowRight");
  await expect(page.locator(".writer-save-status")).toContainText(
    "所有修改已保存",
  );
  await page.getByRole("button", { name: "切换 Markdown 源码" }).click();
  await page.getByRole("textbox", { name: "完整正文 Markdown 源码" }).click();
  await page.keyboard.press("ArrowRight");
  await expect(page.locator(".writer-save-status")).toContainText(
    "所有修改已保存",
  );
  await page.getByRole("button", { name: "切换 Markdown 源码" }).click();
  await body.click();
  await page.keyboard.type("changed");
  await expect(page.locator(".writer-save-status")).toContainText(
    "有未保存的修改",
  );
  await page.keyboard.press("Control+z");
  await expect(page.locator(".writer-save-status")).toContainText(
    "所有修改已保存",
  );
  await page.locator(".writer-back").click();
  await expect(page).toHaveURL(/\/admin\/publications$/);
});

test("paid trial boundary survives source and preview without a separate preview payload", async ({
  page,
}) => {
  const saved = await editor(
    page,
    "## 免费章节\n\n试读文字\n\n## 完整章节\n\n付费文字",
  );
  await page.goto("/admin/publications/41");
  await expect(
    page.getByRole("button", { name: "试读到这里", exact: true }),
  ).toHaveCount(0);
  await page.getByRole("button", { name: "积分解锁", exact: true }).click();
  await page.getByLabel("积分价格", { exact: true }).fill("50");
  const body = page.getByRole("textbox", { name: "完整正文", exact: true });
  await body.click();
  await page.keyboard.press("Control+Home");
  await page.getByRole("button", { name: "试读到这里", exact: true }).click();
  await expect(page.locator("[data-trial-boundary]")).toHaveCount(1);
  await expect(
    page.getByRole("button", { name: "试读到这里", exact: true }),
  ).toBeDisabled();
  await page.getByRole("button", { name: "切换 Markdown 源码" }).click();
  await expect(
    page.getByLabel("完整正文 Markdown 源码", { exact: true }),
  ).toHaveValue(/<!-- arieshub:paid -->/);
  await page.getByRole("button", { name: "切换 Markdown 源码" }).click();
  await page.getByRole("button", { name: "阅读预览", exact: true }).click();
  await page.getByRole("button", { name: "继续编辑", exact: true }).click();
  await expect(page.locator("[data-trial-boundary]")).toHaveCount(1);
  await page.getByRole("button", { name: "保存草稿", exact: true }).click();
  await expect.poll(() => saved.length).toBeGreaterThan(0);
  expect(saved.at(-1)).not.toHaveProperty("previewMarkdown");
  expect(String(saved.at(-1)?.fullMarkdown)).toContain(
    "<!-- arieshub:paid -->",
  );
});

test("legacy paid preview is preserved when adopting a single body", async ({
  page,
}, info) => {
  await editor(page);
  await page.route("**/api/v1/admin/publications/41", (route) =>
    fulfillResult(route, {
      json: {
        ...draft,
        accessType: "CREDIT",
        creditPrice: 50,
        previewMarkdown: "旧版免费介绍",
        fullMarkdown: "付费实践步骤",
      },
    }),
  );
  await page.goto("/admin/publications/41");
  await expect(page.locator(".writer-trial-hint")).toContainText(
    "旧版试读已合并",
  );
  const body = page.getByRole("textbox", { name: "完整正文", exact: true });
  await expect(body).toContainText("旧版免费介绍");
  await expect(body).toContainText("付费实践步骤");
  await expect(page.locator("[data-trial-boundary]")).toHaveCount(1);
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.screenshot({
    path: info.outputPath("trial-editor.png"),
    fullPage: true,
  });
  await page.getByRole("button", { name: "免费阅读", exact: true }).click();
  await expect(page.locator("[data-trial-boundary]")).toHaveCount(0);
  await expect(body).toContainText("付费实践步骤");
});
