"use client";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import {
  ArrowUpRight,
  Star,
  Eye,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import {
  PublicationAccessBadge,
  PublicationFormBadge,
} from "../publication-badges";
import { Button } from "../ui/button";
import { useReaderResource } from "../use-reader-resource";
import {
  PublishedContentCard,
  PublishedCover,
} from "../published-content-card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";
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
    size: "20",
    sort: "LATEST",
  });
  const { data, error } = useReaderResource<ReaderPage>(
    `/api/v1/publications/cards?${params}`,
    retry,
  );
  const themes = useReaderResource<Category[]>("/api/v1/categories");
  // 精选单独按运营排序读取，不把最新分页中的普通文章伪装为精选。
  const featured = useReaderResource<ReaderPage>(
    "/api/v1/publications/cards?sort=FEATURED&size=8&page=1",
  );
  const selected =
    featured.data?.items.filter((item) => item.publication.featured) ?? [];
  const featuredTrack = useRef<HTMLDivElement>(null);
  const [featuredEdges, setFeaturedEdges] = useState({
    previous: false,
    next: false,
  });
  useEffect(() => {
    const track = featuredTrack.current;
    if (!track) return;
    // 监听真实滚动边界，包括触摸、键盘和窗口缩放；不渲染无效方向的箭头。
    const sync = () => {
      const previous = track.scrollLeft > 2;
      const next = track.scrollLeft + track.clientWidth < track.scrollWidth - 2;
      setFeaturedEdges((current) =>
        current.previous === previous && current.next === next
          ? current
          : { previous, next },
      );
    };
    const observer = new ResizeObserver(sync);
    observer.observe(track);
    track.addEventListener("scroll", sync, { passive: true });
    sync();
    return () => {
      observer.disconnect();
      track.removeEventListener("scroll", sync);
    };
  }, [featured.data, featured.error]);
  function slideFeatured(direction: number) {
    const track = featuredTrack.current;
    if (!track) return;
    track.scrollBy({
      left: direction * track.clientWidth * 0.9,
      behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches
        ? "instant"
        : "smooth",
    });
  }
  const gallery = useRef<HTMLDivElement>(null);
  const loadingHeight = useRef(0);
  useEffect(() => {
    const element = gallery.current;
    if (!element) return;
    // 筛选期间锁住原区域高度，骨架不把分页和页脚突然推开。
    element.style.height =
      !data && loadingHeight.current ? `${loadingHeight.current}px` : "";
    let frame = 0;
    // 宽度组合轮换，大图不会固定在左侧；每幅只避让它覆盖的网格列。
    const arrange = () => {
      const columns = window.matchMedia("(max-width: 600px)").matches
        ? 1
        : window.matchMedia("(max-width: 1000px)").matches
          ? 2
          : window.matchMedia("(max-width: 1279px)").matches
            ? 3
            : window.matchMedia("(max-width: 1799px)").matches
              ? 4
              : 5;
      // 60 格可整除多种列数，桌面增列控制封面尺度，宽度组合仍逐组轮换。
      const bottoms = Array<number>(60).fill(0);
      const baseSpans =
        columns === 5
          ? [14, 10, 12, 10, 14]
          : columns === 4
            ? [17, 13, 16, 14]
            : columns === 3
              ? [25, 15, 20]
              : columns === 2
                ? [30, 30]
                : [60];
      const cards = Array.from(element.children) as HTMLElement[];
      cards.forEach((card, index) => {
        const column = index % columns;
        const rotation = Math.floor(index / columns) % columns;
        const spans = [
          ...baseSpans.slice(rotation),
          ...baseSpans.slice(0, rotation),
        ];
        let start = spans.slice(0, column).reduce((sum, span) => sum + span, 0);
        let span = spans[column];
        // 首组建立构图，后续直接寻找能容纳当前宽度的最低位置。
        // 不按“三/四/五幅一行”等待整组结束，短卡片下方可以立即接续。
        if (index >= columns) {
          let best = Infinity;
          const desiredSpan = span;
          // 小幅收放宽度以适应剩余空间，避免仅差一格就跨到下一条高边界。
          const flexibility = columns >= 3 ? 2 : 0;
          for (
            let size = desiredSpan - flexibility;
            size <= Math.min(desiredSpan + flexibility, Math.max(...baseSpans));
            size++
          ) {
            for (let candidate = 0; candidate <= 60 - size; candidate++) {
              const boundary = Math.max(
                ...bottoms.slice(candidate, candidate + size),
              );
              const score = boundary + Math.abs(size - desiredSpan) * 20;
              if (score < best) {
                best = score;
                start = candidate;
                span = size;
              }
            }
          }
        }
        const offset =
          index < columns
            ? columns === 1
              ? 0
              : columns === 2
                ? [0, 20][index]
                : [0, 52, 16, 76, 32][index]
            : 0;
        const top = Math.max(...bottoms.slice(start, start + span)) + offset;
        card.style.gridColumn = `${start + 1} / span ${span}`;
        const height = Math.ceil(card.getBoundingClientRect().height);
        card.style.gridRow = `${top + 1} / span ${height}`;
        // 多列宽卡片取其覆盖列的最高边界，其余区域继续向下接续。
        bottoms.fill(
          top + height + (columns === 1 ? 24 : 16),
          start,
          start + span,
        );
      });
    };
    const schedule = () => {
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(arrange);
    };
    // 字体、封面、阅读状态及窗口变化均重新测量真实高度，避免重叠或留下旧空隙。
    const observer = new ResizeObserver(schedule);
    observer.observe(element);
    Array.from(element.children).forEach((card) => observer.observe(card));
    arrange();
    // 进入视口才播放渐入；未滚动到的文章不提前消耗动效。
    const reveal = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.setAttribute("data-revealed", "true");
            reveal.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.05 },
    );
    if (data)
      Array.from(element.children).forEach((card, index) => {
        const article = card as HTMLElement;
        article.setAttribute("data-revealed", "false");
        article.style.setProperty("--discover-delay", `${(index % 3) * 75}ms`);
        reveal.observe(article);
      });
    return () => {
      observer.disconnect();
      reveal.disconnect();
      cancelAnimationFrame(frame);
    };
  }, [data, error]);
  function filter(name: string, value: string) {
    loadingHeight.current =
      gallery.current?.getBoundingClientRect().height ?? 0;
    const next = new URLSearchParams(params);
    next.set(name, value);
    if (name !== "page") next.set("page", "1");
    next.delete("size");
    next.delete("sort");
    router.push(`/discover?${next}`);
  }
  return (
    <>
      <header className={initialQuery ? "discover-search-heading" : "sr-only"}>
        <h1>{initialQuery ? `关于「${initialQuery}」` : "探索"}</h1>
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
              {c.name}
            </Button>
          ))}
        </div>
        <Link href="/my-content">我的收藏</Link>
      </div>
      {((!featured.data && !featured.error) || selected.length > 0) && (
        <section
          className="discover-featured"
          aria-labelledby="discover-featured-heading"
        >
          <h2 id="discover-featured-heading">
            <Star size={20} aria-hidden="true" />
            <span className="discover-heading-lead">值得</span>
            <span className="discover-heading-emphasis">停下来看的</span>
          </h2>
          <div
            ref={featuredTrack}
            id="discover-featured-track"
            className="discover-featured-track"
            data-count={selected.length || undefined}
          >
            {!featured.data
              ? Array.from({ length: 6 }, (_, index) => (
                  <div
                    className="discover-featured-card"
                    key={index}
                    aria-hidden="true"
                  >
                    <div className="discover-featured-image skeleton-block" />
                    <div className="discover-featured-copy">
                      <span className="skeleton-block discover-featured-title-placeholder" />
                      <span className="skeleton-block discover-skeleton-line" />
                    </div>
                  </div>
                ))
              : selected.map((item) => (
                  <Link
                    className="discover-featured-card"
                    key={item.publication.id}
                    href={`/publications/${item.publication.id}`}
                  >
                    <div className="discover-featured-image">
                      <PublishedCover item={item} />
                    </div>
                    <div className="discover-featured-copy">
                      <div className="discover-featured-type">
                        <PublicationFormBadge
                          type={item.publication.publicationType}
                          minimal
                        />
                        <span>{item.publication.categoryName}</span>
                      </div>
                      <h3>{item.publication.title}</h3>
                      <p className="discover-featured-summary">
                        {item.publication.summary}
                      </p>
                      <div className="discover-featured-footer">
                        <PublicationAccessBadge
                          access={item.publication.accessType}
                          credits={item.publication.creditPrice}
                          minimal
                        />
                        <span className="discover-featured-views">
                          <Eye size={12} aria-hidden="true" />
                          {item.interaction.viewCount ?? 0}
                        </span>
                        <ArrowUpRight
                          size={16}
                          className="discover-featured-arrow"
                          aria-hidden="true"
                        />
                      </div>
                    </div>
                  </Link>
                ))}
          </div>
          {featuredEdges.previous && (
            <Button
              className="discover-featured-control discover-featured-previous"
              variant="ghost"
              size="icon"
              aria-label="查看上一组精选"
              aria-controls="discover-featured-track"
              onClick={() => slideFeatured(-1)}
            >
              <ChevronLeft />
            </Button>
          )}
          {featuredEdges.next && (
            <Button
              className="discover-featured-control discover-featured-next"
              variant="ghost"
              size="icon"
              aria-label="查看下一组精选"
              aria-controls="discover-featured-track"
              onClick={() => slideFeatured(1)}
            >
              <ChevronRight />
            </Button>
          )}
        </section>
      )}
      <div className="discover-content-heading">
        <h2>
          <span className="discover-heading-lead">发现</span>
          <span className="discover-heading-emphasis">下一步灵感</span>
        </h2>
        <div className="reader-filter-selects discover-filters">
          <div>
            <span id="discover-form-label">内容形式</span>
            <Select
              aria-label="内容形式筛选"
              items={{
                "": "全部形式",
                CASE_STUDY: "实战案例",
                ARTICLE: "学习文章",
                COURSE: "课程",
              }}
              disabled={!data && !error}
              value={type}
              onValueChange={(value) => filter("type", value ?? "")}
            >
              <SelectTrigger
                className="discover-filter-trigger"
                aria-label="内容形式筛选"
              >
                <SelectValue placeholder="全部形式" />
              </SelectTrigger>
              <SelectContent
                className="discover-filter-popup"
                align="start"
                alignItemWithTrigger={false}
                sideOffset={8}
              >
                <SelectItem value="">全部形式</SelectItem>
                <SelectItem value="CASE_STUDY">实战案例</SelectItem>
                <SelectItem value="ARTICLE">学习文章</SelectItem>
                <SelectItem value="COURSE">课程</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div>
            <span id="discover-access-label">阅读方式</span>
            <Select
              items={{ "": "全部方式", FREE: "免费阅读", CREDIT: "积分内容" }}
              disabled={!data && !error}
              value={access}
              onValueChange={(value) => filter("access", value ?? "")}
            >
              <SelectTrigger
                className="discover-filter-trigger"
                aria-label="阅读方式筛选"
              >
                <SelectValue placeholder="全部方式" />
              </SelectTrigger>
              <SelectContent
                className="discover-filter-popup"
                align="start"
                alignItemWithTrigger={false}
                sideOffset={8}
              >
                <SelectItem value="">全部方式</SelectItem>
                <SelectItem value="FREE">免费阅读</SelectItem>
                <SelectItem value="CREDIT">积分内容</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
      </div>
      {error ? (
        <div className="hub-empty" role="alert">
          <h2>内容暂时无法读取</h2>
          <p>{error}</p>
          <Button onClick={() => setRetry(retry + 1)}>重新加载</Button>
        </div>
      ) : !data || data.items.length ? (
        <>
          <div
            ref={gallery}
            className={`hub-discover-grid discover-art-gallery${!data ? " page-skeleton discover-loading" : ""}`}
            aria-busy={!data}
            aria-label={!data ? "正在加载文章" : undefined}
          >
            {!data
              ? Array.from({ length: 9 }, (_, index) => (
                  <article
                    className="hub-content-card discover-placeholder"
                    key={`skeleton-${index}`}
                    aria-hidden="true"
                  >
                    <div className="hub-content-image skeleton-block" />
                    <div className="discover-skeleton-copy">
                      <span className="skeleton-block discover-skeleton-type" />
                      <span className="skeleton-block discover-skeleton-title" />
                      <span className="skeleton-block discover-skeleton-line" />
                      <span className="skeleton-block discover-skeleton-line" />
                      <span className="skeleton-block discover-skeleton-meta" />
                    </div>
                  </article>
                ))
              : data.items.map((p) => (
                  <PublishedContentCard
                    item={p}
                    key={p.publication.id}
                    minimal
                    categoryColor={
                      themes.data?.find(
                        (theme) => theme.slug === p.publication.categorySlug,
                      )?.color
                    }
                  />
                ))}
          </div>
          {data && (
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
          )}
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
