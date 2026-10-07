import { fulfillResult } from "./api-result";
import { expect, test, type Page } from "@playwright/test";

const image = Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j6V8AAAAASUVORK5CYII=",
  "base64",
);
const avatarId = "0ad2ecc1-d2a6-47ab-8355-fd260bf1ad6e";

async function memberSession(page: Page) {
  let member = {
    id: "9001",
    email: "profile@example.com",
    nickname: "林小雨",
    bio: "探索 AI 与日常生活的更多可能。",
    avatarFileId: null as string | null,
    role: "USER",
    emailVerified: true,
    createdAt: "2026-09-28T10:00:00Z",
  };
  await page.context().addCookies([
    {
      name: "arieshub_token",
      value: "profile-test",
      url: `http://localhost:${process.env.E2E_PORT ?? "3200"}`,
    },
  ]);
  await page.route("**/api/v1/users/me", (route) =>
    fulfillResult(route, { json: member }),
  );
  await page.route("**/api/v1/users/me/profile", async (route) => {
    expect(route.request().method()).toBe("PUT");
    member = { ...member, ...route.request().postDataJSON() };
    await fulfillResult(route, { json: member });
  });
  await page.route("**/api/v1/users/me/avatar-url", (route) =>
    fulfillResult(route, {
      json: {
        url: `http://localhost:${process.env.E2E_PORT ?? "3200"}/test-avatar.png`,
        expiresAt: new Date(Date.now() + 300_000).toISOString(),
      },
    }),
  );
  await page.route("**/test-avatar.png", (route) =>
    fulfillResult(route, { contentType: "image/png", body: image }),
  );
}

