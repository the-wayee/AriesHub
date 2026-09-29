import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { conceptCases } from "@/lib/concept-cases";

export const metadata: Metadata = { title: "案例阅读" };

export function generateStaticParams() {
  return conceptCases.map((item) => ({ slug: item.slug }));
}

export default async function LearnPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const item = conceptCases.find((entry) => entry.slug === slug);
  if (!item) notFound();

  return (
    <div className="concept-reader">
      <aside className="reader-toc">
        <Link href={`/cases/${item.slug}`} className="reader-back">
          ← 返回案例
        </Link>
        <h1>{item.title}</h1>
        <p>{item.summary}</p>
        <nav aria-label="章节目录">
          {item.chapters.map((chapter, index) => (
            <a key={chapter.title} href={`#chapter-${index + 1}`}>
              <span>{String(index + 1).padStart(2, "0")}</span>
              {chapter.title}
            </a>
          ))}
        </nav>
      </aside>
      <main className="reader-body">
        <p className="mono-label">01 / READING</p>
        <h2>从一个具体问题开始</h2>
        <p className="reader-lede">
          {item.summary}{" "}
          这份案例把关键步骤与思考整理在一起，方便你对照自己的项目逐步实践。
        </p>
        {item.chapters.map((chapter, index) => (
          <section id={`chapter-${index + 1}`} key={chapter.title}>
            <p className="mono-label">
              {String(index + 1).padStart(2, "0")} /{" "}
              {chapter.free || item.price === 0 ? "OPEN" : "FULL CASE"}
            </p>
            <h3>{chapter.title}</h3>
            {chapter.free || item.price === 0 ? (
              <p>
                从目标出发，明确这一步要解决什么，再把方法应用到自己的情境中。正式内容将在案例编辑完成后更新。
              </p>
            ) : (
              <p className="reader-locked">
                此章节属于完整案例。当前是前端设计原型，内容与权限尚未接入。
              </p>
            )}
          </section>
        ))}
      </main>
      <aside className="reader-resources">
        <h2>本案例资源</h2>
        <p>资源文件将在内容系统确定后接入。</p>
        <div className="reader-resource-list">
          {item.deliverables.map((entry) => (
            <div key={entry}>
              <span aria-hidden="true">▤</span>
              <strong>{entry}</strong>
              <small>待提供</small>
            </div>
          ))}
        </div>
        <h3>授权与使用范围</h3>
        <p>正式授权说明将在购买流程上线前公布。</p>
        <Link href="/community">遇到问题？去讨论 →</Link>
      </aside>
    </div>
  );
}
