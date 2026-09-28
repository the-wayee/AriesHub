import type { Metadata } from "next";
import type { ReactNode } from "react";
import { SiteFooter, SiteHeader } from "@/components/site-shell";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "AriesHub · 把 AI 想法做成作品",
    template: "%s · AriesHub",
  },
  description:
    "探索 AI 实战案例、制作过程与可复用方法，从一个具体作品开始学习。",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="zh-CN">
      <body>
        <SiteHeader />
        <main id="main" className="wrap">
          {children}
        </main>
        <SiteFooter />
      </body>
    </html>
  );
}
