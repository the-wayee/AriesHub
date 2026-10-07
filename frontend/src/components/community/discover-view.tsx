"use client";
import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "../ui/button";
import { useReaderResource } from "../use-reader-resource";
import { PublishedContentCard } from "../published-content-card";
import { PageSkeleton } from "../page-skeleton";
import type { Category } from "@/lib/catalog-types";
import type { ReaderPage } from "@/lib/publication-reader";
/** 筛选写入 URL，刷新/分享保留状态；服务端完成检索、排序与分页。 */
export function DiscoverView({
  initialQuery = "",
  category = "",
  type = "",
  access = "",
  page = 1,
}: {
  initialQuery?: string;
  category?: string;
  type?: string;
  access?: string;
  page?: number;
}) {
  const router = useRouter();
  const [retry, setRetry] = useState(0);
  const params = new URLSearchParams({
    q: initialQuery,
    category,
    type,
    access,
    page: String(page),
    size: "9",
    sort: "LATEST",
  });
  const { data, error } = useReaderResource<ReaderPage>(
    `/api/v1/publications/cards?${params}`,
    retry,
  );
  const themes = useReaderResource<Category[]>("/api/v1/categories");
  function filter(name: string, value: string) {
    const next = new URLSearchParams(params);
    next.set(name, value);
    if (name !== "page") next.set("page", "1");
    next.delete("size");
    next.delete("sort");
    router.push(`/discover?${next}`);
  }
  return (
    <>
      <header className="hub-page-heading">
        <p className="hub-kicker">EXPLORE</p>
        <h1>
          {initialQuery ? (
            `关于「${initialQuery}」`
          ) : (
            <>
              从别人的实践里，
              <br />
              <span>找到自己的下一步。</span>
            </>
          )}
        </h1>
        <p>真实发布的实战案例、学习文章和课程。</p>
      </header>
      <div className="hub-filterbar">
        <div className="hub-pills">
          <Button
            variant="ghost"
            disabled={!data && !error}
            aria-pressed={!category}
            onClick={() => filter("category", "")}
          >
            全部主题
          </Button>
          {themes.data?.map((c) => (
            <Button
              key={c.id}
              variant="ghost"
              disabled={!data && !error}
              aria-pressed={category === c.slug}
              onClick={() => filter("category", c.slug)}
            >
              <i
                aria-hidden="true"
                className="category-color-marker"
                style={{ backgroundColor: c.color }}
              />
              {c.name}
            </Button>
          ))}
        </div>
        <Link href="/my-content">我的收藏</Link>
      </div>
      <div className="reader-filter-selects">
        <label>
          内容形式
          <select
            aria-label="内容形式筛选"
            disabled={!data && !error}
            value={type}
            onChange={(e) => filter("type", e.target.value)}
          >
            <option value="">全部形式</option>
            <option value="CASE_STUDY">实战案例</option>
            <option value="ARTICLE">学习文章</option>
            <option value="COURSE">课程</option>
          </select>
        </label>
        <label>
          阅读方式
          <select
            aria-label="阅读方式筛选"
            disabled={!data && !error}
            value={access}
            onChange={(e) => filter("access", e.target.value)}
          >
            <option value="">全部方式</option>
            <option value="FREE">免费阅读</option>
            <option value="CREDIT">积分内容</option>
          </select>
        </label>
      </div>
      {error ? (
        <div className="hub-empty" role="alert">
          <h2>内容暂时无法读取</h2>
          <p>{error}</p>
          <Button onClick={() => setRetry(retry + 1)}>重新加载</Button>
        </div>
      ) : !data ? (
        <PageSkeleton variant="gallery" />
      ) : data.items.length ? (
        <>
          <div className="hub-discover-grid">
            {data.items.map((p) => (
              <PublishedContentCard item={p} key={p.publication.id} />
            ))}
          </div>
          <div className="reader-pagination">
            <Button
              disabled={page <= 1}
              onClick={() => filter("page", String(page - 1))}
            >
              上一页
            </Button>
            <span>
              第 {page} 页 · 共 {data.total} 篇
            </span>
            <Button
              disabled={page >= data.totalPages}
              onClick={() => filter("page", String(page + 1))}
            >
              下一页
            </Button>
          </div>
        </>
      ) : (
        <div className="hub-empty">
          <h2>没有找到匹配的内容</h2>
          <p>换一个主题或关键词再试试。</p>
        </div>
      )}
    </>
  );
}
