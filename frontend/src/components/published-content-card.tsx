"use client";
import Link from "next/link";
import Image from "next/image";
import { useState } from "react";
import { PublicationInteractions } from "./publication-interactions";
import { PublicationMedia } from "./publication-media";
import type { PublicationCardData } from "@/lib/publication-reader";
import {
  PublicationAccessBadge,
  PublicationFormBadge,
  PublicationProgressBadge,
} from "./publication-badges";
/** 使用实际内容形式与 ID，列表已携带签名，失效时才请求单素材续签。 */
export function PublishedContentCard({
  item,
  compact = false,
  minimal = false,
  categoryColor,
}: {
  item: PublicationCardData;
  compact?: boolean;
  minimal?: boolean;
  categoryColor?: string;
}) {
  const { publication: p } = item;
  return (
    <article
      className={`hub-content-card published-content-card hub-enter${compact ? " community-card-compact" : ""}`}
      data-form={p.publicationType}
    >
      <Link className="hub-content-image" href={`/publications/${p.id}`}>
        <PublishedCover key={p.coverFileId} item={item} />
        {!minimal && <PublicationFormBadge type={p.publicationType} />}
      </Link>
      <div className="hub-content-copy">
        {minimal && (
          <div className="home-card-type">
            <PublicationFormBadge type={p.publicationType} minimal />
          </div>
        )}
        <Link href={`/publications/${p.id}`}>
          <h2>{p.title}</h2>
        </Link>
        <p>{p.summary}</p>
        <div className="hub-content-meta">
          <div>
            <span
              className="publication-category-name"
              style={
                minimal && categoryColor
                  ? {
                      color: `color-mix(in srgb, ${categoryColor} 65%, #27313d)`,
                    }
                  : undefined
              }
            >
              {minimal && categoryColor && (
                <i
                  aria-hidden="true"
                  style={{ backgroundColor: categoryColor }}
                />
              )}
              {p.categoryName}
            </span>
            <PublicationAccessBadge
              minimal={minimal}
              access={p.accessType}
              credits={p.creditPrice}
            />
          </div>
          <time dateTime={p.publishedAt}>{p.publishedAt.slice(0, 10)}</time>
        </div>
        {item.progress && (
          <Link
            className="publication-resume-link"
            href={`/publications/${p.id}${item.progress.percent === 100 ? "" : "?resume=1"}`}
            aria-label={
              item.progress.percent === 100
                ? `再读一次 · ${p.title}`
                : undefined
            }
          >
            <PublicationProgressBadge percent={item.progress.percent} />
          </Link>
        )}
        <PublicationInteractions id={p.id} initial={item.interaction} />
      </div>
    </article>
  );
}

/** 卡片和首页大卡共用聚合签名，只有首次失败或缺少签名时才调用受控续签接口。 */
export function PublishedCover({
  item,
  priority = false,
}: {
  item: PublicationCardData;
  priority?: boolean;
}) {
  const { publication: p, cover } = item;
  const [renew, setRenew] = useState(false);
  if (!p.coverFileId)
    return (
      <div className="reader-cover-empty" aria-label="暂无封面">
        {p.categoryName}
      </div>
    );
  return cover && !renew ? (
    <Image
      src={cover.url}
      alt={p.title}
      width={720}
      height={540}
      sizes="(max-width:760px) 100vw,50vw"
      unoptimized
      priority={priority}
      onError={() => setRenew(true)}
    />
  ) : (
    <PublicationMedia id={p.coverFileId} publicationId={p.id} label={p.title} />
  );
}
