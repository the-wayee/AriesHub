import { expect, test, type Page } from "@playwright/test";
import { fulfillResult } from "./api-result";
import { mockMemberSession } from "./member-session";

const token = "a".repeat(43);
async function mockShare(page: Page, paid = false, canRead = !paid) {
  await page.route("**/api/v1/users/me", (route) =>
    fulfillResult(route, {
      status: 401,
      json: { code: "UNAUTHENTICATED", msg: "请先登录" },
    }),
  );
  await page.route(`**/api/v1/shares/${token}`, (route) =>
    fulfillResult(route, {
      json: {
        sharedBy: "林舟",
        sharedAvatarUrl: "/concepts/knowledge-color.webp",
        canRead,
        cover: {
          url: "/concepts/automation-color.webp",
          expiresAt: "2099-01-01T00:00:00Z",
        },
        detail: {
          publication: {
            id: "11",

            title: "把一个想法，变成作品",
            summary: "从一段提示词开始，记录一次完整的创作。",
            categoryName: "AI 创作",
            accessType: paid ? "CREDIT" : "FREE",
            creditPrice: paid ? 160 : 0,
            coverFileId: "f".repeat(36),
          },
          preview: { previewMarkdown: "## 从这里开始\n这是分享的试读内容。" },
        },
      },
    }),
  );
  await page.route("**/api/v1/discussions/comments?*", (route) =>
    fulfillResult(route, {
      json: {
        items: [
          {
            comment: {
              id: "301",
              authorId: "9002",
              authorName: "程雨",
              authorAvatarUrl: "/concepts/presentation-color.webp",
              body: "这篇文章很有帮助。",
              likeCount: 0,
              likedByMe: false,
              canDelete: false,
              deleted: false,
              depth: 0,
              createdAt: "2026-10-07T08:00:00Z",
            },
            previewReplies: [],
            replyCount: 0,
          },
        ],
        page: 1,
        size: 10,
        total: 1,
        totalPages: 1,
      },
    }),
  );
  await page.route(`**/api/v1/shares/${token}/attachments`, (route) =>
    fulfillResult(route, { json: [] }),
  );
  await page.route("**/api/v1/publications/11/interaction", (route) =>
    fulfillResult(route, {
      json: { likeCount: 0, bookmarkCount: 0, shareCount: 1, viewCount: 8 },
    }),
  );
}

test("free share reads just its article without a member session", async ({
  page,
}, info) => {
  await mockShare(page);
  const fileId = "11111111-1111-4111-8111-111111111111";
  let imageRequests = 0;
  await page.route(`**/api/v1/shares/${token}/media/${fileId}/url`, (route) => {
    imageRequests++;
    return fulfillResult(route, {
      json: { url: "/concepts/automation-color.webp" },
    });
  });
  let fullRequests = 0;
  await page.route(`**/api/v1/shares/${token}/content`, (route) => {
    fullRequests++;
    return fulfillResult(route, {
      json: {
        publicationId: "11",
        markdown: `## 完整创作过程\n仅此文章的完整正文。\n\n![实战流程图](media:${fileId})`,
        version: "1",
      },
    });
  });
  await page.goto(`/s/${token}`);
  await expect(page.getByText("林舟 给你分享了一篇文章")).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "把一个想法，变成作品" }),
  ).toBeVisible();
  await expect(page.getByText("仅此文章的完整正文。")).toBeVisible();
  await expect(page.getByRole("link", { name: "查看全文" })).toHaveCount(0);
  await page.screenshot({
    path: info.outputPath("share-invitation.png"),
    fullPage: true,
  });
  await expect(page.getByText("仅此文章的完整正文。")).toBeVisible();
  expect(fullRequests).toBe(1);
  const zoom = page.getByRole("button", { name: "放大查看实战流程图" });
  await zoom.click();
  const dialog = page.getByRole("dialog", { name: "实战流程图" });
  await expect(dialog).toBeVisible();
  await expect(dialog.getByRole("img")).toHaveAttribute(
    "src",
    /automation-color/,
  );
  await page.keyboard.press("Escape");
  await expect(dialog).toHaveCount(0);
  await expect(zoom).toBeFocused();
  await zoom.click();
  await page.getByRole("button", { name: "关闭图片预览" }).click();
  await expect(dialog).toHaveCount(0);
  expect(imageRequests).toBe(1);

  await expect(page.getByRole("region", { name: "文章评论" })).toContainText(
    "这篇文章很有帮助。",
  );
  await expect(page.getByRole("img", { name: "林舟的头像" })).toBeVisible();
  await expect(page.getByRole("img", { name: "程雨的头像" })).toBeVisible();
  await expect(
    page.getByRole("img", { name: "程雨的头像" }).locator("img"),
  ).toHaveAttribute("src", /\/concepts\/presentation-color\.webp$/);
  await expect(page.getByRole("link", { name: "登录参与" })).toHaveAttribute(
    "href",
    `/login?next=${encodeURIComponent(`/s/${token}`)}`,
  );
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  for (const route of ["/home", "/discover", "/publications/12"]) {
    await page.goto(route);
    await expect(page).toHaveURL(/\/login\?next=/);
  }
});

test("paid share shows a login prompt and never fetches full text for guests", async ({
  page,
}) => {
  await mockShare(page, true);
  let fullRequests = 0;
  await page.route(`**/api/v1/shares/${token}/content`, (route) => {
    fullRequests++;
    return fulfillResult(route, { json: { markdown: "PRIVATE" } });
  });
  await page.goto(`/s/${token}/read`);
  await expect(
    page.getByRole("link", { name: "登录后解锁全文" }),
  ).toBeVisible();
  await expect(page.getByText("160 积分 · 解锁后可阅读全文")).toBeVisible();
  expect(fullRequests).toBe(0);
  await page.getByRole("link", { name: "登录后解锁全文" }).click();
  await expect(page).toHaveURL(
    new RegExp(`/login\\?next=${encodeURIComponent(`/s/${token}`)}`),
  );
});

test("an unlocked paid article opens its full text on the share URL", async ({
  page,
}) => {
  await mockShare(page, true, true);
  await mockMemberSession(page);
  await page.route(`**/api/v1/shares/${token}/content`, (route) =>
    fulfillResult(route, {
      json: {
        publicationId: "11",
        markdown: "已解锁的完整文章内容。",
        version: "1",
      },
    }),
  );
  await page.goto(`/s/${token}`);
  await expect(page.getByText("已解锁的完整文章内容。")).toBeVisible();
  await expect(page.getByRole("link", { name: "查看全文" })).toHaveCount(0);
  await expect(page.getByRole("link", { name: "登录后解锁全文" })).toHaveCount(
    0,
  );
});
