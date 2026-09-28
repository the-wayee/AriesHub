import type { Metadata } from "next";
import { AdminCaseList } from "@/components/admin-case-list";

export const metadata: Metadata = { title: "内容后台" };

export default function AdminPage() {
  return (
    <div className="admin-page">
      <header className="page-heading admin-heading">
        <p className="eyebrow">ARIESHUB / CONTENT STUDIO</p>
        <h1>内容后台</h1>
        <p>把实践过程整理成案例，再决定何时发布到主站。</p>
      </header>
      <AdminCaseList />
    </div>
  );
}
