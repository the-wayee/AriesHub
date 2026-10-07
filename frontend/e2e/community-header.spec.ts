import { expect, test } from "@playwright/test";
import { mockMemberSession } from "./member-session";
import { fulfillResult } from "./api-result";

test("community navigation preserves header composition and updates only the active link", async ({
  page,
}, info) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await mockMemberSession(page, "ADMIN");
  await page.route("**/api/v1/inspiration/quote", (route) =>
    fulfillResult(route, { json: null }),
  );
  await page.route("**/api/v1/home", (route) =>
    fulfillResult(route, {
      json: { categories: [], featured: [], latest: [], continueReading: [] },
    }),
  );
  await page.route("**/api/v1/categories", (route) =>
    fulfillResult(route, { json: [] }),
  );
  await page.route(
    /\/api\/v1\/(publications\/cards|users\/me\/library)\?/,
    (route) =>
      fulfillResult(route, {
        json: { items: [], page: 1, size: 9, total: 0, totalPages: 0 },
      }),
  );
  await page.goto("/home");
  await expect(page.locator(".app-boot-overlay")).toHaveCount(0);
  const measure = () =>
    page.locator(".hub-header").evaluate((header) => {
      const nodes = [
        header,
        header.querySelector(".hub-brand")!,
        header.querySelector("nav")!,
        header.querySelector("form")!,
        header.querySelector(".hub-header-actions")!,
      ];
      return nodes.map((node) => {
        const rect = node.getBoundingClientRect();
        const css = getComputedStyle(node);
        return {
          x: rect.x,
          y: rect.y,
          width: rect.width,
          height: rect.height,
          background: css.backgroundColor,
          font: css.fontSize,
        };
      });
    });
  const initial = await measure();
  for (const [path, label] of [
    ["/discover", "探索"],
    ["/community", "讨论"],
    ["/my-content", "我的空间"],
    ["/home", "首页"],
  ]) {
    const nav = page.getByRole("navigation", { name: "社区导航" });
    await nav.getByRole("link", { name: label, exact: true }).click();
    await expect(page).toHaveURL(new RegExp(`${path}$`));
    await expect(
      nav.getByRole("link", { name: label, exact: true }),
    ).toHaveAttribute("aria-current", "page");
    await expect(page.locator(".hub-nav")).toHaveCount(0);
    const next = await measure();
    for (let index = 0; index < initial.length; index++) {
      for (const key of ["x", "y", "width", "height"] as const)
        expect(next[index][key]).toBeCloseTo(initial[index][key], 0);
      expect(next[index].background).toBe(initial[index].background);
      expect(next[index].font).toBe(initial[index].font);
    }
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true);
    await page.screenshot({ path: info.outputPath(`header-${label}.png`) });
  }
});
