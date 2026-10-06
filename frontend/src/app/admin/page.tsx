import type { Metadata } from "next";
import { AdminPublicationList } from "@/components/admin-publication-list";

export const metadata: Metadata = { title: "内容后台" };

export default function AdminPage() {
  return (
    <div className="admin-page">
      <header className="page-heading admin-heading">
        <p className="eyebrow">工作台 / 内容管理</p>
        <h1>内容后台</h1>
        <p>整理实战案例、文章与课程。从草稿开始，按你的节奏发布。</p>
      </header>
      <AdminPublicationList />
    </div>
  );
}
