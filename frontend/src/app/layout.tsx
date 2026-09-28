import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "AriesHub · 把 AI 想法做成作品", template: "%s · AriesHub" },
  description: "探索 AI 实战案例、制作过程与可复用素材，从一个具体作品开始学习。AriesHub 案例库正在筹备中。",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return <html lang="zh-CN"><body>{children}</body></html>;
}
