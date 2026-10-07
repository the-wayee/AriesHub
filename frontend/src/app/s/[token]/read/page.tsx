import type { Metadata } from "next";
import { redirect } from "next/navigation";
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
  // 兼容已生成的旧入口，统一回到直接阅读的分享地址。
  redirect(`/s/${token}`);
}
