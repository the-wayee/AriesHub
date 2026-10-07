import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { PublicationDetailView } from "@/components/publication-detail-view";
import { getPublication, getContent } from "@/lib/catalog";

export async function generateMetadata({
  params,
}: PageProps<"/publications/[id]">): Promise<Metadata> {
  const { id } = await params;
  if (!isPublicationId(id)) return { title: "内容不存在" };
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
  if (!isPublicationId(id)) notFound();
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
  const content = await getContent(id);
  return (
    <PublicationDetailView
      detail={result.data}
      content={content?.ok ? content.data : undefined}
      contentError={!content.ok && ![401, 403].includes(content.status)}
    />
  );
}

/** 保留 bigint 的字符串表示，避免 JavaScript Number 截断文章 ID；拒绝旧名称地址。 */
function isPublicationId(id: string): boolean {
  return (
    /^[1-9][0-9]{0,18}$/.test(id) && BigInt(id) <= BigInt("9223372036854775807")
  );
}
