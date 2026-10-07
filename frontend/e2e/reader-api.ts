import { fulfillResult } from "./api-result";
import type { Page } from "@playwright/test";
import type {
  PublicationCardData,
  ReadingProgress,
} from "../src/lib/publication-reader";
/** 请求层假实现保留服务端关系，重载页面不能依赖 localStorage。 */
export async function mockReaderApi(page: Page) {
  const liked = new Set<string>(),
    bookmarked = new Set<string>();
  const progress = new Map<string, ReadingProgress>();
  const titles = ["AI 需求实践", "AI 演示实践", "自动化工作流", "个人知识库"];
  const categories = ["coding", "slides", "automation", "automation"];
  const ids = ["4", "7", "9", "10"];
  const state = (id: string) => ({
    publicationId: id,
    liked: liked.has(id),
    bookmarked: bookmarked.has(id),
    likeCount: liked.has(id) ? 1 : 0,
  });
  const cards = (): PublicationCardData[] =>
    ids.map((id, i) => ({
      publication: {
        id,
        slug: `legacy-${id}`,
        title: titles[i],
        summary: "真实请求契约中的练习摘要",
        categoryName: i === 0 ? "AI 编程" : i === 1 ? "AI 演示" : "日常自动化",
        categorySlug: categories[i],
        publicationType: i === 3 ? "ARTICLE" : "CASE_STUDY",
        accessType: "FREE",
        creditPrice: 0,
        publishedAt: "2026-10-07T00:00:00Z",
        coverFileId: null,
        featured: i === 0,
      },
      cover: null,
      interaction: state(id),
      progress: progress.get(id) ?? null,
    }));
  await page.route("**/api/v1/categories", (route) =>
    fulfillResult(route, {
      json: [
        { id: "1", slug: "coding", name: "AI 编程", publicationCount: 1 },
        { id: "2", slug: "slides", name: "AI 演示", publicationCount: 1 },
        {
          id: "3",
          slug: "automation",
          name: "日常自动化",
          publicationCount: 2,
        },
      ],
    }),
  );
  await page.route("**/api/v1/publications/cards?*", (route) => {
    const q = new URL(route.request().url()).searchParams;
    let items = cards().filter(
      (c) =>
        (!q.get("category") ||
          c.publication.categorySlug === q.get("category")) &&
        (!q.get("q") || c.publication.title.includes(q.get("q")!)) &&
        (!q.get("type") || c.publication.publicationType === q.get("type")) &&
        (!q.get("access") || c.publication.accessType === q.get("access")),
    );
    const total = items.length,
      size = Number(q.get("size") ?? 9),
      n = Number(q.get("page") ?? 1);
    items = items.slice((n - 1) * size, n * size);
    return fulfillResult(route, {
      json: {
        items,
        page: n,
        size,
        total,
        totalPages: Math.ceil(total / size),
      },
    });
  });
  await page.route("**/api/v1/home", (route) =>
    fulfillResult(route, {
      json: {
        categories: [
          { id: "1", slug: "coding", name: "AI 编程", publicationCount: 1 },
        ],
        featured: cards().slice(0, 1),
        latest: cards(),
        continueReading: cards().filter((c) => c.progress),
      },
    }),
  );
  await page.route("**/api/v1/users/me/library?*", (route) => {
    const kind = new URL(route.request().url()).searchParams.get("kind");
    const items = cards().filter((c) =>
      kind === "LIKE"
        ? liked.has(c.publication.id)
        : kind === "HISTORY"
          ? progress.has(c.publication.id)
          : bookmarked.has(c.publication.id),
    );
    return fulfillResult(route, {
      json: {
        items,
        page: 1,
        size: 9,
        total: items.length,
        totalPages: items.length ? 1 : 0,
      },
    });
  });
  await page.route(
    /\/api\/v1\/publications\/\d+\/(interaction|bookmark|like|reading-progress)$/,
    (route) => {
      const path = new URL(route.request().url()).pathname.split("/");
      const id = path.at(-2)!,
        kind = path.at(-1)!;
      if (kind === "reading-progress") {
        if (route.request().method() === "PUT")
          progress.set(id, {
            ...route.request().postDataJSON(),
            publicationId: id,
            updatedAt: new Date().toISOString(),
          });
        return fulfillResult(route, { json: progress.get(id) ?? null });
      }
      if (kind !== "interaction") {
        const set = kind === "like" ? liked : bookmarked;
        if (route.request().method() === "PUT") set.add(id);
        else set.delete(id);
      }
      return fulfillResult(route, { json: state(id) });
    },
  );
  return { liked, bookmarked, progress };
}
