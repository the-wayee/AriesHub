"use client";
import Link from "next/link";
import { ArrowRight, BookOpen } from "lucide-react";
import { useState } from "react";
import { useAuthSession } from "../auth-session";
import { useReaderResource } from "../use-reader-resource";
import {
  PublishedContentCard,
  PublishedCover,
} from "../published-content-card";
import { PublicationInteractions } from "../publication-interactions";
import { PageSkeleton } from "../page-skeleton";
import { type MemberHome } from "@/lib/publication-reader";
import {
  CommunityActivityFeed,
  CommunityPreviewRail,
  CommunityPulse,
} from "./community-widgets";
import { Button } from "../ui/button";
import {
  PublicationFormBadge,
  PublicationAccessBadge,
  PublicationProgressBadge,
} from "../publication-badges";
/** 通栏首页：文章和阅读进度来自真实聚合接口，社区示例独立标识。 */
export function HomeView() {
  const { user } = useAuthSession();
  const [retry, setRetry] = useState(0);
  const [selection, setSelection] = useState<"featured" | "latest">("featured");
  const { data, error } = useReaderResource<MemberHome>(
    user ? "/api/v1/home" : null,
    retry,
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
                  </Link>
                  <div className="community-spotlight-copy">
                    <h2 className="home-feature-label">主理人精选</h2>
                    <Link
                      className="home-feature-title"
                      href={`/publications/${spotlight.publication.id}`}
                    >
                      {spotlight.publication.title}
                    </Link>
                    <p>{spotlight.publication.summary}</p>
                    <div className="home-feature-tags">
                      <PublicationFormBadge
                        type={spotlight.publication.publicationType}
                      />
                      <PublicationAccessBadge
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
                <CommunityPulse latest={data.latest[0]} />
              </div>
            </div>
            <section className="home-practice-gallery" aria-label="文章画廊">
              <div className="hub-section-heading">
                <h2 className="community-title community-title-blue">
                  值得收藏的实践
                </h2>
                <div className="home-gallery-tabs" aria-label="文章排序">
                  <Button
                    variant="ghost"
                    aria-pressed={selection === "featured"}
                    onClick={() => setSelection("featured")}
                  >
                    精选优先
                  </Button>
                  <Button
                    variant="ghost"
                    aria-pressed={selection === "latest"}
                    onClick={() => setSelection("latest")}
                  >
                    最新发布
                  </Button>
                </div>
                <Link href="/discover">
                  查看全部 <ArrowRight />
                </Link>
              </div>
              <div className="home-content-gallery">
                {gallery.map((item) => (
                  <PublishedContentCard key={item.publication.id} item={item} />
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
                      key={item.publication.id}
                      item={item}
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
