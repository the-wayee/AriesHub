import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { AdminCaseEditor } from "@/components/admin-case-editor";

export const metadata: Metadata = { title: "编辑案例" };

export default async function EditCasePage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  if (!/^[1-9]\d*$/.test(id)) notFound();
  return <AdminCaseEditor id={id} />;
}
