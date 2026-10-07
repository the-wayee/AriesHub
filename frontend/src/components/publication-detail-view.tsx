import { ReadingProgressTracker } from "./reading-progress";
import { PublicationInteractions } from "./publication-interactions";
import Link from "next/link";
import { Markdown } from "./markdown";
import { PublicationMedia } from "./publication-media";
import type {
  PublicationDetail,
  PublicationContent,
} from "@/lib/catalog-types";

const CONTENT_FORMS: Record<string, string> = {
  CASE_STUDY: "实战案例",
  ARTICLE: "学习文章",
  COURSE: "课程",
};

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
  return (
    <article className="hub-article-detail published-article">
      <Link className="hub-back" href="/discover">
        ← 返回探索
      </Link>
      <header className="hub-detail-heading">
        <p className="hub-kicker">
          {article.categoryName} / {CONTENT_FORMS[article.publicationType]}
        </p>
        <h1>{article.title}</h1>
        <p>{article.summary}</p>
        <div className="published-meta">
          <span>
            {new Date(article.publishedAt).toLocaleDateString("zh-CN")}
          </span>
          <span>
            {article.accessType === "FREE"
              ? "免费阅读"
              : `${article.creditPrice} 积分 · 免费预览`}
          </span>
          <span>版本 {preview.version}</span>
        </div>
        <PublicationInteractions id={article.id} />
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
          aria-label={content ? "完整正文" : "公开预览"}
        >
          {content ? (
            <ReadingProgressTracker id={article.id} version={content.version}>
              <Markdown publicationId={article.id}>{content.markdown}</Markdown>
            </ReadingProgressTracker>
          ) : (
            <>
              <h2>公开预览</h2>
              <Markdown publicationId={article.id}>
                {preview.previewMarkdown || "主理人尚未提供公开预览。"}
              </Markdown>
              {contentError && (
                <div className="hub-empty" role="alert">
                  <p>完整正文暂时无法读取，请稍后重试。</p>
                  <Link href={`/publications/${article.id}`}>重新加载</Link>
                </div>
              )}
            </>
          )}
        </section>
        <aside className="hub-purchase">
          <p className="hub-kicker">关于这份内容</p>
          <h2>
            {article.accessType === "FREE"
              ? "开放阅读"
              : `${article.creditPrice} 积分`}
          </h2>
          {article.accessType === "CREDIT" && (
            <p>积分权益尚未开放，当前仅提供免费预览，不会扣除积分。</p>
          )}
          {preview.requirements && (
            <>
              <h3>开始之前</h3>
              <Markdown>{preview.requirements}</Markdown>
            </>
          )}
          {preview.deliverables && (
            <>
              <h3>内容与交付</h3>
              <Markdown>{preview.deliverables}</Markdown>
            </>
          )}
          <small>文章链接 /publications/{article.id}</small>
        </aside>
      </div>
    </article>
  );
}
