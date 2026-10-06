import { fulfillResult } from "./api-result";
import { expect, test } from "@playwright/test";

test("guest navigation is present in the first HTML response", async ({
  page,
}) => {
  let meRequests = 0;
  await page.route("**/api/v1/users/me", async (route) => {
    meRequests += 1;
    await fulfillResult(route, {
      status: 401,
      json: { code: "UNAUTHORIZED", message: "请先登录" },
    });
  });
  const response = await page.goto("/");
  const html = await response?.text();
  expect(html).toContain('href="/login"');
  expect(html).toContain('href="/register"');
  await page.evaluate(
    () =>
      new Promise<void>((resolve) =>
        requestAnimationFrame(() => requestAnimationFrame(() => resolve())),
      ),
  );
  expect(meRequests).toBe(0);
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
      url: `http://localhost:${process.env.E2E_PORT ?? "3200"}`,
    },
  ]);
  await page.route("**/api/v1/users/me", async (route) => {
    await sessionGate;
    await fulfillResult(route, { status: 200, json: member });
  });

  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  await expect(
    page.locator("header").getByRole("link", { name: "登录", exact: true }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("link", { name: "注册", exact: true }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("link", { name: "进入社区", exact: true }),
  ).toHaveAttribute("href", "/home");

  releaseSession?.();
  await expect(
    page.getByRole("button", { name: "打开用户菜单" }),
  ).toBeVisible();
  await page.getByRole("link", { name: "进入社区", exact: true }).click();
  await expect(page).toHaveURL(/\/home$/);
});

