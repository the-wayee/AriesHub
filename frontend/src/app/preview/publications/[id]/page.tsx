import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ArticleView } from "@/components/community/article-view";
import { conceptPublications } from "@/lib/concept-publications";

export const metadata: Metadata = {
  title: "示例预览",
  robots: { index: false, follow: false },
};

/** 独立静态预览不读取数据库；正式文章始终由 /publications/{id} 提供。 */
export default async function PublicationPreview({
  params,
}: PageProps<"/preview/publications/[id]">) {
  const { id } = await params;
  const item = conceptPublications.find((example) => example.id === id);
  if (!item) notFound();
  return (
    <>
      <p className="hub-preview-label">界面预览 · 示例文章</p>
      <ArticleView item={item} />
    </>
  );
}
