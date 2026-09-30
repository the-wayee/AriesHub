"use client";

import Image from "next/image";
import Link from "next/link";
import { ArrowRight, ArrowUpRight, Check, LockKeyhole } from "lucide-react";
import { ContentActions } from "./content-card";
import { type ConceptCase, formatConceptCredits } from "@/lib/concept-cases";

export function ArticleView({ item }: { item: ConceptCase }) {
  return (
    <article className="hub-article-detail">
      <Link className="hub-back" href="/discover">
        ← 返回探索
      </Link>
      <header className="hub-detail-heading hub-enter">
        <p className="hub-kicker">{item.category} / 主理人分享</p>
        <h1>{item.title}</h1>
        <p>{item.summary}</p>
        <div className="hub-detail-author">
          <i>A</i>
          <div>
            <strong>Aries</strong>
            <small>内容主理人 · {item.chapters.length} 个章节</small>
          </div>
          <ContentActions item={item} />
        </div>
      </header>
      <Image
        className="hub-detail-cover hub-enter"
        src={item.image}
        alt={item.title}
        width={1400}
        height={780}
        priority
        unoptimized
      />
      <div className="hub-detail-grid">
        <section className="hub-detail-body">
          <p className="hub-kicker">INSIDE THE PRACTICE</p>
          <h2>从起点，到自己的答案。</h2>
          <p>
            {item.summary}{" "}
            不只记录结果，也把关键判断、具体步骤和过程中值得留意的细节放在一起。
          </p>
          <h3>内容目录</h3>
          <div className="hub-chapter-list">
            {item.chapters.map((chapter, index) => (
              <Link
                key={chapter.title}
                href={`/learn/${item.slug}?chapter=${index}`}
              >
                <span>{String(index + 1).padStart(2, "0")}</span>
                <strong>{chapter.title}</strong>
                <small>
                  {chapter.free || item.creditPrice === 0 ? (
                    "免费预览"
                  ) : (
                    <LockKeyhole />
                  )}
                </small>
                <ArrowUpRight />
              </Link>
            ))}
          </div>
        </section>
        <aside className="hub-purchase">
          <p className="hub-kicker">深入这次实践</p>
          <h2>{formatConceptCredits(item.creditPrice)}</h2>
          <p>
            {item.creditPrice ? "单篇内容 · 积分解锁" : "开放阅读，与大家分享"}
          </p>
          <Link className="hub-primary" href={`/learn/${item.slug}`}>
            {item.creditPrice ? "阅读免费预览" : "开始阅读"}
            <ArrowUpRight />
          </Link>
          {item.creditPrice > 0 && (
            <Link className="hub-inline-link" href={`/checkout/${item.slug}`}>
              查看积分解锁信息 <ArrowRight />
            </Link>
          )}
          <hr />
          <h3>这份内容包含</h3>
          {item.deliverables.map((text) => (
            <p key={text}>
              <Check />
              {text}
            </p>
          ))}
          <small>
            完整正文与资源将在积分权益接入后开放，当前不会扣除积分。
          </small>
        </aside>
      </div>
    </article>
  );
}
