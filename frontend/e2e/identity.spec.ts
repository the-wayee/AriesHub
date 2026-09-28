import { expect, test } from "@playwright/test";

test("member can register, log out and log back in", async ({
  page,
}, testInfo) => {
  const suffix = `${testInfo.project.name}-${Date.now()}`
    .toLowerCase()
    .replace(/[^a-z0-9-]/g, "-");
  const email = `e2e-${suffix}@example.com`;
  const nickname = `测试成员${testInfo.project.name === "mobile" ? "M" : "D"}`;

  await page.goto("/register");
  await page.getByLabel("怎么称呼你").fill(nickname);
  await page.getByLabel("邮箱").fill(email);
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
  await page.getByLabel("邮箱").fill(email);
  await page.getByLabel("密码").fill("arieshub2026");
  await page.getByRole("button", { name: "登录 AriesHub" }).click();
  await expect(page).toHaveURL(/\/account$/);
  await expect(page.getByText(email, { exact: true })).toBeVisible();
});
