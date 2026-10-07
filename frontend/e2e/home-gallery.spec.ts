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
      bookmarkCount: index + 2,
      shareCount: index + 4,
      viewCount: 100 + index * 17,
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
    await expect(
      page.locator(".home-content-gallery .publication-featured-badge"),
    ).toHaveCount(3);
    await expect(
      page.locator(
        '.home-content-gallery [data-publication-id="7"] .publication-featured-badge',
      ),
    ).toHaveCount(0);
    await page.getByRole("button", { name: "最新发布", exact: true }).click();
    await expect(
      page.locator(".home-content-gallery .hub-content-card").first(),
    ).toContainText(titles[0]);
    await page.getByRole("button", { name: "精选优先", exact: true }).click();
    await expect(
      page.locator(".home-content-gallery .hub-content-card").first(),
    ).toContainText(titles[2]);
    await page.waitForTimeout(500);
    const selected = await page
      .getByRole("button", { name: "精选优先", exact: true })
      .boundingBox();
    const indicator = await page
      .locator(".home-gallery-indicator")
      .boundingBox();
    expect(Math.abs((selected?.x ?? 0) - (indicator?.x ?? 0))).toBeLessThan(1);
    expect(
      Math.abs((selected?.width ?? 0) - (indicator?.width ?? 0)),
    ).toBeLessThan(1);
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

test("community feed shows three rows and loops with typed icons", async ({
  page,
}, info) => {
  test.setTimeout(90000);
  await mockMemberSession(page);
  await page.route("**/api/v1/home", (route) =>
    fulfillResult(route, { json: homeFixture() }),
  );
  const row = (id: number, kind = "PUBLICATION_LIKE") => ({
    id: String(id),
    actorId: "9001",
    actorName: `成员 ${id}`,
    actorAvatarUrl: "/concepts/automation-color.webp",
    kind,
    title: `实践记录 ${id}`,
    content: kind.startsWith("DISCUSSION_") ? `这是成员 ${id}的发言` : null,
    href: `/publications/11`,
    createdAt: new Date().toISOString(),
  });
  const kinds = [
    "MEMBER_JOINED",
    "PUBLICATION_PUBLISHED",
    "DISCUSSION_COMMENTED",
    "PUBLICATION_LIKE",
    "PUBLICATION_BOOKMARK",
    "DISCUSSION_REPLIED",
    "PUBLICATION_SHARE",
    "DISCUSSION_LIKED",
  ];
  const first = Array.from({ length: 6 }, (_, index) =>
    row(100 - index, kinds[index % kinds.length]),
  );
  let olderAttempts = 0;
  const requests: string[] = [];
  await page.route("**/api/v1/community/activities?*", (route) => {
    const url = new URL(route.request().url());
    requests.push(url.search);
    if (url.searchParams.get("filter") === "MEMBERS")
      return fulfillResult(route, {
        json: {
          items: [
            {
              ...row(120, "MEMBER_JOINED"),
              href: null,
              title: "欢迎来到 AriesHub",
            },
          ],
          nextCursor: null,
        },
      });
    if (url.searchParams.get("filter") === "COMMENTS")
      return fulfillResult(route, {
        json: {
          items: [
            {
              ...row(121, "DISCUSSION_REPLIED"),
              href: "/publications/11#comments",
            },
          ],
          nextCursor: null,
        },
      });
    if (url.searchParams.get("filter") === "PUBLICATIONS")
      return fulfillResult(route, { json: { items: [], nextCursor: null } });
    if (url.searchParams.get("before")) {
      olderAttempts++;
      if (olderAttempts === 1)
        return fulfillResult(route, {
          status: 503,
          json: { code: "TEMPORARILY_UNAVAILABLE", msg: "动态暂时无法加载" },
        });
      return fulfillResult(route, {
        json: {
          items: [
            row(94, "PUBLICATION_SHARE"),
            row(93, "PUBLICATION_BOOKMARK"),
          ],
          nextCursor: null,
        },
      });
    }
    return fulfillResult(route, { json: { items: first, nextCursor: "95" } });
  });
  await page.clock.install();
  await page.goto("/home");
  const feed = page.getByRole("region", { name: "社区动态", exact: true });
  await feed.scrollIntoViewIfNeeded();
  await expect(feed.locator("article")).toHaveCount(4);
  await expect(
    page.locator(".home-explore-heading").getByLabel("社区动态"),
  ).toHaveCount(0);
  await expect(
    feed.getByRole("img", { name: "成员 100的头像" }).locator("img"),
  ).toHaveAttribute("src", /automation-color/);
  const visibleRows = feed.locator('article:not([aria-hidden="true"])');
  await expect(visibleRows).toHaveCount(3);
  await expect(feed.locator(".community-event-icon svg")).toHaveCount(4);
  await expect(
    feed.getByRole("button", { name: /暂停动态|播放动态|下一条动态/ }),
  ).toHaveCount(0);
  const track = feed.locator(".community-feed-track");
  const offset = () =>
    track.evaluate(
      (node) => new DOMMatrixReadOnly(getComputedStyle(node).transform).m42,
    );
  await page.mouse.move(0, 0);
  const before = await offset();
  await page.waitForTimeout(500);
  expect(await offset()).toBeLessThan(before);
  await feed.hover();
  const stopped = await offset();
  await page.waitForTimeout(700);
  expect(await offset()).toBeCloseTo(stopped, 1);
  await page.mouse.move(0, 0);
  await page.waitForTimeout(500);
  expect(await offset()).toBeLessThan(stopped);
  // 加速浏览器时钟验收完整循环，仍使用真实轨道动画而非手动操作按钮。
  for (let step = 0; step < 1; step++) {
    const previous = await visibleRows.first().textContent();
    await page.clock.runFor(5000);
    await expect(visibleRows.first()).not.toHaveText(previous!);
  }
  await expect(feed.getByRole("alert")).toContainText("动态暂时无法加载");
  await feed.getByRole("button", { name: "重试加载" }).click();
  await expect(feed.getByRole("alert")).toHaveCount(0);
  await page.mouse.move(0, 0);
  await page
    .locator(".home-explore-heading")
    .click({ position: { x: 5, y: 5 } });
  for (let step = 0; step < 7; step++) {
    const previous = await visibleRows.first().textContent();
    await page.clock.runFor(5000);
    await expect(visibleRows.first()).not.toHaveText(previous!);
  }
  expect(requests.filter((query) => query.includes("before=95"))).toHaveLength(
    2,
  );
  await expect(visibleRows).toHaveCount(3);
  expect(
    await feed.getByLabel("社区动态轮播").evaluate((node) => node.clientHeight),
  ).toBe(276);
  await feed.getByRole("tab", { name: "新成员" }).click();
  await expect(feed.locator("article")).toHaveCount(1);
  await expect(feed).toContainText("加入了社区");
  await expect(feed.locator("article a")).toHaveCount(0);
  await feed.getByRole("tab", { name: "评论与回复" }).click();
  await expect(feed).toContainText("回复了");
  await expect(feed).toContainText("这是成员 121的发言");
  await expect(feed).not.toContainText("实践记录 121");
  await expect(feed.locator("article a")).toHaveAttribute(
    "href",
    "/publications/11#comments",
  );
  await feed.getByRole("tab", { name: "新文章" }).click();
  await expect(feed).toContainText("这里还没有动态");
  await feed.getByRole("tab", { name: "新文章" }).press("ArrowRight");
  await expect(
    feed.getByRole("tab", { name: "互动", exact: true }),
  ).toHaveAttribute("aria-selected", "true");
  await expect(feed.locator("article")).toHaveCount(4);
  // 视觉验收等待入场动画结束，避免截图只捕获到淡入过程中的低透明度。
  await feed.locator("article").evaluateAll(async (nodes) => {
    await Promise.all(
      nodes.flatMap((node) =>
        node.getAnimations().map((animation) => animation.finished),
      ),
    );
  });
  await feed.hover();
  await page.screenshot({ path: info.outputPath("community-feed.png") });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});

