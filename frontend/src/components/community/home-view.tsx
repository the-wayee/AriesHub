"use client";
import Link from "next/link";
import { ArrowRight, BookOpen } from "lucide-react";
import { useRef, useState } from "react";
import gsap from "gsap";
import { useGSAP } from "@gsap/react";
import { useAuthSession } from "../auth-session";
import { useReaderResource } from "../use-reader-resource";
import {
  PublishedContentCard,
  PublishedCover,
} from "../published-content-card";
import { PublicationMetrics } from "../publication-metrics";
import { PublicationInteractions } from "../publication-interactions";
import { PageSkeleton } from "../page-skeleton";
import { type MemberHome } from "@/lib/publication-reader";
import { CommunityPreviewRail, CommunityPulse } from "./community-widgets";
import { CommunityActivityFeed } from "./community-activity";
import { Button } from "../ui/button";
import {
  PublicationFormBadge,
  PublicationFeaturedBadge,
  PublicationAccessBadge,
  PublicationProgressBadge,
} from "../publication-badges";
gsap.registerPlugin(useGSAP);

/** 通栏首页：文章和阅读进度来自真实聚合接口，社区动态使用独立的跨领域接口。 */
export function HomeView() {
  const { user } = useAuthSession();
  const [retry, setRetry] = useState(0);
  const [selection, setSelection] = useState<"featured" | "latest">("featured");
  const { data, error } = useReaderResource<MemberHome>(
    user ? "/api/v1/home" : null,
    retry,
  );
  const galleryRoot = useRef<HTMLElement>(null);
  const previousPositions = useRef(new Map<string, DOMRect>());
  const previousIndicator = useRef<{ x: number; width: number } | null>(null);
  const chooseSelection = (next: "featured" | "latest") => {
    if (next === selection) return;
    // 保留旧视觉坐标；数据重排后从当前位置移动到新位置，快速切换也能连续衔接。
    previousPositions.current = new Map(
      Array.from(
        galleryRoot.current?.querySelectorAll<HTMLElement>(
          ".home-content-gallery > article",
        ) ?? [],
        (node) => [node.dataset.publicationId!, node.getBoundingClientRect()],
      ),
    );
    const indicator = galleryRoot.current?.querySelector<HTMLElement>(
      ".home-gallery-indicator",
    );
    if (indicator)
      previousIndicator.current = {
        x: Number(gsap.getProperty(indicator, "x")),
        width: indicator.getBoundingClientRect().width,
      };
    setSelection(next);
  };
  useGSAP(
    () => {
      const root = galleryRoot.current;
      const selected = root?.querySelector<HTMLElement>(
        '.home-gallery-tabs button[aria-pressed="true"]',
      );
      const tabs = root?.querySelector<HTMLElement>(".home-gallery-tabs");
      if (!root || !selected || !tabs) return;
      const reduced = window.matchMedia(
        "(prefers-reduced-motion: reduce)",
      ).matches;
      const rect = selected.getBoundingClientRect();
      const tabsRect = tabs.getBoundingClientRect();
      // 上一轮清理会还原样式，先恢复点击时的位置，再接着移动下划线。
      if (previousIndicator.current)
        gsap.set(".home-gallery-indicator", previousIndicator.current);
      previousIndicator.current = null;
      gsap.to(".home-gallery-indicator", {
        x: rect.left - tabsRect.left,
        width: rect.width,
        duration: reduced ? 0 : 0.3,
        ease: "power2.out",
      });
      if (!reduced)
        for (const node of root.querySelectorAll<HTMLElement>(
          ".home-content-gallery > article",
        )) {
          const previous = previousPositions.current.get(
            node.dataset.publicationId!,
          );
          if (!previous) continue;
          const next = node.getBoundingClientRect();
          gsap.fromTo(
            node,
            { x: previous.left - next.left, y: previous.top - next.top },
            { x: 0, y: 0, duration: 0.45, ease: "power3.out" },
          );
        }
      previousPositions.current.clear();
    },
    {
      scope: galleryRoot,
      dependencies: [selection, data],
      revertOnUpdate: true,
    },
  );
  const spotlight = data?.featured[0] ?? data?.latest[0];
  const reading = data?.continueReading[0];
  // 按文章 ID 去重，避免精选与最新发布中的同一内容重复占位。
  const gallery = data
    ? Array.from(
        new Map(
          (selection === "featured"
            ? [...data.featured, ...data.latest]
            : [...data.latest, ...data.featured]
          ).map((item) => [item.publication.id, item]),
        ).values(),
      )
    : [];
  return (
    <>
      <header className="home-explore-heading">
        <div>
          <h1>今天，继续你的探索</h1>
          <p>
            {user?.nickname ? `${user.nickname}，欢迎回来。` : "欢迎回来。"}
          </p>
        </div>
        <nav className="home-topic-nav" aria-label="探索主题">
          <Link href="/discover" className="home-topic-all">
            全部内容
          </Link>
          {data?.categories.map((c) => (
            <Link
              key={c.id}
              href={`/discover?category=${encodeURIComponent(c.slug)}`}
            >
              <i
                aria-hidden="true"
                className="category-color-marker"
                style={{ backgroundColor: c.color }}
              />
              {c.name}
            </Link>
          ))}
        </nav>
      </header>
      {error ? (
        <div className="hub-empty" role="alert">
          <h2>首页暂时无法读取</h2>
          <p>{error}</p>
          <Button onClick={() => setRetry(retry + 1)}>重新加载</Button>
        </div>
      ) : !data ? (
        <PageSkeleton variant="gallery" />
      ) : (
        <div className="home-gallery-layout">
          <div className="home-gallery-main">
            <div className="home-lead-grid">
              {spotlight ? (
                <section
                  className="community-home-spotlight"
                  aria-label="今日阅读"
                >
                  <Link
                    className="community-spotlight-art"
                    href={`/publications/${spotlight.publication.id}`}
                  >
                    <PublishedCover
                      key={spotlight.publication.coverFileId}
                      item={spotlight}
                      priority
                    />
                    {spotlight.publication.featured && (
                      <PublicationFeaturedBadge />
                    )}
                  </Link>
                  <div className="community-spotlight-copy">
                    <h2 className="home-feature-label">
                      {spotlight.publication.featured
                        ? "主理人精选"
                        : "最新内容"}
                    </h2>
                    <Link
                      className="home-feature-title"
                      href={`/publications/${spotlight.publication.id}`}
                    >
                      {spotlight.publication.title}
                    </Link>
                    <p>{spotlight.publication.summary}</p>
                    <div className="home-feature-tags">
                      <PublicationFormBadge
                        minimal
                        type={spotlight.publication.publicationType}
                      />
                      <PublicationAccessBadge
                        minimal
                        access={spotlight.publication.accessType}
                        credits={spotlight.publication.creditPrice}
                      />
                    </div>
                    <small>
                      {spotlight.publication.categoryName} ·{" "}
                      {spotlight.publication.publishedAt.slice(0, 10)}
                    </small>
                    <Link
                      className="hub-primary"
                      href={`/publications/${spotlight.publication.id}`}
                    >
                      阅读文章 <ArrowRight />
                    </Link>
                    <PublicationMetrics
                      id={spotlight.publication.id}
                      initial={spotlight.interaction}
                    />
                    <PublicationInteractions
                      id={spotlight.publication.id}
                      initial={spotlight.interaction}
                    />
                  </div>
                </section>
              ) : (
                <section className="hub-empty">
                  <h2>还没有已发布内容</h2>
                  <p>新的实践与文章会出现在这里。</p>
                </section>
              )}
              <div className="home-personal-column">
                <section className="home-reading-widget" aria-label="个人阅读">
                  <div className="hub-rail-heading">
                    <h2>{reading ? "继续阅读" : "开始一次阅读"}</h2>
                    <Link href="/my-content">
                      阅读记录 <ArrowRight />
                    </Link>
                  </div>
                  {reading ? (
                    <>
                      <Link
                        className="home-reading-cover"
                        href={`/publications/${reading.publication.id}${reading.progress?.percent === 100 ? "" : "?resume=1"}`}
                      >
                        <PublishedCover
                          key={reading.publication.coverFileId}
                          item={reading}
                        />
                      </Link>
                      <h3>{reading.publication.title}</h3>
                      <progress
                        className="publication-reading-bar"
                        data-complete={reading.progress?.percent === 100}
                        value={reading.progress?.percent ?? 0}
                        max={100}
                        aria-label="上次阅读进度"
                      />
                      <div className="home-reading-footer">
                        <PublicationProgressBadge
                          percent={reading.progress?.percent ?? 0}
                          label="上次读到"
                        />
                        <Link
                          href={`/publications/${reading.publication.id}${reading.progress?.percent === 100 ? "" : "?resume=1"}`}
                        >
                          {reading.progress?.percent === 100
                            ? "再读一次"
                            : "继续阅读"}{" "}
                          <ArrowRight />
                        </Link>
                      </div>
                    </>
                  ) : (
                    <div className="home-reading-empty">
                      <BookOpen />
                      <p>读一篇感兴趣的文章，下次从这里接着看。</p>
                      <Link href="/discover">
                        发现下一份实践 <ArrowRight />
                      </Link>
                    </div>
                  )}
                </section>
                <CommunityPulse />
              </div>
            </div>
            <section
              ref={galleryRoot}
              className="home-practice-gallery"
              aria-label="文章画廊"
            >
              <div className="hub-section-heading">
                <h2 className="community-title community-title-blue">
                  值得收藏的实践
                </h2>
                <div className="home-gallery-tabs" aria-label="文章排序">
                  <Button
                    variant="ghost"
                    aria-pressed={selection === "featured"}
                    onClick={() => chooseSelection("featured")}
                  >
                    精选优先
                  </Button>
                  <Button
                    variant="ghost"
                    aria-pressed={selection === "latest"}
                    onClick={() => chooseSelection("latest")}
                  >
                    最新发布
                  </Button>
                  <span className="home-gallery-indicator" aria-hidden="true" />
                </div>
                <Link href="/discover">
                  查看全部 <ArrowRight />
                </Link>
              </div>
              <div className="home-content-gallery">
                {gallery.map((item) => (
                  <PublishedContentCard
                    key={item.publication.id}
                    item={item}
                    categoryColor={
                      data.categories.find(
                        (category) =>
                          category.slug === item.publication.categorySlug,
                      )?.color
                    }
                    minimal
                  />
                ))}
              </div>
              {!gallery.length && (
                <p className="hub-empty">内容正在整理，稍后再来看看。</p>
              )}
            </section>
            {data.continueReading.length > 1 && (
              <section className="home-more-reading">
                <div className="hub-section-heading">
                  <h2>你的阅读足迹</h2>
                  <Link href="/my-content">
                    全部记录 <ArrowRight />
                  </Link>
                </div>
                <div className="home-content-gallery">
                  {data.continueReading.slice(1).map((item) => (
                    <PublishedContentCard
                      minimal
                      key={item.publication.id}
                      item={item}
                      categoryColor={
                        data.categories.find(
                          (category) =>
                            category.slug === item.publication.categorySlug,
                        )?.color
                      }
                    />
                  ))}
                </div>
              </section>
            )}
          </div>
          <aside className="home-community-sidebar" aria-label="社区活动与推荐">
            <CommunityActivityFeed />
            <CommunityPreviewRail />
            <section>
              <div className="hub-rail-heading">
                <h2 className="community-title community-title-green">
                  探索主题
                </h2>
                <Link href="/discover">全部主题</Link>
              </div>
              {data.categories.map((c) => (
                <Link
                  key={c.id}
                  className="reader-theme-link"
                  href={`/discover?category=${encodeURIComponent(c.slug)}`}
                >
                  <i
                    aria-hidden="true"
                    className="category-color-marker"
                    style={{ backgroundColor: c.color }}
                  />
                  {c.name}
                  <span>{c.publicationCount} 篇</span>
                </Link>
              ))}
            </section>
          </aside>
        </div>
      )}
    </>
  );
}
