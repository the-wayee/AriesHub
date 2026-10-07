"use client";

import Link from "next/link";
import { usePublicationReturn } from "@/lib/publication-navigation";

/** 返回实际进入文章的列表，探索页的关键词和分类筛选同时保留。 */
export function PublicationBackLink() {
  const target = usePublicationReturn();
  return (
    <Link className="hub-back" href={target.href}>
      ← {target.label}
    </Link>
  );
}
