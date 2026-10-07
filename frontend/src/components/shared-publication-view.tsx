"use client";
import Link from "next/link";
import { ArrowUpRight, Asterisk, LockKeyhole } from "lucide-react";
import { useState } from "react";
import type { components } from "@/lib/api-schema";
import { useReaderResource } from "./use-reader-resource";
import { useAuthSession } from "./auth-session";
import { Markdown } from "./markdown";
import { PublicationMedia } from "./publication-media";
import { PublicationAttachments } from "./publication-attachments";
import { PublicationMetrics } from "./publication-metrics";
import { PublicationComments } from "./publication-comments";
import { UserAvatar } from "./user-avatar";

type SharedPublication = components["schemas"]["SharedPublication"];
type Content = components["schemas"]["PublicationContent"];

/** 分享阅读留在独立路由，令牌不存入全站 Cookie，不让访客借此浏览其他内容。 */
export function SharedPublicationView({ token }: { token: string }) {
  const { user } = useAuthSession();
  const [revision, setRevision] = useState(0);
  const { data, error } = useReaderResource<SharedPublication>(
    `/api/v1/shares/${token}`,
    revision,
  );
  const { data: content, error: contentError } = useReaderResource<Content>(
    data?.canRead ? `/api/v1/shares/${token}/content` : null,
    revision,
  );
  const article = data?.detail.publication;
  const loginUrl = `/login?next=${encodeURIComponent(`/s/${token}`)}`;
  return (
    <>
      <header className="share-page-header">
        <Link href="/home" aria-label="进入 AriesHub">
          <Asterisk size={24} />
          AriesHub
        </Link>
        <Link href={user ? "/home" : loginUrl}>
          {user ? "进入社区" : "登录"}
          <ArrowUpRight size={14} />
        </Link>
      </header>
      {!data ? (
        <section className="share-page-state" role={error ? "alert" : "status"}>
          <h1>{error ? "这份分享暂时无法查看" : "正在打开分享…"}</h1>
          {error && (
            <>
              <p>{error}</p>
              <button
                type="button"
                onClick={() => setRevision((value) => value + 1)}
              >
                重新加载
              </button>
            </>
          )}
        </section>
      ) : (
        article && (
          <article
            className={`share-page-article${content ? " share-page-reading" : ""}`}
          >
            <div className="share-page-invitation">
              <UserAvatar name={data.sharedBy} url={data.sharedAvatarUrl} />
              <p>
                <span>{data.sharedBy}</span> 给你分享了一篇文章
              </p>
            </div>
            <div className="share-page-heading">
              <span>{article.categoryName}</span>
              <h1>{article.title}</h1>
              <p>{article.summary}</p>
            </div>
            {article.coverFileId && (
              <div className="share-page-cover">
                <PublicationMedia
                  id={article.coverFileId}
                  publicationId={article.id}
                  shareToken={token}
                  signedUrl={data.cover?.url}
                  label={article.title}
                />
              </div>
            )}
            {data.canRead ? (
              <>
                {content ? (
                  <div className="share-page-body">
                    <Markdown publicationId={article.id} shareToken={token}>
                      {content.markdown}
                    </Markdown>
                    <PublicationMetrics id={article.id} trackView />
                    <PublicationAttachments
                      publicationId={article.id}
                      shareToken={token}
                    />
                  </div>
                ) : (
                  <p
                    className="share-page-state"
                    role={contentError ? "alert" : "status"}
                  >
                    {contentError ?? "正在载入正文…"}
                    {contentError && (
                      <button
                        type="button"
                        onClick={() => setRevision((value) => value + 1)}
                      >
                        重试
                      </button>
                    )}
                  </p>
                )}
                <footer className="share-page-footer">
                  <p>还有更多值得探索的实践。</p>
                  <Link href={user ? "/home" : "/login?next=%2Fhome"}>
                    {user ? "探索社区" : "登录，继续探索"}
                    <ArrowUpRight size={16} />
                  </Link>
                </footer>
              </>
            ) : (
              <>
                <div className="share-page-preview">
                  <Markdown
                    publicationId={article.id}
                    shareToken={token}
                    attachmentsLocked={article.accessType === "CREDIT"}
                  >
                    {data.detail.preview.previewMarkdown.slice(0, 900)}
                  </Markdown>
                </div>
                <div className="share-page-action">
                  {!user ? (
                    <Link href={loginUrl}>
                      <LockKeyhole size={17} />
                      登录后解锁全文
                    </Link>
                  ) : (
                    <button type="button" disabled>
                      <LockKeyhole size={17} />
                      解锁功能暂未开放
                    </button>
                  )}
                  <small>
                    {article.accessType === "FREE"
                      ? "这篇文章可直接阅读"
                      : `${article.creditPrice} 积分 · 解锁后可阅读全文`}
                  </small>
                </div>
              </>
            )}
            <PublicationComments
              publicationId={article.id}
              targetKey={article.slug}
              loginReturnTo={`/s/${token}`}
            />
          </article>
        )
      )}
    </>
  );
}
