import { fulfillResult } from "./api-result";
import type { Page } from "@playwright/test";

export async function mockMemberSession(page: Page, role = "USER") {
  await page.context().addCookies([
    {
      name: "arieshub_token",
      value: "member-test",
      url: `http://localhost:${process.env.E2E_PORT ?? "3200"}`,
    },
  ]);
  await page.route("**/api/v1/users/me", (route) =>
    fulfillResult(route, {
      json: {
        id: "9001",
        email: "member@example.com",
        nickname: "测试成员",
        bio: "",
        avatarFileId: null,
        role,
        emailVerified: true,
        createdAt: "2026-09-28T10:00:00Z",
      },
    }),
  );
}
