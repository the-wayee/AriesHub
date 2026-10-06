import type { Metadata } from "next";
import { AccountPanel } from "@/components/account-panel";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";

export const metadata: Metadata = { title: "我的账号" };

export default function AccountPage() {
  return (
    <div className="profile-page">
      <Link className="profile-back" href="/home">
        <ArrowLeft />
        返回社区
      </Link>
      <header className="profile-heading hub-enter">
        <p className="hub-kicker">账号 / 个人资料</p>
        <h1>你的个人资料</h1>
        <p>设置头像、昵称与签名。以你喜欢的方式出现在社区。</p>
      </header>
      <AccountPanel />
    </div>
  );
}
