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
async function editor(page: Page) {
  await mockMemberSession(page, "ADMIN");
  const saved: Record<string, unknown>[] = [];
  await page.route("**/api/v1/admin/**", async (route) => {
    if (route.request().url().endsWith("/categories"))
      return fulfillResult(route, {
        json: [{ id: "1", slug: "coding", name: "AI 编程" }],
      });
    if (route.request().method() === "PUT")
      saved.push(route.request().postDataJSON());
    return fulfillResult(route, { json: draft });
  });
  return saved;
}

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
