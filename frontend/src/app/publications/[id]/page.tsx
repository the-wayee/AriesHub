import type { Metadata } from "next";
import { notFound, permanentRedirect } from "next/navigation";
import { ArticleView } from "@/components/community/article-view";
import { PublicationDetailView } from "@/components/publication-detail-view";
import { conceptPublications } from "@/lib/concept-publications";
import { getPublication, getContent } from "@/lib/catalog";

export async function generateMetadata({
  params,
}: PageProps<"/publications/[id]">): Promise<Metadata> {
  const { id } = await params;
  const example = conceptPublications.find((item) => item.slug === id);
  if (example)
    return { title: example.title, robots: { index: false, follow: false } };
  const result = await getPublication(id);
  return result.ok
    ? {
        title: result.data.publication.title,
        description: result.data.publication.summary,
        alternates: {
          canonical: `/publications/${result.data.publication.id}`,
        },
      }
    : { title: "内容" };
}

export default async function PublicationPage({
  params,
}: PageProps<"/publications/[id]">) {
  const { id } = await params;
  // 旧概念稿保留明确的预览标识，绝不作为数据库中已发布的文章展示。
  const example = conceptPublications.find((item) => item.slug === id);
  if (example)
    return (
      <>
        <p className="hub-preview-label">界面预览 · 示例文章</p>
        <ArticleView item={example} />
      </>
    );
  const result = await getPublication(id);
  if (!result.ok) {
    if (result.status === 404 || result.status === 400) notFound();
    return (
      <section className="hub-empty" role="alert">
        <h1>内容暂时无法读取</h1>
        <p>请稍后重新加载。</p>
        <a href={`/publications/${encodeURIComponent(id)}`}>重新加载</a>
      </section>
    );
  }
  if (id !== result.data.publication.id)
    permanentRedirect(`/publications/${result.data.publication.id}`);
  const content =
    result.data.publication.accessType === "FREE"
      ? await getContent(id)
      : undefined;
  return (
    <PublicationDetailView
      detail={result.data}
      content={content?.ok ? content.data : undefined}
      contentError={content !== undefined && !content.ok}
    />
  );
}
