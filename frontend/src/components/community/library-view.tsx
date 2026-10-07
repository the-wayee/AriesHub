"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Button } from "../ui/button";
import { useReaderResource } from "../use-reader-resource";
import { PublishedContentCard } from "../published-content-card";
import { PageSkeleton } from "../page-skeleton";
import type { ReaderPage } from "@/lib/publication-reader";
/** 我的空间读取当前会话，不从本地原型迁移无法验证的收藏与历史。 */
export function LibraryView() {
  const [tab, setTab] = useState("BOOKMARK");
  const [page, setPage] = useState(1);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const refresh = () => setRetry((n) => n + 1);
    window.addEventListener("arieshub:publication", refresh);
    return () => window.removeEventListener("arieshub:publication", refresh);
  }, []);
  const { data, error } = useReaderResource<ReaderPage>(
    `/api/v1/users/me/library?kind=${tab}&page=${page}&size=9`,
    retry,
  );
  return (
    <>
      <header className="hub-page-heading">
        <p className="hub-kicker">YOUR SPACE</p>
        <h1>
          留住灵感，
          <br />
          <span>继续探索。</span>
        </h1>
        <p>收藏、点赞和阅读记录随账号保存。</p>
      </header>
      <div className="hub-filterbar">
        <div className="hub-pills">
          {[
            ["BOOKMARK", "我的收藏"],
            ["HISTORY", "阅读记录"],
            ["LIKE", "我的点赞"],
          ].map(([id, label]) => (
            <Button
              key={id}
              variant="ghost"
              aria-pressed={tab === id}
              onClick={() => {
                setTab(id);
                setPage(1);
              }}
            >
              {label}
            </Button>
          ))}
        </div>
        <Button variant="ghost" onClick={() => setRetry(retry + 1)}>
          刷新
        </Button>
      </div>
      {error ? (
        <div className="hub-empty" role="alert">
          <p>{error}</p>
          <Button onClick={() => setRetry(retry + 1)}>重试</Button>
        </div>
      ) : !data ? (
        <PageSkeleton variant="gallery" />
      ) : data.items.length ? (
        <>
          <div className="hub-discover-grid">
            {data.items.map((p) => (
              <PublishedContentCard key={p.publication.id} item={p} />
            ))}
          </div>
          <div className="reader-pagination">
            <Button disabled={page === 1} onClick={() => setPage(page - 1)}>
              上一页
            </Button>
            <span>
              第 {page} 页 · 共 {data.total} 篇
            </span>
            <Button
              disabled={page >= data.totalPages}
              onClick={() => setPage(page + 1)}
            >
              下一页
            </Button>
          </div>
        </>
      ) : (
        <div className="hub-empty">
          <h2>
            {tab === "HISTORY" ? "还没有阅读记录" : "给喜欢的内容，留一个位置"}
          </h2>
          <p>
            {tab === "HISTORY"
              ? "打开免费完整正文开始阅读，位置会随账号保存。"
              : "在内容卡片上收藏或点赞，建立自己的内容空间。"}
          </p>
          <Link href="/discover">去发现 →</Link>
        </div>
      )}
    </>
  );
}
