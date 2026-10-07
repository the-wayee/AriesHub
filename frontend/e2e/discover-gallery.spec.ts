import { expect, test } from "@playwright/test";
import { mockMemberSession } from "./member-session";
import { fulfillResult } from "./api-result";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import type { PublicationCardData } from "../src/lib/publication-reader";

// 固定的本地封面与示范稿用于验证错位空间，避免 OSS 与开发数据变化干扰验收。
const demos = JSON.parse(
  readFileSync("../backend/scripts/community-content.json", "utf8"),
);
test("discover gallery keeps an asymmetric rhythm and readable responsive order", async ({
  page,
}, info) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await mockMemberSession(page);
  await page.route("**/editorial-preview/*.png", (route) =>
    route.fulfill({
      contentType: "image/png",
      path: resolve(
        "../backend/scripts/editorial-covers",
        new URL(route.request().url()).pathname.split("/").pop()!,
      ),
    }),
  );
  const items: PublicationCardData[] = Array.from({ length: 9 }, (_, i) => ({
    publication: {
      id: String(i + 4),
      title: demos[i % demos.length].title,
      summary: demos[i % demos.length].summary,
      categoryName: "AI 实践",
      categorySlug: "practice",
      publicationType: "CASE_STUDY",
      accessType: "FREE",
      creditPrice: 0,
      publishedAt: "2026-10-07T00:00:00Z",
      coverFileId: `cover-${i}`,
      featured: i < 3,
    },
    cover: {
      url: `/editorial-preview/${String((i % 8) + 1).padStart(2, "0")}.png`,
      expiresAt: "2099-01-01T00:00:00Z",
    },
    interaction: {
      publicationId: String(i + 4),
      likeCount: 0,
      liked: false,
      bookmarked: false,
    },
    progress: null,
  }));
  await page.route("**/api/v1/categories", (route) =>
    fulfillResult(route, { json: [] }),
  );
  await page.route("**/api/v1/inspiration/quote", (route) =>
    fulfillResult(route, { json: null }),
  );
  await page.route("**/api/v1/publications/cards?*", (route) =>
    fulfillResult(route, {
      json: { items, total: 9, totalPages: 1, page: 1, size: 9 },
    }),
  );
  await page.goto("/discover");
  await expect(page.locator(".app-boot-overlay")).toHaveCount(0);
  await expect(page.locator(".discover-art-gallery article")).toHaveCount(9);
  await expect(page.locator(".discover-featured-card[href]")).toHaveCount(3);
  await expect(page.locator(".discover-featured-summary")).toHaveCount(3);
  await expect(page.locator(".discover-featured-type")).toHaveCount(3);
  for (const width of [1920, 1440, 1100, 768, 390]) {
    await page.setViewportSize({ width, height: 1000 });
    const main = await page.locator(".hub-main").boundingBox();
    expect(main!.x).toBeLessThanOrEqual(16);
    expect(main!.width).toBeGreaterThanOrEqual(width - 32);
    expect(
      await page
        .locator(".discover-featured-image")
        .first()
        .evaluate((el) => el.getBoundingClientRect().height),
    ).toBeLessThanOrEqual(148);
    if (width >= 1280) {
      const track = await page
        .locator(".discover-featured-track")
        .boundingBox();
      const last = await page
        .locator(".discover-featured-card[href]")
        .last()
        .boundingBox();
      expect(track!.x + track!.width - (last!.x + last!.width)).toBeLessThan(3);
    }
    await expect
      .poll(() =>
        page
          .locator(".discover-art-gallery article")
          .first()
          .evaluate((el) => el.style.gridColumn),
      )
      .toBe(
        width >= 1800
          ? "1 / span 14"
          : width >= 1280
            ? "1 / span 17"
            : width > 1000
              ? "1 / span 25"
              : width > 600
                ? "1 / span 30"
                : "1 / span 60",
      );
    const bounds = await page
      .locator(".discover-art-gallery .hub-content-image")
      .evaluateAll((els) =>
        els.map((el) => {
          const r = el.getBoundingClientRect();
          return { x: r.x, y: r.y, width: r.width, height: r.height };
        }),
      );
    if (width > 1000) {
      expect(bounds[0].width).toBeGreaterThan(bounds[1].width);
      expect(bounds[1].y - bounds[0].y).toBe(52);
      expect(
        Math.max(...bounds.slice(0, 4).map((card) => card.height)) -
          Math.min(...bounds.slice(0, 4).map((card) => card.height)),
      ).toBeGreaterThan(60);
      expect(
        Math.max(...bounds.map((card) => card.height)),
      ).toBeLessThanOrEqual(360);
      const count = width >= 1800 ? 5 : width >= 1280 ? 4 : 3;
      expect(bounds[count].width).toBeLessThan(bounds[0].width);
      if (width >= 1280)
        expect(Math.max(...bounds.map((card) => card.width))).toBeLessThan(480);
    } else if (width === 768) {
      expect(bounds[1].x).toBeGreaterThan(bounds[0].x);
      expect(bounds[1].y - bounds[0].y).toBe(20);
    } else {
      expect(bounds[1].x).toBe(bounds[0].x);
      expect(bounds[1].y).toBeGreaterThan(bounds[0].y + bounds[0].height);
    }
    const columns =
      width >= 1800
        ? 5
        : width >= 1280
          ? 4
          : width > 1000
            ? 3
            : width > 600
              ? 2
              : 1;
    const cards = await page
      .locator(".discover-art-gallery article")
      .evaluateAll((els) =>
        els.map((el) => {
          const r = el.getBoundingClientRect();
          return { x: r.x, right: r.right, y: r.y, bottom: r.bottom };
        }),
      );
    // 限高后的封面仍铺满卡片，不能按长宽比反向缩窄，留下假列间距。
    for (let index = 0; index < cards.length; index++) {
      expect(
        Math.abs(bounds[index].width - (cards[index].right - cards[index].x)),
      ).toBeLessThan(1);
    }
    // 变宽卡片只等待它覆盖的区域：接续间距紧凑，任意两篇完整内容不重叠。
    for (let index = columns; index < cards.length; index++) {
      const current = cards[index];
      const supportingBottom = Math.max(
        ...cards
          .slice(0, index)
          .filter(
            (card) => card.x < current.right - 1 && card.right > current.x + 1,
          )
          .map((card) => card.bottom),
      );
      const gap = current.y - supportingBottom;
      expect(gap).toBeGreaterThanOrEqual(columns === 1 ? 23 : 15);
      expect(gap).toBeLessThan(columns === 1 ? 26 : 18);
    }
    for (let index = 0; index < cards.length; index++) {
      for (const other of cards.slice(index + 1)) {
        const card = cards[index];
        expect(
          card.right <= other.x + 1 ||
            other.right <= card.x + 1 ||
            card.bottom <= other.y + 1 ||
            other.bottom <= card.y + 1,
        ).toBe(true);
      }
    }
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true);
    await page.screenshot({
      path: info.outputPath(`discover-${width}.png`),
      fullPage: true,
    });
  }
  const featuredTrack = page.locator(".discover-featured-track");
  expect(
    await featuredTrack.evaluate((el) => getComputedStyle(el).scrollbarWidth),
  ).toBe("none");
  expect(
    await page
      .locator(".discover-featured")
      .evaluate((el) => getComputedStyle(el).backgroundColor),
  ).toBe("rgba(0, 0, 0, 0)");
  const nextFeatured = page.getByRole("button", { name: "查看下一组精选" });
  await expect(nextFeatured).toBeVisible();
  await nextFeatured.click();
  await expect
    .poll(() => featuredTrack.evaluate((el) => el.scrollLeft))
    .toBeGreaterThan(0);
  await expect(
    page.getByRole("button", { name: "查看上一组精选" }),
  ).toBeVisible();
  await nextFeatured.click();
  await expect(nextFeatured).toHaveCount(0);
  await page.getByRole("button", { name: "查看上一组精选" }).click();
  await expect(nextFeatured).toBeVisible();
  // 自定义菜单保留语义与键盘访问；选择后出现同布局骨架，随后恢复真实卡片。
  let finishRequest: (() => void) | undefined;
  await page.route("**/api/v1/publications/cards?*", async (route) => {
    await new Promise<void>((resolve) => {
      finishRequest = resolve;
    });
    await fulfillResult(route, {
      json: { items, total: 9, totalPages: 1, page: 1, size: 9 },
    });
  });
  await page.emulateMedia({ reducedMotion: "no-preference" });
  const filter = page.getByRole("combobox", { name: "内容形式筛选" });
  await expect(filter).toContainText("全部形式");
  await filter.click();
  await expect(
    page.getByRole("option", { name: "学习文章", exact: true }),
  ).toBeVisible();
  await expect
    .poll(() =>
      page
        .locator(".discover-filter-popup")
        .evaluate((el) => getComputedStyle(el).opacity),
    )
    .toBe("1");
  await page.screenshot({ path: info.outputPath("filter-menu.png") });
  await page.getByRole("option", { name: "学习文章", exact: true }).click();
  await expect(page).toHaveURL(/type=ARTICLE/);
  await expect(page.locator(".discover-placeholder")).toHaveCount(9);
  await page.screenshot({ path: info.outputPath("gallery-loading.png") });
  await expect(page.locator(".discover-art-gallery")).toHaveAttribute(
    "aria-busy",
    "true",
  );
  await expect.poll(() => Boolean(finishRequest)).toBe(true);
  finishRequest!();
  await expect(page.locator(".discover-art-gallery")).toHaveAttribute(
    "aria-busy",
    "false",
  );
  await expect(
    page.locator(".discover-art-gallery article[data-revealed=true]").first(),
  ).toBeVisible();
  await expect(page.locator(".discover-placeholder")).toHaveCount(0);
  await filter.focus();
  await page.keyboard.press("ArrowDown");
  await expect(page.getByRole("listbox")).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(filter).toBeFocused();
});
