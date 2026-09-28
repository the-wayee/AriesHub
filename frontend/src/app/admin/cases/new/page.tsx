import type { Metadata } from "next";
import { AdminCaseEditor } from "@/components/admin-case-editor";

export const metadata: Metadata = { title: "新建案例" };

export default function NewCasePage() {
  return <AdminCaseEditor />;
}
