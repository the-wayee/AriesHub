import { expect, test } from "@playwright/test";
import { fulfillResult } from "./api-result";
import { mockMemberSession } from "./member-session";

test("account and navigation use the same verified user without fetching the profile twice", async ({
  page,
}) => {
  await mockMemberSession(page);
  let requests = 0;
  page.on("request", (request) => {
    if (new URL(request.url()).pathname === "/api/v1/users/me") requests++;
  });
  await page.goto("/account");
  await expect(
    page.getByRole("heading", { name: "测试成员", exact: true }),
  ).toBeVisible();
  expect(requests).toBe(1);
});

test("Strict Mode shares pending GETs and failed requests can be retried", async ({
  page,
}) => {
  await mockMemberSession(page, "ADMIN");
  let requests = 0;
  await page.route("**/api/v1/admin/operations/overview", async (route) => {
    requests++;
    await fulfillResult(route, {
      status: 503,
      json: { code: "SERVICE_UNAVAILABLE", msg: "测试服务暂不可用" },
    });
  });
  await page.goto("/admin");
  await expect(
    page.getByRole("alert").filter({ hasText: "测试服务暂不可用" }),
  ).toBeVisible();
  expect(requests).toBe(1);
  await page.getByRole("button", { name: "重新加载" }).click();
  await expect.poll(() => requests).toBe(2);
});