test("profile saves nickname and signature, updates badge, and survives reload", async ({
  page,
}, testInfo) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await memberSession(page);
  await page.goto("/account");
  await expect(page.locator(".app-boot-overlay")).toHaveCount(0);
  await expect(page.getByRole("navigation", { name: "社区导航" })).toHaveCount(
    1,
  );
  await expect(
    page.getByRole("heading", { name: "林小雨", exact: true }),
  ).toBeVisible();
  await expect(page.getByRole("button", { name: "保存资料" })).toBeDisabled();
  await page.getByLabel("怎么称呼你").fill("林间好奇者");
  await page
    .getByLabel("个性签名")
    .fill("最近在用 AI 做一个属于自己的小项目。");
  await page.getByRole("button", { name: "保存资料" }).click();
  await expect(page.getByRole("status")).toHaveText("资料已保存");
  await expect(
    page.getByRole("button", { name: "打开用户菜单" }),
  ).toContainText("林间好奇者");
  await page.reload();
  await expect(page.getByLabel("个性签名")).toHaveValue(
    "最近在用 AI 做一个属于自己的小项目。",
  );
  await expect(
    page.getByRole("heading", { name: "林间好奇者", exact: true }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.evaluate(() => window.scrollTo(0, 0));
  await expect(page.locator(".hub-header")).toBeInViewport();
  await page.screenshot({
    path: testInfo.outputPath("account.png"),
    fullPage: true,
  });
});

test("avatar uploads before profile save and can be removed", async ({
  page,
}) => {
  await memberSession(page);
  let uploads = 0;
  await page.route("**/api/v1/users/me/avatar", async (route) => {
    uploads++;
    expect(route.request().headers()["content-type"]).toContain(
      "multipart/form-data",
    );
    expect(route.request().postData()).toContain("avatar.png");
    await fulfillResult(route, {
      status: 201,
      json: { id: avatarId, purpose: "AVATAR" },
    });
  });
  await page.goto("/account");
  await expect(page.locator(".app-boot-overlay")).toHaveCount(0);
  await page.getByLabel("选择头像图片").setInputFiles({
    name: "avatar.png",
    mimeType: "image/png",
    buffer: image,
  });
  await page.getByRole("button", { name: "保存资料" }).click();
  await expect(page.getByRole("status")).toHaveText("资料已保存");
  expect(uploads).toBe(1);
  await expect(page.locator(".user-menu-trigger img")).toBeVisible();
  await page.reload();
  await expect(page.locator(".profile-card-avatar img")).toBeVisible();
  await page.getByRole("button", { name: "移除头像" }).click();
  await page.getByRole("button", { name: "保存资料" }).click();
  await expect(page.getByRole("status")).toHaveText("资料已保存");
  await expect(page.locator(".user-menu-trigger img")).toHaveCount(0);
});

test("upload failures preserve edits and invalid file types are rejected", async ({
  page,
}) => {
  await memberSession(page);
  await page.route("**/api/v1/users/me/avatar", (route) =>
    fulfillResult(route, {
      status: 503,
      json: {
        code: "STORAGE_UNAVAILABLE",
        message: "文件存储暂时不可用，请稍后重试",
      },
    }),
  );
  await page.goto("/account");
  await expect(page.locator(".app-boot-overlay")).toHaveCount(0);
  await page.getByLabel("选择头像图片").setInputFiles({
    name: "avatar.svg",
    mimeType: "image/svg+xml",
    buffer: Buffer.from("<svg/>"),
  });
  await expect(
    page.locator(".profile-editor").getByRole("alert"),
  ).toContainText("请选择 PNG、JPG 或 WebP 图片");
  await page.getByLabel("个性签名").fill("这份修改应该被保留。");
  await page.getByLabel("选择头像图片").setInputFiles({
    name: "avatar.png",
    mimeType: "image/png",
    buffer: image,
  });
  await page.getByRole("button", { name: "保存资料" }).click();
  await expect(
    page.locator(".profile-editor").getByRole("alert"),
  ).toContainText("文件存储暂时不可用");
  await expect(page.getByLabel("个性签名")).toHaveValue("这份修改应该被保留。");
  await expect(page.getByRole("button", { name: "保存资料" })).toBeEnabled();
});

test("user menu opens by keyboard, links to settings and logs out", async ({
  page,
}) => {
  await memberSession(page);
  await page.route("**/api/v1/auth/logout", async (route) => {
    await page.unroute("**/api/v1/users/me");
    await page.route("**/api/v1/users/me", (r) =>
      fulfillResult(r, {
        status: 401,
        json: { code: "UNAUTHENTICATED", message: "请先登录" },
      }),
    );
    await fulfillResult(route, { status: 204 });
  });
  await page.goto("/community");
  await expect(
    page
      .getByRole("navigation", { name: "社区导航" })
      .getByRole("link", { name: "账号" }),
  ).toHaveCount(0);
  const badge = page.getByRole("button", { name: "打开用户菜单" });
  await badge.press("Enter");
  await expect(page.getByRole("menuitem", { name: "账号设置" })).toBeVisible();
  await page.getByRole("menuitem", { name: "账号设置" }).press("Escape");
  await expect(badge).toBeFocused();
  await badge.click();
  await page.getByRole("menuitem", { name: "账号设置" }).click();
  await expect(page).toHaveURL(/\/account$/);
  await badge.click();
  await page.getByRole("menuitem", { name: "退出登录" }).click();
  await expect(page).toHaveURL(/\/$/);
  await expect(
    page.locator("header").getByRole("link", { name: "登录", exact: true }),
  ).toBeVisible();
});

test("account service errors show retry instead of treating member as logged out", async ({
  page,
}) => {
  await memberSession(page);
  await page.route("**/api/v1/users/me", (route) =>
    fulfillResult(route, {
      status: 503,
      json: { code: "SERVICE_UNAVAILABLE", message: "账号服务暂时不可用" },
    }),
  );
  await page.goto("/account");
  await expect(page.locator(".app-boot-overlay")).toHaveCount(0);
  await expect(
    page.getByRole("heading", { name: "暂时无法验证登录状态" }),
  ).toBeVisible();
  await expect(page.getByRole("button", { name: "重新验证" })).toBeVisible();
});