test("community filtering waits for its one-second animation without collapsing the feed", async ({
  page,
}, info) => {
  await mockMemberSession(page);
  await page.route("**/api/v1/home", (route) =>
    fulfillResult(route, { json: homeFixture() }),
  );
  const rows = (prefix: string) =>
    Array.from({ length: 3 }, (_, index) => ({
      id: String(300 + index),
      actorId: "9001",
      actorName: "读者",
      actorAvatarUrl: null,
      kind: "DISCUSSION_COMMENTED",
      title: "文章标题",
      content: `${prefix} ${index}`,
      href: "/publications/11#comments",
      createdAt: new Date().toISOString(),
    }));
  let requests = 0;
  let releaseSlow!: () => void;
  const slow = new Promise<void>((resolve) => {
    releaseSlow = resolve;
  });
  await page.route("**/api/v1/community/activities?*", async (route) => {
    requests++;
    if (requests === 3) await slow;
    if (requests === 4)
      return fulfillResult(route, {
        status: 503,
        json: { code: "TEMPORARILY_UNAVAILABLE", msg: "刷新暂时失败" },
      });
    return fulfillResult(route, {
      json: {
        items: rows(
          requests === 1
            ? "原有动态"
            : requests === 2
              ? "更新后的发言"
              : "慢请求动态",
        ),
        nextCursor: null,
      },
    });
  });
  await page.goto("/home");
  const feed = page.getByRole("region", { name: "社区动态", exact: true });
  const commentsTab = feed.getByRole("tab", {
    name: "评论与回复",
    exact: true,
  });
  const allTab = feed.getByRole("tab", { name: "全部动态", exact: true });
  await expect(feed.getByRole("button", { name: "刷新社区动态" })).toHaveCount(
    0,
  );
  await expect(feed).toContainText("原有动态");
  await feed.hover();
  const bounds = () =>
    feed.evaluate((node) => ({
      height: node.getBoundingClientRect().height,
      nextTop: node.nextElementSibling
        ? node.nextElementSibling.getBoundingClientRect().top + window.scrollY
        : null,
    }));
  const initialBounds = await bounds();
  const panel = feed.getByLabel("社区动态轮播");
  const initialHeight = await panel.evaluate((node) => node.clientHeight);
  const started = Date.now();
  const response = page.waitForResponse((response) =>
    response.url().includes("/community/activities"),
  );
  await commentsTab.click();
  await response;
  await expect(
    feed.locator('.community-refresh-feedback[data-phase="loading"]'),
  ).toBeVisible();
  expect(await feed.locator(".community-refresh-feedback").textContent()).toBe(
    "",
  );
  expect(
    await feed
      .locator(".community-refresh-symbol")
      .evaluate((node) => node.getBoundingClientRect().width),
  ).toBeGreaterThan(60);
  await page.waitForTimeout(250);
  await expect(feed).toContainText("原有动态");
  await expect(feed).not.toContainText("更新后的发言");
  expect(await panel.evaluate((node) => node.clientHeight)).toBe(initialHeight);
  expect(await bounds()).toEqual(initialBounds);
  await feed.screenshot({
    path: info.outputPath("community-refresh-loading.png"),
  });
  await expect(
    feed.locator('.community-refresh-feedback[data-phase="complete"]'),
  ).toBeVisible();
  await expect(feed).toContainText("原有动态");
  await feed.screenshot({
    path: info.outputPath("community-refresh-complete.png"),
  });
  await expect(feed).toContainText("更新后的发言");
  await expect(feed.locator(".community-refresh-feedback")).toHaveCount(0);
  expect(Date.now() - started).toBeGreaterThanOrEqual(1000);
  expect(await bounds()).toEqual(initialBounds);
  expect(requests).toBe(2);

  // 慢接口必须等响应，动画完成不能先清空旧数据；失败也保留现有内容和高度。
  await allTab.click();
  await page.waitForTimeout(1200);
  await expect(
    feed.locator('.community-refresh-feedback[data-phase="loading"]'),
  ).toBeVisible();
  await expect(feed).toContainText("更新后的发言");
  expect(await bounds()).toEqual(initialBounds);
  releaseSlow();
  await expect(feed).toContainText("慢请求动态");
  await commentsTab.click();
  await expect(feed.getByRole("alert")).toContainText("刷新暂时失败");
  await expect(feed).toContainText("慢请求动态");
  expect(await bounds()).toEqual(initialBounds);
});

