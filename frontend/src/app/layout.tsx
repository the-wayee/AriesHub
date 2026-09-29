import type { Metadata } from "next";
import type { ReactNode } from "react";
import { RouteShell } from "@/components/studio/route-shell";
import { cookies } from "next/headers";
import { SiteFooter, SiteHeader } from "@/components/site-shell";
import "./globals.css";
import "./independent.css";
import "./cosmos.css";

export const metadata: Metadata = {
  title: {
    default: "AriesHub · AI 实践社区",
    template: "%s · AriesHub",
  },
  description:
    "一个分享 AI 实践、交流想法的社区。主理人持续发布深度内容，成员一起学习与探索。",
};

export default async function RootLayout({
  children,
}: {
  children: ReactNode;
}) {
  const hasSession = (await cookies()).has("arieshub_token");
  return (
    <html lang="zh-CN" data-scroll-behavior="smooth">
      <body>
        <RouteShell
          header={<SiteHeader />}
          footer={<SiteFooter />}
          hasSession={hasSession}
        >
          {children}
        </RouteShell>
      </body>
    </html>
  );
}
