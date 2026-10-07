import { expect, test } from "@playwright/test";
import { fulfillResult } from "./api-result";
import { mockMemberSession } from "./member-session";

const comment = {
  id: "701",
  parentId: null,
  rootId: null,
  depth: 0,
  authorId: "9001",
  authorName: "林舟",
  body: "重试时保留同一个任务标识，这个细节很有帮助。",
  likeCount: 2,
  likedByMe: false,
  deleted: false,
  canDelete: true,
  createdAt: "2026-10-07T08:00:00Z",
};

test("emoji inserts at the cursor and nested replies retain their @ recipient", async ({
  page,
}, info) => {
  await mockMemberSession(page);
  const reply = {
    ...comment,
    id: "703",
    authorName: "程雨",
    authorId: "9002",
    parentId: "701",
    rootId: "701",
    depth: 1,
    body: "谢谢分享👏",
  };
  const replies = [reply];
  const bodies: Record<string, unknown>[] = [];
  await page.route("**/api/v1/discussions/comments**", async (route) => {
    if (route.request().method() === "POST") {
      const body = route.request().postDataJSON();
      bodies.push(body);
      if (body.parentId)
        replies.push({
          ...reply,
          id: "704",
          parentId: body.parentId,
          authorName: "林舟",
          body: body.body,
          replyToAuthorName: "程雨",
        } as typeof reply);
      return fulfillResult(route, {
        status: 201,
        json: { ...comment, body: body.body },
      });
    }
    return fulfillResult(route, {
      json: {
        items: [
          { comment, replyCount: replies.length, previewReplies: replies },
        ],
        page: 1,
        size: 10,
        total: 1,
        totalPages: 1,
      },
    });
  });
  await page.goto("/publications/11");
  const section = page.getByRole("region", { name: "文章评论" });
  const input = section.getByRole("textbox", { name: "写下评论" });
  await input.fill("你好世界");
  await input.evaluate((node: HTMLTextAreaElement) => {
    node.focus();
    node.setSelectionRange(2, 2);
    node.dispatchEvent(new Event("select", { bubbles: true }));
  });
  await section.getByRole("button", { name: "添加表情" }).click();
  await expect(page.getByRole("button", { name: "表情 微笑" })).toBeVisible();
  await page.screenshot({ path: info.outputPath("emoji-picker.png") });
  await page.getByRole("button", { name: "表情 微笑" }).click();
  await expect(input).toHaveValue("你好😊世界");
  await expect(input).toBeFocused();
  await section.getByRole("button", { name: "发表评论" }).click();
  expect(bodies[0].body).toBe("你好😊世界");
  const replyRow = section.locator(".comment-replies .comment-row").first();
  await replyRow.getByRole("button", { name: "回复", exact: true }).click();
  await expect(section.locator(".comment-reply-to")).toContainText("@程雨");
  await section.getByRole("textbox", { name: "回复 程雨" }).fill("一起实践");
  const nestedComposer = section.locator(".comment-thread .comment-composer");
  await nestedComposer.getByRole("button", { name: "添加表情" }).click();
  await page.getByRole("button", { name: "表情 加油" }).click();
  await section.getByRole("button", { name: "发送回复" }).click();
  expect(bodies.at(-1)?.parentId).toBe("703");
  await expect(section.locator(".comment-reply-prefix")).toContainText("@程雨");
  await expect(section.locator(".comment-replies")).toContainText("一起实践💪");
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});

