import type { Metadata } from "next";
import { AuthExperience } from "@/components/studio/auth-experience";
export const metadata: Metadata = { title: "登录" };
export default async function Page({
  searchParams,
}: {
  searchParams: Promise<{
    email?: string | string[];
    next?: string | string[];
  }>;
}) {
  const { email, next } = await searchParams;
  // 登录回跳仅接受列出的站内路径，保留分享令牌与单篇阅读入口。
  const returnTo =
    typeof next === "string" &&
    /^(?:\/publications\/[1-9]\d*(?:#comments)?|\/s\/[A-Za-z0-9_-]{43}(?:\/read)?|\/home|\/discover)$/.test(
      next,
    )
      ? next
      : undefined;
  return (
    <AuthExperience
      mode="login"
      returnTo={returnTo}
      initialEmail={typeof email === "string" ? email : ""}
    />
  );
}
