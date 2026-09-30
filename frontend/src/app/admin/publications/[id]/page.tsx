import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { AdminPublicationEditor } from "@/components/admin-publication-editor";

export const metadata: Metadata = { title: "编辑内容" };

export default async function EditPublicationPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  if (!/^[1-9]\d*$/.test(id)) notFound();
  return <AdminPublicationEditor id={id} />;
}
