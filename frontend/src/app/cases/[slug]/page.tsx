import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { getCase, getContent } from "@/lib/catalog";
import { ContentState } from "@/components/content-state";
import { Markdown } from "@/components/markdown";
import { priceLabel } from "@/components/case-card";

export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}): Promise<Metadata> {
  const { slug } = await params;
  const result = await getCase(slug);
  return result.ok
    ? {
        title: result.data.caseInfo.title,
        description: result.data.caseInfo.summary,
      }
    : { title: "案例" };
}

export default async function CasePage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const result = await getCase(slug);
  if (!result.ok) {
    if (result.status === 404 || result.status === 400) notFound();
    return (
      <ContentState
        title="案例暂时加载不了"
        message="请稍后重试，或返回案例库浏览其他内容。"
        href={`/cases/${encodeURIComponent(slug)}`}
        reload
        label="重新加载"
        requestId={result.requestId}
      />
    );
  }
  const { caseInfo: info, preview } = result.data;
  const content = info.accessType === "FREE" ? await getContent(info.id) : null;
  // 两次读取之间可能发生下架或收费方式变更；以正文接口的最新权限结果为准。
  if (content && !content.ok && content.status === 404) notFound();
  return (
    <article className="detail-page">
      <nav className="breadcrumbs" aria-label="面包屑">
        <Link href="/cases">案例库</Link>
        <span aria-hidden="true">/</span>
        <Link href={`/cases?category=${info.categorySlug}`}>
          {info.categoryName}
        </Link>
      </nav>
      <header className="detail-heading">
        <div className="detail-labels">
          <span>{info.categoryName}</span>
          {info.isDemo && <span className="demo-badge">演示案例</span>}
        </div>
        <h1>{info.title}</h1>
        <p>{info.summary}</p>
      </header>
      {info.isDemo && (
        <p className="demo-notice">
          这是用于体验平台的演示内容，暂不提供购买或文件下载。
        </p>
      )}
      <div className="detail-grid">
        <div className="detail-content">
          <section aria-labelledby="preview-title">
            <p className="eyebrow">BEFORE YOU START</p>
            <h2 id="preview-title">案例预览</h2>
            <Markdown>{preview.previewMarkdown}</Markdown>
          </section>
          {info.accessType === "FREE" ? (
            <section
              className="full-content"
              id="read"
              aria-labelledby="read-title"
            >
              <p className="eyebrow">THE PROCESS</p>
              <h2 id="read-title">完整教程</h2>
              {content?.ok ? (
                <Markdown>{content.data.markdown}</Markdown>
              ) : (
                <ContentState
                  title="正文暂时无法读取"
                  message="内容可能已更新或访问方式发生变化，请重新加载。"
                  href={`/cases/${slug}`}
                  reload
                  label="重新加载"
                />
              )}
            </section>
          ) : (
            <section className="locked-content" aria-labelledby="locked-title">
              <p className="eyebrow">THE COMPLETE CASE</p>
              <h2 id="locked-title">完整内容，稍后开放</h2>
              <p>现在可以查看公开预览与交付说明。购买功能尚未开放。</p>
            </section>
          )}
        </div>
        <aside className="case-facts" aria-label="案例说明">
          <p className="eyebrow">CASE NOTES</p>
          <p className="case-price">{priceLabel(info)}</p>
          {info.accessType === "FREE" ? (
            <a className="primary-link" href="#read">
              阅读完整教程 ↓
            </a>
          ) : (
            <p className="purchase-status">暂未开放购买</p>
          )}
          <dl>
            <dt>开始之前</dt>
            <dd>{preview.requirements}</dd>
            <dt>内容与交付</dt>
            <dd>{preview.deliverables}</dd>
            <dt>内容版本</dt>
            <dd>{preview.version}</dd>
            <dt>最近更新</dt>
            <dd>
              {new Intl.DateTimeFormat("zh-CN", {
                timeZone: "Asia/Shanghai",
              }).format(new Date(preview.updatedAt))}
            </dd>
          </dl>
          <Link className="text-link" href="/cases">
            ← 继续浏览案例
          </Link>
        </aside>
      </div>
    </article>
  );
}
