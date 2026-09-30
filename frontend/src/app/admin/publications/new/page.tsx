import type { Metadata } from "next";
import { AdminPublicationEditor } from "@/components/admin-publication-editor";

export const metadata: Metadata = { title: "新建内容" };

export default function NewPublicationPage() {
  return <AdminPublicationEditor />;
}
