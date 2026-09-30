"use client";

import { Bookmark, ArrowRight } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { conceptPublications } from "@/lib/concept-publications";
import { ContentCard } from "./content-card";

const filters = [
  ["", "全部"],
  ["web", "AI 编程"],
  ["content", "内容创作"],
  ["automation", "自动化"],
];

export function DiscoverView({ initialQuery = "" }: { initialQuery?: string }) {
  const [filter, setFilter] = useState("");
  const items = conceptPublications.filter(
    (item) =>
      (!filter || item.categorySlug === filter) &&
      `${item.title} ${item.summary}`
        .toLowerCase()
        .includes(initialQuery.toLowerCase()),
  );
  return (
    <>
      <header className="hub-page-heading hub-enter">
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
        <p>实战案例、文章和课程，由主理人持续更新。</p>
      </header>
      <div className="hub-filterbar">
        <div className="hub-pills">
          {filters.map(([value, label]) => (
            <Button
              key={value}
              variant="ghost"
              aria-pressed={filter === value}
              onClick={() => setFilter(value)}
            >
              {label}
            </Button>
          ))}
        </div>
        <Link href="/my-content">
          <Bookmark /> 我的收藏
        </Link>
      </div>
      {items.length ? (
        <div className="hub-discover-grid">
          {items.map((item) => (
            <ContentCard
              key={item.slug}
              item={item}
              index={conceptPublications.indexOf(item)}
            />
          ))}
        </div>
      ) : (
        <div className="hub-empty">
          <span>✳</span>
          <h2>没有找到匹配的内容</h2>
          <p>换个关键词，或者回到全部内容继续探索。</p>
          <Link href="/discover">
            查看全部 <ArrowRight />
          </Link>
        </div>
      )}
    </>
  );
}
