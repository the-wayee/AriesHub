import { expect, test } from "@playwright/test";

test("guest navigation is present in the first HTML response", async ({
  page,
}) => {
  const response = await page.goto("/");
  const html = await response?.text();
  expect(html).toContain('href="/login"');
  expect(html).toContain('href="/register"');
});

test("header keeps the signed-in entry stable while a refresh checks the session", async ({
  page,
  context,
}) => {
  const member = {
    id: "9002",
    email: "member@example.com",
    nickname: "测试成员",
    role: "USER",
    emailVerified: true,
    createdAt: "2026-09-28T10:00:00Z",
  };
  let releaseSession: (() => void) | undefined;
  const sessionGate = new Promise<void>((resolve) => {
    releaseSession = resolve;
  });

  await context.addCookies([
    {
      name: "arieshub_token",
      value: "test-session",
      url: `http://localhost:${process.env.E2E_PORT ?? "3000"}`,
    },
  ]);
  await page.route("**/api/v1/auth/me", async (route) => {
    await sessionGate;
    await route.fulfill({ status: 200, json: member });
  });

  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  await expect(
    page.getByRole("link", { name: "登录", exact: true }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("link", { name: "注册", exact: true }),
  ).toHaveCount(0);

  releaseSession?.();
  await expect(page.getByRole("link", { name: member.nickname })).toBeVisible();
});

test("email delivery failures show a support request number", async ({
  page,
}) => {
  await page.route("**/api/v1/auth/email-codes", async (route) => {
    await route.fulfill({
      status: 503,
      json: {
        code: "EMAIL_DELIVERY_FAILED",
        message: "验证码邮件暂时无法发送，请稍后再试",
        requestId: "email-test-request",
      },
    });
  });

  await page.goto("/register");
  await page.getByLabel("邮箱", { exact: true }).fill("reader@example.com");
  await page.getByRole("button", { name: "获取验证码" }).click();
  await expect(page.locator(".form-error")).toContainText(
    "验证码邮件暂时无法发送",
  );
  await expect(page.locator(".form-error")).toContainText(
    "请求编号：email-test-request",
  );
});

test("member can request codes, register, log out and log back in", async ({
  page,
}, testInfo) => {
  const suffix = `${testInfo.project.name}-${Date.now()}`
    .toLowerCase()
    .replace(/[^a-z0-9-]/g, "-");
  const email = `e2e-${suffix}@example.com`;
  const nickname = `测试成员${testInfo.project.name === "mobile" ? "M" : "D"}`;
  const user = {
    id: "9001",
    email,
    nickname,
    role: "USER",
    emailVerified: true,
    createdAt: "2026-09-28T10:00:00Z",
  };
  let loggedIn = false;

  // Resend 的真实投递由 Java 集成测试和手工收件验证负责；浏览器测试只验证交互契约。
  await page.route("**/api/v1/auth/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/email-codes")) {
      await route.fulfill({
        status: 202,
        contentType: "application/json",
        body: JSON.stringify({ expiresInSeconds: 600, resendAfterSeconds: 60 }),
      });
      return;
    }
    if (path.endsWith("/register") || path.endsWith("/login")) {
      loggedIn = true;
      await route.fulfill({
        status: path.endsWith("/register") ? 201 : 200,
        json: user,
      });
      return;
    }
    if (path.endsWith("/logout")) {
      loggedIn = false;
      await route.fulfill({ status: 204, body: "" });
      return;
    }
    await route.fulfill(
      loggedIn
        ? { status: 200, json: user }
        : {
            status: 401,
            json: {
              code: "UNAUTHORIZED",
              message: "请先登录",
              requestId: "e2e",
            },
          },
    );
  });

  await page.goto("/register");
  await page.getByLabel("怎么称呼你").fill(nickname);
  await page.getByLabel("邮箱", { exact: true }).fill(email);
  await page.getByRole("button", { name: "获取验证码" }).click();
  await expect(page.getByText("验证码已发送")).toBeVisible();
  await page.getByLabel("邮箱验证码").fill("123456");
  await page.getByLabel("密码").fill("arieshub2026");
  await page.getByRole("button", { name: "创建账号" }).click();

  await expect(page).toHaveURL(/\/account$/);
  await expect(page.getByRole("heading", { name: nickname })).toBeVisible();
  await expect(page.getByRole("link", { name: nickname })).toBeVisible();

  await page.getByRole("button", { name: "退出登录" }).click();
  await expect(page).toHaveURL(/\/$/);
  await expect(
    page.getByRole("link", { name: "登录", exact: true }),
  ).toBeVisible();

  await page.getByRole("link", { name: "登录", exact: true }).click();
  await page.getByLabel("邮箱", { exact: true }).fill(email);
  await page.getByRole("button", { name: "获取验证码" }).click();
  await page.getByLabel("邮箱验证码").fill("654321");
  await page.getByLabel("密码").fill("arieshub2026");
  await page.getByRole("button", { name: "登录 AriesHub" }).click();
  await expect(page).toHaveURL(/\/account$/);
  await expect(page.getByText(email, { exact: true })).toBeVisible();
});
