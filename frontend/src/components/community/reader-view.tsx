"use client";

import Image from "next/image";
import Link from "next/link";
import {
  ArrowRight,
  Bookmark,
  Heart,
  LockKeyhole,
  MessageCircle,
} from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { type ConceptPublication } from "@/lib/concept-publications";
import { useCommunityState } from "./local-state";

export function ReaderView({
  item,
  initialChapter,
}: {
  item: ConceptPublication;
  initialChapter: number;
}) {
  const { state, toggle, remember } = useCommunityState();
  const [chapter, setChapter] = useState(
    Math.min(Math.max(initialChapter, 0), item.chapters.length - 1),
  );
  const current = item.chapters[chapter];
  const locked = !current.free && item.creditPrice > 0;
  const liked = state.liked.includes(item.id);
  const saved = state.saved.includes(item.id);
  function choose(index: number) {
    setChapter(index);
    remember(item.id, index);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
  return (
    <div className="hub-reader">
      <aside className="hub-reader-toc">
        <Link href={`/preview/publications/${item.id}`}>← 返回内容</Link>
        <p className="hub-kicker">内容目录</p>
        <nav>
          {item.chapters.map((entry, index) => (
            <Button
              variant="ghost"
              key={entry.title}
              aria-current={chapter === index ? "step" : undefined}
              onClick={() => choose(index)}
            >
              <span>{String(index + 1).padStart(2, "0")}</span>
              {entry.title}
              {!entry.free && item.creditPrice > 0 && <LockKeyhole />}
            </Button>
          ))}
        </nav>
        <div className="hub-reader-progress">
          <span>阅读进度</span>
          <i>
            <b
              style={{
                width: `${((chapter + 1) / item.chapters.length) * 100}%`,
              }}
            />
          </i>
        </div>
      </aside>
      <article className="hub-reader-body hub-enter">
        <p className="hub-kicker">{item.category} / 主理人分享</p>
        <h1>{item.title}</h1>
        <p className="hub-reader-lede">
          {item.tagline} {item.summary}
        </p>
        <div className="hub-detail-author">
          <i>A</i>
          <div>
            <strong>Aries</strong>
            <small>内容主理人</small>
          </div>
          <span>{locked ? "完整内容" : "免费预览"}</span>
        </div>
        <Image
          src={item.image}
          alt={item.title}
          width={1000}
          height={600}
          priority
          unoptimized
        />
        <section>
          <h2>
            <span>{String(chapter + 1).padStart(2, "0")}</span>
            {current.title}
          </h2>
          {locked ? (
            <div className="hub-reader-locked">
              <LockKeyhole />
              <h3>这一节属于完整内容</h3>
              <p>公开预览到这里。你仍可以查看内容结构、交付说明和社区讨论。</p>
              <Link className="hub-primary" href={`/checkout/${item.id}`}>
                查看完整内容 <ArrowRight />
              </Link>
            </div>
          ) : (
            <>
              <p>
                先想清楚，这一步真正需要解决什么问题。把目标写成一句话，再决定哪些内容和动作应该留下。
              </p>
              <p>
                小而完整的起点，往往比复杂的功能清单更有价值。完成一条能走通的路径，再根据真实反馈继续调整。
              </p>
              <blockquote>
                <strong>动手试试</strong>
                用一句话，描述你想完成的结果，以及它准备帮助谁。
              </blockquote>
            </>
          )}
        </section>
        {chapter + 1 < item.chapters.length && (
          <Button
            className="hub-next-chapter"
            onClick={() => choose(chapter + 1)}
          >
            下一节：{item.chapters[chapter + 1].title}
            <ArrowRight />
          </Button>
        )}
      </article>
      <aside className="hub-reader-side">
        <h2>
          <MessageCircle /> 边读边聊
        </h2>
        <small>讨论示例</small>
        <p>第一版应该保留哪些功能？</p>
        <Link href={`/community?about=${item.id}`}>
          去讨论 <ArrowRight />
        </Link>
        <hr />
        <h3>这篇内容包含</h3>
        {item.deliverables.slice(0, 3).map((text) => (
          <p key={text}>{text}</p>
        ))}
      </aside>
      <div className="hub-reader-toolbar">
        <Button
          variant="ghost"
          aria-pressed={liked}
          onClick={() => toggle("liked", item.id)}
        >
          <Heart fill={liked ? "currentColor" : "none"} />
          点赞
        </Button>
        <Button
          variant="ghost"
          aria-pressed={saved}
          onClick={() => toggle("saved", item.id)}
        >
          <Bookmark fill={saved ? "currentColor" : "none"} />
          收藏
        </Button>
        <Link href={`/community?about=${item.id}`}>
          <MessageCircle />
          讨论
        </Link>
      </div>
    </div>
  );
}