test("community tabs animate in place and discard a superseded category response", async ({
  page,
}) => {
  await mockMemberSession(page);
  await page.route("**/api/v1/home", (route) =>
    fulfillResult(route, { json: homeFixture() }),
  );
  let releaseMembers!: () => void;
  const members = new Promise<void>((resolve) => {
    releaseMembers = resolve;
  });
  await page.route("**/api/v1/community/activities?*", async (route) => {
    const filter = new URL(route.request().url()).searchParams.get("filter");
    if (filter === "MEMBERS") await members;
    const name =
      filter === "COMMENTS"
        ? "评论分类中的发言"
        : filter === "MEMBERS"
          ? "过期的新成员响应"
          : "原有列表中的发言";
    return fulfillResult(route, {
      json: {
        items: [
          {
            id: "800",
            actorId: "9001",
            actorName: "读者",
            actorAvatarUrl: null,
            kind: "DISCUSSION_COMMENTED",
            title: "文章标题",
            content: name,
            href: "/publications/11#comments",
            createdAt: new Date().toISOString(),
          },
        ],
        nextCursor: null,
      },
    });
  });
  await page.goto("/home");
  const feed = page.getByRole("region", { name: "社区动态", exact: true });
  await expect(feed).toContainText("原有列表中的发言");
  await feed.scrollIntoViewIfNeeded();
  const height = await feed.evaluate(
    (node) => node.getBoundingClientRect().height,
  );
  await expect(feed.getByRole("button", { name: "刷新社区动态" })).toHaveCount(
    0,
  );
  await feed.getByRole("tab", { name: "新成员", exact: true }).click();
  await expect(
    feed.locator('.community-refresh-feedback[data-phase="loading"]'),
  ).toBeVisible();
  await feed.getByRole("tab", { name: "评论与回复", exact: true }).click();
  await expect(feed).toContainText("原有列表中的发言");
  expect(
    await feed.evaluate((node) => node.getBoundingClientRect().height),
  ).toBe(height);
  await expect(
    feed.locator('.community-refresh-feedback[data-phase="complete"]'),
  ).toBeVisible();
  await expect(feed).toContainText("评论分类中的发言");
  await expect(feed.locator(".community-refresh-feedback")).toHaveCount(0);
  releaseMembers();
  await page.waitForTimeout(300);
  await expect(feed).not.toContainText("过期的新成员响应");
  await expect(
    feed.getByRole("tab", { name: "评论与回复", exact: true }),
  ).toHaveAttribute("aria-selected", "true");
  expect(
    await feed.evaluate((node) => node.getBoundingClientRect().height),
  ).toBe(height);
});
