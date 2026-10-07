import { PublicationOutline } from "./publication-outline";
import { PublicationComments } from "./publication-comments";
import { PublicationAttachments } from "./publication-attachments";
import { ReadingProgressTracker } from "./reading-progress";
import { PublicationMetrics } from "./publication-metrics";
import { PublicationShare } from "./publication-share";
import { PublicationInteractions } from "./publication-interactions";
import Link from "next/link";
import { LockKeyhole, BookOpen } from "lucide-react";
import { Markdown } from "./markdown";
import { PublicationMedia } from "./publication-media";
import {
  PublicationAccessBadge,
  PublicationFormBadge,
} from "./publication-badges";
import type {
  PublicationDetail,
  PublicationContent,
} from "@/lib/catalog-types";

/** 真实发布内容按 ID 展示；私有正文只使用后端授权后返回的数据。 */
export function PublicationDetailView({
  detail,
  content,
  contentError = false,
}: {
  detail: PublicationDetail;
  content?: PublicationContent;
  contentError?: boolean;
}) {
  const { publication: article, preview } = detail;
  const locked = article.accessType === "CREDIT" && !content && !contentError;
  return (
    <article className="hub-article-detail published-article">
      <Link className="hub-back" href="/discover">
        ← 返回探索
      </Link>
      <header className="hub-detail-heading">
        <p className="hub-kicker">
          {article.categoryName} /{" "}
          <PublicationFormBadge type={article.publicationType} />
        </p>
        <h1>{article.title}</h1>
        <p>{article.summary}</p>
        <div className="published-meta">
          <span>
            {new Date(article.publishedAt).toLocaleDateString("zh-CN")}
          </span>
          <PublicationMetrics key={article.id} id={article.id} trackView />
          <PublicationAccessBadge
            access={article.accessType}
            credits={article.creditPrice}
          />
        </div>
      </header>
      {article.coverFileId && (
        <div className="published-cover">
          <PublicationMedia
            id={article.coverFileId}
            publicationId={article.id}
            label={article.title}
          />
        </div>
      )}
      <div className="hub-detail-grid">
        <section
          className="hub-detail-body"
          aria-label={content ? "完整正文" : "免费试读"}
        >
          {content ? (
            <ReadingProgressTracker id={article.id} version={content.version}>
              <Markdown publicationId={article.id}>{content.markdown}</Markdown>
            </ReadingProgressTracker>
          ) : (
            <>
              {preview.previewMarkdown ? (
                <div
                  className={locked ? "publication-trial-excerpt" : undefined}
                >
                  <Markdown
                    publicationId={article.id}
                    attachmentsLocked={locked}
                  >
                    {preview.previewMarkdown}
                  </Markdown>
                </div>
              ) : !locked ? (
                <p>暂未提供试读内容。</p>
              ) : null}
              {locked && (
                <section
                  className="publication-unlock"
                  aria-label="解锁完整内容"
                >
                  <span
                    className="publication-unlock-symbol"
                    aria-hidden="true"
                  >
                    <LockKeyhole size={22} />
                  </span>
                  <p className="publication-unlock-eyebrow">
                    {preview.previewMarkdown
                      ? "免费试读到这里"
                      : "这篇内容需要解锁"}
                  </p>
                  <h2>解锁后，继续阅读完整内容</h2>
                  <p>阅读全文，查看作者分享的完整实践与随文资源。</p>
                  <div className="publication-unlock-price">
                    <strong>
                      {article.creditPrice.toLocaleString("zh-CN")}
                    </strong>
                    <span>积分 / 篇</span>
                  </div>
                  <button
                    type="button"
                    disabled
                    aria-describedby="unlock-availability"
                  >
                    <LockKeyhole size={16} />
                    积分解锁 · 暂未开放
                  </button>
                  <small id="unlock-availability">
                    解锁功能即将开放，当前不会扣除积分。
                  </small>
                  <Link href="/discover?access=FREE">
                    <BookOpen size={15} />
                    先读其他免费内容
                  </Link>
                </section>
              )}
              {contentError && (
                <div className="hub-empty" role="alert">
                  <p>完整正文暂时无法读取，请稍后重试。</p>
                  <Link href={`/publications/${article.id}`}>重新加载</Link>
                </div>
              )}
            </>
          )}
          <div className="publication-end-actions" aria-label="文章操作">
            <PublicationInteractions id={article.id} prominent />
            <PublicationShare id={article.id} title={article.title} />
          </div>
          <PublicationComments
            publicationId={article.id}
            targetKey={article.id}
          />
        </section>
        <aside className="publication-sidebar" aria-label="文章目录与资源">
          <PublicationOutline
            contentKey={content?.markdown ?? preview.previewMarkdown}
            chapters={detail.chapters}
          />
          <PublicationAttachments publicationId={article.id} />
        </aside>
      </div>
    </article>
  );
}
