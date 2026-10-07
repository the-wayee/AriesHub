import type { Metadata } from "next";
import { DiscoverView } from "@/components/community/discover-view";

export const metadata: Metadata = { title: "探索" };
export default async function DiscoverPage({
  searchParams,
}: PageProps<"/discover">) {
  const params = await searchParams;
  return (
    <DiscoverView
      category={typeof params.category === "string" ? params.category : ""}
      type={typeof params.type === "string" ? params.type : ""}
      access={typeof params.access === "string" ? params.access : ""}
      page={
        typeof params.page === "string" && /^\d{1,5}$/.test(params.page)
          ? Math.max(1, Math.min(10000, Number(params.page)))
          : 1
      }
      initialQuery={typeof params.q === "string" ? params.q.slice(0, 120) : ""}
    />
  );
}
