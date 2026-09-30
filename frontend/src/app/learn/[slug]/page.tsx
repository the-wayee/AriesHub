import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ReaderView } from "@/components/community/reader-view";
import { conceptPublications } from "@/lib/concept-publications";

export const metadata: Metadata = { title: "阅读" };
export function generateStaticParams() {
  return conceptPublications.map((item) => ({ slug: item.slug }));
}
export default async function LearnPage({
  params,
  searchParams,
}: PageProps<"/learn/[slug]">) {
  const [{ slug }, query] = await Promise.all([params, searchParams]);
  const item = conceptPublications.find((entry) => entry.slug === slug);
  if (!item) notFound();
  const value = typeof query.chapter === "string" ? Number(query.chapter) : 0;
  return (
    <ReaderView
      item={item}
      initialChapter={Number.isInteger(value) ? value : 0}
    />
  );
}
