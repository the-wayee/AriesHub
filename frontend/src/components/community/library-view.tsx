"use client";

import { Bookmark, ArrowRight } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { conceptPublications } from "@/lib/concept-publications";
import { ContentCard } from "./content-card";
import { useCommunityState } from "./local-state";

export function LibraryView() {
  const { state } = useCommunityState();
  const [tab, setTab] = useState<"saved" | "history" | "liked">("saved");
  const items = conceptPublications.filter((item) =>
    tab === "saved"
      ? state.saved.includes(item.slug)
      : tab === "liked"
        ? state.liked.includes(item.slug)
        : state.history[item.slug] !== undefined,
  );
  return (
    <>
      <header className="hub-page-heading hub-enter">
        <p className="hub-kicker">YOUR SPACE</p>
        <h1>
          留住灵感，
          <br />
          <span>继续探索。</span>
        </h1>
        <p>收藏过的内容，走到一半的思路，都在这里。</p>
      </header>
      <div className="hub-filterbar">
        <div className="hub-pills">
          {(
            [
              ["saved", "我的收藏", state.saved.length],
              ["history", "阅读记录", Object.keys(state.history).length],
              ["liked", "我的点赞", state.liked.length],
            ] as const
          ).map(([id, label, count]) => (
            <Button
              key={id}
              variant="ghost"
              aria-pressed={tab === id}
              onClick={() => setTab(id)}
            >
              {label}
              <small>{count}</small>
            </Button>
          ))}
        </div>
      </div>
      {items.length ? (
        <div className="hub-discover-grid">
          {items.map((item) => (
            <div key={item.slug}>
              <ContentCard
                item={item}
                index={conceptPublications.indexOf(item)}
              />
              {tab === "history" && (
                <Link
                  className="hub-inline-link"
                  href={`/learn/${item.slug}?chapter=${state.history[item.slug]}`}
                >
                  继续第 {state.history[item.slug] + 1} 节 <ArrowRight />
                </Link>
              )}
            </div>
          ))}
        </div>
      ) : (
        <div className="hub-empty">
          <Bookmark />
          <h2>
            {tab === "history"
              ? "下一次探索，从这里开始"
              : "给喜欢的内容，留一个位置"}
          </h2>
          <p>
            {tab === "history"
              ? "打开一篇内容开始阅读，这里会记住你的章节。"
              : "在内容卡片上点一下收藏，慢慢建立自己的灵感空间。"}
          </p>
          <Link href="/discover">
            去发现 <ArrowRight />
          </Link>
        </div>
      )}
    </>
  );
}
