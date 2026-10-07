import { expect, test } from "@playwright/test";
import { resolve } from "node:path";
import { readFileSync } from "node:fs";
import { mockMemberSession } from "./member-session";
import { fulfillResult } from "./api-result";
import type {
  MemberHome,
  PublicationCardData,
} from "../src/lib/publication-reader";

// 文章文案和阅读方式复用已发布示范稿，互动数字仍是隔离的视觉验收数据。
const demos: Pick<
  PublicationCardData["publication"],
  "title" | "summary" | "publicationType" | "accessType" | "creditPrice"
>[] = JSON.parse(
  readFileSync(resolve("../backend/scripts/community-content.json"), "utf8"),
);
const titles = demos.map((demo) => demo.title);

/** 视觉验收使用本地概念影像，不依赖 OSS、真实账号和变化中的开发数据库。 */
function homeFixture(): MemberHome {
  const cards: PublicationCardData[] = titles.map((title, index) => ({
    publication: {
      id: String(index + 4),
      slug: `example-${index}`,
      title,
      summary: demos[index].summary,
      categoryName: "AI 实践",
      categorySlug: "practice",
      publicationType: demos[index].publicationType,
      accessType: demos[index].accessType,
      creditPrice: demos[index].creditPrice,
      publishedAt: "2026-10-07T00:00:00Z",
      coverFileId: `00000000-0000-4000-8000-${String(index).padStart(12, "0")}`,
      featured: index < 3,
    },
    cover: {
      url: `/editorial-preview/${String(index + 1).padStart(2, "0")}.png`,
      expiresAt: "2099-01-01T00:00:00Z",
    },
    interaction: {
      publicationId: String(index + 4),
      likeCount: index * 3,
      liked: false,
      bookmarked: false,
    },
    progress: null,
  }));
  const reading = {
    ...cards[4],
    progress: {
      publicationId: cards[4].publication.id,
      version: "1.0",
      position: "read-example-0",
      percent: 43,
      updatedAt: "2026-10-07T00:00:00Z",
    },
  };
  return {
    categories: [
      {
        id: "1",
        name: "AI 编程",
        slug: "coding",
        color: "#4967A9",
        publicationCount: 4,
      },
      {
        id: "2",
        name: "AI 影像",
        slug: "image",
        color: "#4967A9",
        publicationCount: 2,
      },
      {
        id: "3",
        name: "工作流",
        slug: "workflow",
        color: "#4967A9",
        publicationCount: 2,
      },
    ],
    featured: [cards[2], cards[0], cards[1]],
    latest: cards,
    continueReading: [reading],
  };
}

test("full-width home preserves reading, unique articles and responsive composition", async ({
  page,
}, testInfo) => {
  await mockMemberSession(page);
  await page.route("**/editorial-preview/*.png", (route) => {
    const name = new URL(route.request().url()).pathname.split("/").pop()!;
    return route.fulfill({
      contentType: "image/png",
      path: resolve("../backend/scripts/editorial-covers", name),
    });
  });
  await page.route("**/api/v1/home", (route) =>
    fulfillResult(route, { json: homeFixture() }),
  );
  // 同一浏览器验证宽屏、平板与手机，确保不是仅在默认桌面宽度上成立。
  for (const width of [1920, 768, 390]) {
    await page.setViewportSize({ width, height: 1080 });
    await page.goto("/home");
    await expect(
      page.locator(".home-content-gallery .hub-content-card"),
    ).toHaveCount(8);
    await expect(page.locator(".home-content-gallery h2").first()).toHaveCSS(
      "font-size",
      "18px",
    );
    await expect(
      page.locator(".home-practice-gallery .hub-section-heading h2"),
    ).toHaveCSS("color", "rgb(32, 33, 38)");
    await expect(page.getByLabel("上次阅读进度")).toHaveAttribute(
      "value",
      "43",
    );
    await expect(page.locator(".home-reading-footer a")).toHaveAttribute(
      "href",
      "/publications/8?resume=1",
    );
    const main = await page.locator(".hub-main").boundingBox();
    expect(main?.width).toBeGreaterThan(width - 65);
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true);
    await page.getByRole("button", { name: "最新发布", exact: true }).click();
    await expect(
      page.locator(".home-content-gallery .hub-content-card").first(),
    ).toContainText(titles[0]);
    await page.getByRole("button", { name: "精选优先", exact: true }).click();
    await expect(
      page.locator(".home-content-gallery .hub-content-card").first(),
    ).toContainText(titles[2]);
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.screenshot({
      path: testInfo.outputPath(`home-gallery-${width}.png`),
      fullPage: true,
    });
    await page.screenshot({
      path: testInfo.outputPath(`home-first-screen-${width}.png`),
    });
  }
});

test("reading and access badges share semantic styles including completed articles", async ({
  page,
}, info) => {
  await mockMemberSession(page);
  const data = homeFixture();
  data.continueReading[0].progress!.percent = 100;
  data.latest[4].progress = data.continueReading[0].progress;
  await page.route("**/api/v1/home", (route) =>
    fulfillResult(route, { json: data }),
  );
  await page.route("**/editorial-preview/*.png", (route) =>
    route.fulfill({
      contentType: "image/png",
      path: resolve(
        "../backend/scripts/editorial-covers",
        new URL(route.request().url()).pathname.split("/").at(-1)!,
      ),
    }),
  );
  await page.goto("/home");
  const widget = page.getByLabel("个人阅读", { exact: true });
  await expect(widget.locator('[data-tone="complete"]')).toHaveText(
    "已读完100%",
  );
  await expect(widget.getByRole("link", { name: "再读一次" })).toBeVisible();
  await expect(widget.getByRole("progressbar")).toHaveAttribute("value", "100");
  await expect(
    page.locator('.home-practice-gallery [data-tone="free"]').first(),
  ).toContainText("免费阅读");
  await expect(
    page.locator('.home-practice-gallery [data-tone="credit"]').first(),
  ).toContainText("积分");
  await expect(
    page.locator('.home-practice-gallery [data-tone="preview"]').first(),
  ).toHaveCount(0);
  await expect(
    page.locator('.home-practice-gallery [data-tone="complete"]'),
  ).toHaveText("已读完100%");
  await page.screenshot({
    path: info.outputPath("semantic-reading-badges.png"),
    fullPage: true,
  });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});
