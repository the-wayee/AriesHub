import type { Metadata } from "next";
import { SharedPublicationView } from "@/components/shared-publication-view";
export const metadata: Metadata = {
  title: "给你分享的一篇文章",
  robots: { index: false, follow: false },
};
export default async function Page({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = await params;
  return <SharedPublicationView token={token} />;
}
