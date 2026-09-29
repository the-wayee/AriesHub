import type { Metadata } from "next";
import Link from "next/link";
import { CaseCard } from "@/components/case-card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { conceptCases } from "@/lib/concept-cases";

export const metadata: Metadata = { title: "案例档案" };

const categories = [
  { slug: "", label: "全部" },
  { slug: "web", label: "网站与开发" },
  { slug: "content", label: "内容创作" },
  { slug: "automation", label: "自动化" },
];

export default async function CasesPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const value = (key: string) =>
    typeof params[key] === "string" ? params[key] : "";
  const q = value("q").slice(0, 120);
  const category = value("category");
  const access = value("access");
  const cases = conceptCases.filter(
    (item) =>
      (!q ||
        `${item.title} ${item.summary}`
          .toLowerCase()
          .includes(q.toLowerCase())) &&
      (!category || item.categorySlug === category) &&
      (!access || (access === "FREE" ? item.price === 0 : item.price > 0)),
  );

  return (
    <div className="archive-page">
      <header className="archive-heading">
        <div>
          <p className="mono-label">THE CASE ARCHIVE / 001—004</p>
          <h1>案例档案</h1>
          <p>记录从想法到交付的 AI 实践过程、步骤与思考。</p>
        </div>
        <p className="archive-heading-aside">
          PRACTICE
          <br />
          SHARE
          <br />
          REAL CHANGE.
        </p>
      </header>

      <div className="archive-toolbar">
        <nav className="archive-tabs" aria-label="案例分类">
          {categories.map((tab) => (
            <Link
              key={tab.slug}
              href={tab.slug ? `/cases?category=${tab.slug}` : "/cases"}
              className={category === tab.slug ? "active" : ""}
              aria-current={category === tab.slug ? "page" : undefined}
            >
              {tab.label}
            </Link>
          ))}
        </nav>
        <form action="/cases" className="archive-search">
          {category && <Input name="category" type="hidden" value={category} />}
          <Input
            aria-label="搜索案例"
            name="q"
            type="search"
            placeholder="搜索案例、工具、关键词…"
            defaultValue={q}
          />
          <Select
            name="access"
            defaultValue={access}
            items={[
              { value: "", label: "全部内容" },
              { value: "FREE", label: "免费" },
              { value: "PAID", label: "付费" },
            ]}
          >
            <SelectTrigger aria-label="阅读方式">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="">全部内容</SelectItem>
              <SelectItem value="FREE">免费</SelectItem>
              <SelectItem value="PAID">付费</SelectItem>
            </SelectContent>
          </Select>
          <Button type="submit">搜索 →</Button>
        </form>
      </div>

      <div className="archive-list">
        {cases.length ? (
          cases.map((item, index) => (
            <CaseCard
              key={item.slug}
              item={item}
              index={conceptCases.indexOf(item)}
              featured={index === 0 && !q && !category && !access}
            />
          ))
        ) : (
          <div className="archive-empty">
            <h2>没有找到匹配的案例。</h2>
            <Link href="/cases">清除条件，查看全部 →</Link>
          </div>
        )}
      </div>
      <footer className="archive-page-footer">
        <span>AriesHub&nbsp; — &nbsp; 与 AI 一起，实践更大的可能。</span>
        <span>
          {cases.length.toString().padStart(2, "0")} /{" "}
          {conceptCases.length.toString().padStart(2, "0")} CASES
        </span>
      </footer>
    </div>
  );
}