test("email delivery failures show a support request number", async ({
  page,
}) => {
  await page.route("**/api/v1/auth/email-codes", async (route) => {
    await fulfillResult(route, {
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

test("login shows the server rate-limit message without asking for a code", async ({
  page,
}) => {
  await page.route("**/api/v1/auth/login", async (route) => {
    expect(route.request().postDataJSON()).toEqual({
      email: "reader@example.com",
      password: "reader1234",
    });
    await fulfillResult(route, {
      status: 429,
      json: {
        code: "AUTH_RATE_LIMITED",
        message: "操作过于频繁，请稍后再试",
        requestId: "rate-test-request",
      },
    });
  });

  await page.goto("/login");
  await expect(page.getByLabel("邮箱验证码")).toHaveCount(0);
  await page.getByLabel("邮箱", { exact: true }).fill("reader@example.com");
  await page.getByLabel("密码", { exact: true }).fill("reader1234");
  await page.getByRole("button", { name: "登录 AriesHub" }).click();
  await expect(page.locator(".form-error")).toContainText("操作过于频繁");
  await expect(page.locator(".form-error")).toContainText(
    "请求编号：rate-test-request",
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
  let codeRequestCount = 0;

  // Resend 的真实投递由 Java 集成测试和手工收件验证负责；浏览器测试只验证交互契约。
  await page.route(/\/api\/v1\/(auth\/.*|users\/me)$/, async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/email-codes")) {
      codeRequestCount += 1;
      await fulfillResult(route, {
        status: 202,
        contentType: "application/json",
        body: JSON.stringify({ expiresInSeconds: 600, resendAfterSeconds: 60 }),
      });
      return;
    }
    if (path.endsWith("/register") || path.endsWith("/login")) {
      if (path.endsWith("/login")) {
        expect(route.request().postDataJSON()).not.toHaveProperty("code");
      }
      loggedIn = true;
      await fulfillResult(route, {
        status: path.endsWith("/register") ? 201 : 200,
        headers: {
          "set-cookie":
            "arieshub_token=identity-test; Path=/; HttpOnly; SameSite=Lax",
        },
        json: user,
      });
      return;
    }
    if (path.endsWith("/logout")) {
      loggedIn = false;
      await fulfillResult(route, {
        status: 204,
        body: "",
        headers: { "set-cookie": "arieshub_token=; Path=/; Max-Age=0" },
      });
      return;
    }
    await fulfillResult(
      route,
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
  await page.getByLabel("邮箱", { exact: true }).fill(email);
  await page.getByRole("button", { name: "获取验证码" }).click();
  await expect(
    page.getByRole("heading", { name: "查看你的收件箱" }),
  ).toBeVisible();
  await page.getByLabel("邮箱验证码").fill("123456");
  await page.getByRole("button", { name: "继续", exact: true }).click();
  await page.getByLabel("密码", { exact: true }).fill("arieshub2026");
  await page.getByRole("button", { name: "继续", exact: true }).click();
  await page.getByLabel("怎么称呼你").fill(nickname);
  await page.getByRole("button", { name: "继续", exact: true }).click();
  await page
    .getByRole("button", { name: "加入 AriesHub", exact: true })
    .click();
  expect(codeRequestCount).toBe(1);

  await expect(page).toHaveURL(/\/home$/, { timeout: 15_000 });
  await page.goto("/account");
  await expect(page.getByRole("heading", { name: nickname })).toBeVisible();

  await page.getByRole("button", { name: "退出登录" }).click();
  await expect(page).toHaveURL(/\/$/);
  await expect(
    page.locator("header").getByRole("link", { name: "登录", exact: true }),
  ).toBeVisible();

  await page
    .locator("header")
    .getByRole("link", { name: "登录", exact: true })
    .click();
  await page.getByLabel("邮箱", { exact: true }).fill(email);
  await expect(page.getByLabel("邮箱验证码")).toHaveCount(0);
  await page.getByLabel("密码", { exact: true }).fill("arieshub2026");
  await page.getByRole("button", { name: "登录 AriesHub" }).click();
  await expect(page).toHaveURL(/\/home$/, { timeout: 15_000 });
  await page.goto("/account");
  await expect(page.getByText(email, { exact: true })).toBeVisible();
  expect(codeRequestCount).toBe(1);
});

for (const conflictAt of ["email-codes", "register"]) {
  test(`existing email redirects to login when ${conflictAt} reports a conflict`, async ({
    page,
  }) => {
    await page.emulateMedia({ reducedMotion: "reduce" });
    await page.route("**/api/v1/auth/email-codes", (route) =>
      fulfillResult(
        route,
        conflictAt === "email-codes"
          ? {
              status: 409,
              json: {
                code: "EMAIL_ALREADY_REGISTERED",
                message: "该邮箱已注册，请直接登录",
              },
            }
          : { status: 202, json: { resendAfterSeconds: 60 } },
      ),
    );
    await page.route("**/api/v1/auth/register", (route) =>
      fulfillResult(route, {
        status: 409,
        json: {
          code: "EMAIL_ALREADY_REGISTERED",
          message: "该邮箱已注册，请直接登录",
        },
      }),
    );
    const email = "reader+existing@example.com";
    await page.goto("/register");
    await page.getByLabel("邮箱", { exact: true }).fill(email);
    await page.getByRole("button", { name: "获取验证码" }).click();
    if (conflictAt === "register") {
      await page.getByLabel("邮箱验证码").fill("123456");
      await page.getByRole("button", { name: "继续", exact: true }).click();
      await page.getByLabel("密码", { exact: true }).fill("arieshub2026");
      await page.getByRole("button", { name: "继续", exact: true }).click();
      await page.getByLabel("怎么称呼你").fill("测试成员");
      await page.getByRole("button", { name: "继续", exact: true }).click();
      await page
        .getByRole("button", { name: "加入 AriesHub", exact: true })
        .click();
    }
    await expect(page).toHaveURL(/\/login\?email=/);
    await expect(page.getByLabel("邮箱", { exact: true })).toHaveValue(email);
    await expect(page.getByLabel("密码", { exact: true })).toHaveValue("");
    await expect(page.locator(".form-error")).toHaveCount(0);
    await page.getByLabel("密码", { exact: true }).fill("login1234");
    const toggle = page.locator(".password-field button");
    await expect(toggle).toHaveCount(1);
    await toggle.click();
    await expect(page.getByLabel("密码", { exact: true })).toHaveAttribute(
      "type",
      "text",
    );
    await toggle.click();
    await expect(page.getByLabel("密码", { exact: true })).toHaveAttribute(
      "type",
      "password",
    );
  });
}

for (const mode of ["login", "register"]) {
  test(`${mode} rate limit shows server countdown and enables retry on expiry`, async ({
    page,
  }) => {
    await page.emulateMedia({ reducedMotion: "reduce" });
    await page.clock.install();
    await page.route(
      `**/api/v1/auth/${mode === "login" ? "login" : "email-codes"}`,
      (route) =>
        fulfillResult(route, {
          status: 429,
          headers: { "Retry-After": "65" },
          json: {
            code: "AUTH_RATE_LIMITED",
            message: "操作过于频繁，请稍后再试",
            requestId: "countdown-test",
          },
        }),
    );
    await page.goto(`/${mode}`);
    await page
      .getByLabel("邮箱", { exact: true })
      .fill("countdown@example.com");
    if (mode === "login")
      await page.getByLabel("密码", { exact: true }).fill("password123");
    await page.locator(".studio-submit").click();
    await expect(page.locator(".form-error")).toContainText("1 分 5 秒后重试");
    await expect(page.locator(".studio-submit")).toBeDisabled();
    await page.clock.fastForward(5000);
    await expect(page.locator(".form-error")).toContainText("1 分 0 秒后重试");
    await page.clock.fastForward(60000);
    await expect(page.locator(".studio-submit")).toBeEnabled();
    await expect(page.locator(".form-error")).toHaveCount(0);
  });
}
