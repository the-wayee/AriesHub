import type { Metadata } from "next";
import { DiscoverView } from "@/components/community/discover-view";

export const metadata: Metadata = { title: "探索" };
export default async function DiscoverPage({
  searchParams,
}: PageProps<"/discover">) {
  const params = await searchParams;
  return (
    <DiscoverView
      initialQuery={typeof params.q === "string" ? params.q.slice(0, 120) : ""}
    />
  );
}
