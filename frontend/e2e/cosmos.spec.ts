import { mockReaderApi } from "./reader-api";
import { fulfillResult } from "./api-result";
import { test, expect } from "@playwright/test";
test("landing discovery, carousel and local assets", async ({ page }) => {
  await mockReaderApi(page);
  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1 })).toContainText(
    "与同路人一起",
  );
  await page.getByRole("button", { name: "今天，想探索什么？" }).click();
  await expect(page.getByText("选择你的下一站")).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(page.getByText("选择你的下一站")).toHaveCount(0);
  await page.getByRole("button", { name: "下一个实践方向" }).click();
  await expect(page.locator(".reader-landing-gallery h2").first()).toHaveText(
    "AI 演示实践",
  );
  // Offscreen lazy images need not be decoded; verify every asset and visible icons.
  const sources = await page
    .locator(".brand-tile img")
    .evaluateAll((images) => [
      ...new Set(images.map((i) => (i as HTMLImageElement).src)),
    ]);
  for (const source of sources)
    expect((await page.request.get(source)).status()).toBe(200);
  await page.evaluate(() => window.scrollTo(0, 0));
  await expect
    .poll(() =>
      page.locator(".brand-tile img").evaluateAll((images) => {
        const visible = images.filter((i) => {
          const r = i.getBoundingClientRect();
          return (
            r.bottom > 0 &&
            r.top < innerHeight &&
            r.right > 0 &&
            r.left < innerWidth
          );
        });
        return (
          visible.length > 0 &&
          visible.every(
            (i) =>
              (i as HTMLImageElement).complete &&
              (i as HTMLImageElement).naturalWidth > 0,
          )
        );
      }),
    )
    .toBe(true);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBeTruthy();
});
test("registration demo completes, back preserves data, passwords stay private", async ({
  page,
}) => {
  await page.goto("/register");
  await page.getByRole("button", { name: "仅体验页面流程" }).click();
  await page.getByLabel("邮箱", { exact: true }).fill("preview@example.com");
  await page.getByRole("button", { name: "获取验证码" }).click();
  await expect(
    page.getByRole("heading", { name: "查看你的收件箱" }),
  ).toBeVisible();
  await page.getByLabel("邮箱验证码").fill("123456");
  await page.getByRole("button", { name: "继续", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "设置你的密码" }),
  ).toBeVisible();
  await page.getByLabel("密码", { exact: true }).fill("preview1234");
  await page.getByRole("button", { name: "显示密码" }).click();
  await expect(page.getByLabel("密码", { exact: true })).toHaveAttribute(
    "type",
    "text",
  );
  await page.getByRole("button", { name: "继续", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "怎么称呼你？" }),
  ).toBeVisible();
  await page.getByLabel("怎么称呼你").fill("体验成员");
  await page.getByRole("button", { name: "上一步" }).click();
  await expect(page.getByLabel("密码", { exact: true })).toHaveValue(
    "preview1234",
  );
  await page.getByRole("button", { name: "继续", exact: true }).click();
  await page.getByRole("button", { name: "继续", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "你想探索什么？" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "AI 编程", exact: true }).click();
  await expect(
    page.getByRole("button", { name: "AI 编程", exact: true }),
  ).toHaveAttribute("aria-pressed", "true");
  await page
    .getByRole("button", { name: "加入 AriesHub", exact: true })
    .click();
  await expect(page).toHaveURL(/discover\?preview=1/);
  expect(
    await page.evaluate(() =>
      JSON.stringify({ ...localStorage, ...sessionStorage }),
    ),
  ).not.toContain("preview1234");
});
test("login keeps rate-limit feedback and no verification code", async ({
  page,
}) => {
  await page.route("**/api/v1/auth/login", (r) =>
    fulfillResult(r, {
      status: 429,
      json: {
        code: "AUTH_RATE_LIMITED",
        message: "操作过于频繁，请稍后再试",
        requestId: "preview-test",
      },
    }),
  );
  await page.goto("/login");
  await expect(page.getByLabel("邮箱验证码")).toHaveCount(0);
  await page.getByLabel("邮箱", { exact: true }).fill("preview@example.com");
  await page.getByLabel("密码", { exact: true }).fill("preview1234");
  await page.getByRole("button", { name: "登录 AriesHub" }).click();
  await expect(page.locator(".form-error")).toContainText("操作过于频繁");
  await expect(page.locator(".form-error")).toContainText("preview-test");
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBeTruthy();
});
test("local film plays through custom controls", async ({ page }) => {
  await page.goto("/");

  // The film frame moves with scroll; use keyboard activation after focus settles.
  await page.getByRole("button", { name: "播放影片", exact: true }).focus();
  await page
    .getByRole("button", { name: "播放影片", exact: true })
    .press("Enter");
  await expect(
    page.getByRole("button", { name: "暂停影片", exact: true }),
  ).toBeAttached({ timeout: 15000 });
  await expect
    .poll(() =>
      page
        .locator("video")
        .evaluate((v) => (v as HTMLVideoElement).currentTime),
    )
    .toBeGreaterThan(0);
  await page.getByRole("button", { name: "暂停影片", exact: true }).focus();
  await page.getByRole("button", { name: "暂停影片", exact: true }).click();
  await expect(
    page.getByRole("button", { name: "播放影片", exact: true }),
  ).toBeVisible();
});

test("reduced motion keeps content visible and accessible", async ({
  page,
}) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("console", (message) => {
    if (
      message.type() === "error" &&
      /hydration|hydrated/i.test(message.text())
    )
      errors.push(message.text());
  });
  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  await expect(page.locator(".hero-copy")).toHaveCSS("opacity", "1");
  await page.goto("/register");
  await page.getByRole("button", { name: "仅体验页面流程" }).click();
  await page.getByRole("button", { name: "获取验证码" }).click();
  await expect(page.locator(".form-error")).toContainText("邮箱");
  await page.getByLabel("邮箱", { exact: true }).fill("reduced@example.com");
  await page.getByRole("button", { name: "获取验证码" }).click();
  await expect(
    page.getByRole("heading", { name: "查看你的收件箱" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "上一步" }).click();
  await expect(page.getByLabel("邮箱", { exact: true })).toHaveValue(
    "reduced@example.com",
  );
  expect(errors).toEqual([]);
});
