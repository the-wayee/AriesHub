import type { Metadata } from "next";
import { DiscussionView } from "@/components/community/discussion-view";

export const metadata: Metadata = { title: "讨论" };
export default async function CommunityPage({
  searchParams,
}: PageProps<"/community">) {
  const params = await searchParams;
  return (
    <DiscussionView
      about={typeof params.about === "string" ? params.about : undefined}
    />
  );
}
