import { expect, test } from "@playwright/test";
import { mockMemberSession } from "./member-session";

test("guest requests are redirected before member HTML is rendered", async ({
  request,
}) => {
  for (const path of [
    "/home",
    "/community",
    "/my-content",
    "/members",
    "/account",
    "/learn/website-from-zero",
    "/checkout/website-from-zero",
    "/admin",
    "/admin/publications/new",
    "/studio",
  ]) {
    const response = await request.get(path, { maxRedirects: 0 });
    expect(response.status()).toBe(307);
    expect(new URL(response.headers().location, response.url()).pathname).toBe(
      "/login",
    );
    expect(await response.text()).not.toContain("晚上好，欢迎回来。");
  }
  expect((await request.get("/discover")).status()).toBe(200);
});

test("forged or expired cookies cannot display member content", async ({
  page,
}) => {
  await mockMemberSession(page);
  await page.route("**/api/v1/users/me", (route) =>
    route.fulfill({
      status: 401,
      json: { code: "UNAUTHENTICATED", message: "请先登录" },
    }),
  );
  await page.goto("/home");
  await expect(page).toHaveURL(/\/login\?next=/);
  await expect(
    page.getByRole("heading", { name: "晚上好，欢迎回来。" }),
  ).toHaveCount(0);
});

test("members cannot enter administrator pages", async ({ page }) => {
  await mockMemberSession(page);
  let adminRequests = 0;
  await page.route("**/api/v1/admin/**", (route) => {
    adminRequests++;
    return route.fulfill({ json: [] });
  });
  await page.goto("/admin/publications/new");
  await expect(
    page.getByRole("heading", { name: "此页面仅对管理员开放" }),
  ).toBeVisible();
  expect(adminRequests).toBe(0);
});
