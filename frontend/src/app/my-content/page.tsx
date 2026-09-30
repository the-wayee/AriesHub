import type { Metadata } from "next";
import { LibraryView } from "@/components/community/library-view";

export const metadata: Metadata = { title: "我的空间" };
export default function MyContentPage() {
  return <LibraryView />;
}
