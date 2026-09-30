import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ArticleView } from "@/components/community/article-view";
import { conceptCases } from "@/lib/concept-cases";

export function generateStaticParams() {
  return conceptCases.map((item) => ({ slug: item.slug }));
}
export async function generateMetadata({
  params,
}: PageProps<"/cases/[slug]">): Promise<Metadata> {
  const { slug } = await params;
  const item = conceptCases.find((entry) => entry.slug === slug);
  return { title: item?.title ?? "内容", description: item?.summary };
}
export default async function CasePage({ params }: PageProps<"/cases/[slug]">) {
  const { slug } = await params;
  const item = conceptCases.find((entry) => entry.slug === slug);
  if (!item) notFound();
  return <ArticleView item={item} />;
}
