import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ArticleView } from "@/components/community/article-view";
import { conceptPublications } from "@/lib/concept-publications";

export function generateStaticParams() {
  return conceptPublications.map((item) => ({ slug: item.slug }));
}
export async function generateMetadata({
  params,
}: PageProps<"/publications/[slug]">): Promise<Metadata> {
  const { slug } = await params;
  const item = conceptPublications.find((entry) => entry.slug === slug);
  return { title: item?.title ?? "内容", description: item?.summary };
}
export default async function PublicationPage({
  params,
}: PageProps<"/publications/[slug]">) {
  const { slug } = await params;
  const item = conceptPublications.find((entry) => entry.slug === slug);
  if (!item) notFound();
  return <ArticleView item={item} />;
}
