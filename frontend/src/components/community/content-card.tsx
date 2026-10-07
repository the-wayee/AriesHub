"use client";

import Image from "next/image";
import Link from "next/link";
import { Bookmark, Heart, MessageCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  type ConceptPublication,
  formatConceptCredits,
} from "@/lib/concept-publications";
import { useCommunityState } from "./local-state";

export function ContentActions({ item }: { item: ConceptPublication }) {
  const { state, toggle } = useCommunityState();
  const liked = state.liked.includes(item.id);
  const saved = state.saved.includes(item.id);
  return (
    <div className="hub-content-actions">
      <Button
        variant="ghost"
        size="icon"
        aria-label={`${liked ? "取消点赞" : "点赞"}：${item.title}`}
        aria-pressed={liked}
        onClick={() => toggle("liked", item.id)}
      >
        <Heart fill={liked ? "currentColor" : "none"} />
      </Button>
      <Button
        variant="ghost"
        size="icon"
        aria-label={`${saved ? "取消收藏" : "收藏"}：${item.title}`}
        aria-pressed={saved}
        onClick={() => toggle("saved", item.id)}
      >
        <Bookmark fill={saved ? "currentColor" : "none"} />
      </Button>
      <Link
        href={`/community?about=${item.id}`}
        aria-label={`讨论：${item.title}`}
      >
        <MessageCircle />
      </Link>
    </div>
  );
}

export function ContentCard({
  item,
  index,
  compact = false,
}: {
  item: ConceptPublication;
  index: number;
  compact?: boolean;
}) {
  const type = index === 1 ? "文章" : index === 3 ? "课程" : "实战案例";
  return (
    <article
      className={`hub-content-card hub-enter ${compact ? "is-compact" : ""}`}
    >
      <Link
        className="hub-content-image"
        href={`/preview/publications/${item.id}`}
      >
        <Image
          src={item.image}
          alt={item.title}
          width={720}
          height={540}
          sizes="(max-width: 760px) 100vw, 50vw"
          unoptimized
        />
        <span>{type}</span>
      </Link>
      <div className="hub-content-copy">
        <Link href={`/preview/publications/${item.id}`}>
          <h2>{item.shortTitle}</h2>
        </Link>
        <p>{item.summary}</p>
        <div className="hub-content-meta">
          <div>
            <span>免费预览</span>
            <small>
              {item.creditPrice
                ? `完整内容 ${formatConceptCredits(item.creditPrice)}`
                : "开放阅读"}
            </small>
          </div>
          <ContentActions item={item} />
        </div>
      </div>
    </article>
  );
}