test("login from a comment returns to the same article", async ({ page }) => {
  await page.route("**/api/v1/auth/login", async (route) => {
    await mockMemberSession(page);
    return fulfillResult(route, {
      json: {
        id: "9001",
        email: "member@example.com",
        nickname: "测试成员",
        role: "USER",
        emailVerified: true,
        createdAt: "2026-10-07T08:00:00Z",
      },
    });
  });
  await page.goto("/login?next=%2Fpublications%2F11%23comments");
  await page.getByLabel("邮箱", { exact: true }).fill("member@example.com");
  await page.getByLabel("密码", { exact: true }).fill("member1234");
  await page.getByRole("button", { name: "登录 AriesHub" }).click();
  await expect(page).toHaveURL(/\/publications\/11#comments$/);
  await expect(
    page
      .getByRole("region", { name: "文章评论" })
      .getByRole("textbox", { name: "写下评论" }),
  ).toBeVisible();
});

test("guest needs login for an ordinary article", async ({ page }) => {
  await page.goto("/publications/11");
  await expect(page).toHaveURL(/\/login\?next=/);
  await expect(page.getByRole("region", { name: "文章评论" })).toHaveCount(0);
});

test("member comments survive failed submissions and support replies likes and deletion", async ({
  page,
}, info) => {
  await mockMemberSession(page);
  const roots = [
    {
      comment: { ...comment },
      replyCount: 0,
      previewReplies: [] as (typeof comment)[],
    },
  ];
  let reject = true;
  const requests: Record<string, unknown>[] = [];
  let likes = 0;
  await page.route("**/api/v1/discussions/comments**", async (route) => {
    const request = route.request();
    if (request.url().endsWith("/like")) {
      likes++;
      return fulfillResult(route, { json: { liked: likes % 2 === 1 } });
    }
    if (request.method() === "DELETE") {
      roots[0].comment = {
        ...roots[0].comment,
        deleted: true,
        body: "",
        canDelete: false,
      };
      return fulfillResult(route, { json: null });
    }
    if (request.method() === "POST") {
      const body = request.postDataJSON();
      requests.push(body);
      if (reject) {
        reject = false;
        return fulfillResult(route, {
          status: 503,
          json: { code: "TEMPORARY_ERROR", msg: "暂时无法发送，请重试" },
        });
      }
      const created = {
        ...comment,
        id: body.parentId ? "703" : "702",
        body: body.body,
        likeCount: 0,
      };
      if (body.parentId) {
        roots[0].previewReplies.push(created);
        roots[0].replyCount++;
      } else
        roots.push({ comment: created, replyCount: 0, previewReplies: [] });
      return fulfillResult(route, { status: 201, json: created });
    }
    return fulfillResult(route, {
      json: {
        items: roots,
        page: 1,
        size: 10,
        total: roots.length,
        totalPages: 1,
      },
    });
  });
  await page.goto("/publications/11");
  const comments = page.getByRole("region", { name: "文章评论" });
  const composer = comments.getByRole("textbox", { name: "写下评论" });
  await composer.fill("我会用自己的项目再试一遍。");
  await comments.getByRole("button", { name: "发表评论" }).click();
  await expect(comments.getByRole("alert")).toContainText("暂时无法发送");
  await expect(composer).toHaveValue("我会用自己的项目再试一遍。");
  await comments.getByRole("button", { name: "发表评论" }).click();
  await expect(composer).toHaveValue("");
  await expect(
    comments.getByText("我会用自己的项目再试一遍。", { exact: true }),
  ).toBeVisible();
  expect(requests[0].targetType).toBe("PUBLICATION");
  expect(requests[0].targetKey).toBeTruthy();
  const first = comments.locator(".comment-thread").first();
  await first.getByRole("button", { name: "点赞 林舟 的评论" }).click();
  await expect(
    first.getByRole("button", { name: "点赞 林舟 的评论" }),
  ).toHaveAttribute("aria-pressed", "true");
  await expect(
    first.getByRole("button", { name: "点赞 林舟 的评论" }),
  ).toContainText("3");
  await first.getByRole("button", { name: "回复", exact: true }).click();
  await first
    .getByRole("textbox", { name: "回复 林舟" })
    .fill("同意，边界条件值得单独验收。");
  await first.getByRole("button", { name: "发送回复" }).click();
  await expect(first).toContainText("同意，边界条件值得单独验收。");
  expect(requests.at(-1)?.parentId).toBe("701");
  await first.getByRole("button", { name: "删除 林舟 的评论" }).first().click();
  await first.getByRole("button", { name: "确认删除" }).click();
  await expect(first).toContainText("这条评论已删除");
  await expect(first).toContainText("同意，边界条件值得单独验收。");
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await comments.scrollIntoViewIfNeeded();
  await page.screenshot({
    path: info.outputPath("comments.png"),
    fullPage: false,
  });
});

test("comment sorting pagination and full reply expansion use server pages", async ({
  page,
}) => {
  await mockMemberSession(page);
  const sorts: string[] = [];
  await page.route("**/api/v1/discussions/comments**", async (route) => {
    const url = new URL(route.request().url());
    if (url.pathname.endsWith("/replies"))
      return fulfillResult(route, {
        json: {
          items: [
            {
              ...comment,
              id: "810",
              authorName: "程雨",
              body: "完整回复",
              canDelete: false,
            },
          ],
          page: 1,
          size: 10,
          total: 1,
          totalPages: 1,
        },
      });
    sorts.push(url.searchParams.get("sort")!);
    const page = Number(url.searchParams.get("page"));
    return fulfillResult(route, {
      json: {
        items: [
          {
            comment: {
              ...comment,
              id: String(page === 1 ? 701 : 702),
              body: page === 1 ? comment.body : "第二页评论",
              canDelete: false,
            },
            replyCount: page === 1 ? 1 : 0,
            previewReplies: [],
          },
        ],
        page,
        size: 10,
        total: 2,
        totalPages: 2,
      },
    });
  });
  await page.goto("/publications/11");
  const comments = page.getByRole("region", { name: "文章评论" });
  await comments.getByRole("button", { name: "查看全部 1 条回复" }).click();
  await expect(comments).toContainText("完整回复");
  await comments.getByRole("button", { name: "查看更多评论" }).click();
  await expect(comments).toContainText("第二页评论");
  await expect(comments.locator(".comment-thread")).toHaveCount(2);
  await comments.getByRole("button", { name: "最热", exact: true }).click();
  await expect(comments.locator(".comment-thread")).toHaveCount(1);
  expect(sorts).toContain("HOT");
});

test("article footer shares a canonical link and offers manual copy on failure", async ({
  page,
}) => {
  await mockMemberSession(page);
  const token = "s".repeat(43);
  await page.route("**/api/v1/publications/11/share-link", (route) =>
    fulfillResult(route, {
      json: {
        publicationId: "11",
        token,
        url: `http://localhost:3200/s/${token}`,
      },
    }),
  );
  await page.addInitScript(() => {
    Object.defineProperty(navigator, "share", {
      configurable: true,
      value: undefined,
    });
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: {
        writeText: async (url: string) => {
          document.documentElement.dataset.sharedUrl = url;
        },
      },
    });
  });
  await page.route("**/api/v1/publications/11/share", (route) =>
    fulfillResult(route, {
      json: {
        publicationId: "11",
        likeCount: 0,
        liked: false,
        bookmarked: false,
        bookmarkCount: 0,
        shareCount: 1,
        viewCount: 1,
      },
    }),
  );
  await page.goto("/publications/11?from=test#comments");
  const actions = page.getByLabel("文章操作", { exact: true });
  await expect(actions).toBeVisible();
  await expect(
    page.locator(".hub-detail-heading .reader-interactions"),
  ).toHaveCount(0);
  await actions.getByRole("button", { name: "分享文章" }).click();
  await page.getByRole("button", { name: "复制链接", exact: true }).click();
  await expect(
    page.getByRole("button", { name: "链接已复制", exact: true }),
  ).toBeVisible();
  await expect(page.locator("html")).toHaveAttribute(
    "data-shared-url",
    /\/s\/[A-Za-z0-9_-]{43}$/,
  );
  await page.evaluate(() => {
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: {
        writeText: async () => {
          throw new Error("denied");
        },
      },
    });
  });
  await page.getByRole("button", { name: "链接已复制", exact: true }).click();
  await expect(
    page.locator(".publication-share-popup").getByRole("alert"),
  ).toContainText("自动复制失败");
  await expect(page.getByRole("textbox", { name: "文章分享链接" })).toHaveValue(
    /\/s\/[A-Za-z0-9_-]{43}$/,
  );
});
