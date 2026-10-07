import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ReaderView } from "@/components/community/reader-view";
import { conceptPublications } from "@/lib/concept-publications";

export const metadata: Metadata = { title: "阅读" };
export function generateStaticParams() {
  return conceptPublications.map((item) => ({ id: item.id }));
}
export default async function LearnPage({
  params,
  searchParams,
}: PageProps<"/learn/[id]">) {
  const [{ id }, query] = await Promise.all([params, searchParams]);
  const item = conceptPublications.find((entry) => entry.id === id);
  if (!item) notFound();
  const value = typeof query.chapter === "string" ? Number(query.chapter) : 0;
  return (
    <ReaderView
      item={item}
      initialChapter={Number.isInteger(value) ? value : 0}
    />
  );
}
