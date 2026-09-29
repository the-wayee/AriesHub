import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { CaseArtwork } from "@/components/case-card";
import { conceptCases, formatConceptPrice } from "@/lib/concept-cases";

export function generateStaticParams() {
  return conceptCases.map((item) => ({ slug: item.slug }));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}): Promise<Metadata> {
  const { slug } = await params;
  const item = conceptCases.find((entry) => entry.slug === slug);
  return { title: item?.title ?? "案例", description: item?.summary };
}

export default async function CasePage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const item = conceptCases.find((entry) => entry.slug === slug);
  if (!item) notFound();

  return (
    <article className="concept-detail">
      <nav className="concept-breadcrumb" aria-label="面包屑">
        <Link href="/cases">案例</Link>
        <span>/</span>
        <Link href={`/cases?category=${item.categorySlug}`}>
          {item.category}
        </Link>
      </nav>
      <div className="concept-detail-main">
        <div className="concept-detail-primary">
          <header className="concept-detail-heading">
            <p className="mono-label">
              CASE STUDY / {item.category.toUpperCase()}
            </p>
            <h1>{item.title}</h1>
            <p>{item.summary}</p>
            <small>
              更新&nbsp; {item.date} &nbsp; | &nbsp; 形式&nbsp; 图文案例 &nbsp;
              | &nbsp; {item.category}
            </small>
          </header>
          <CaseArtwork item={item} priority />
        </div>
        <aside className="concept-detail-sidebar" aria-label="案例内容与价格">
          <p className="mono-label">PRICE / SINGLE CASE</p>
          <div className="concept-price">
            <strong>{formatConceptPrice(item.price)}</strong>
            <span>
              单个案例
              <br />
              按需阅读
            </span>
          </div>
          <Link
            className="solid-action"
            href={
              item.price === 0
                ? `/learn/${item.slug}`
                : `/checkout/${item.slug}`
            }
          >
            {item.price === 0 ? "开始阅读" : "查看购买信息"}
            <span aria-hidden="true">→</span>
          </Link>
          <a className="outline-action" href="#preview">
            阅读免费预览 <span aria-hidden="true">→</span>
          </a>
          <div className="concept-deliverables">
            <p className="mono-label">YOU WILL GET</p>
            {item.deliverables.map((entry, index) => (
              <div key={entry}>
                <span className="deliverable-icon" aria-hidden="true">
                  {["▤", "◇", "▣", "✳"][index % 4]}
                </span>
                <strong>{entry}</strong>
              </div>
            ))}
          </div>
          <div className="concept-support">
            <p className="mono-label">THE APPROACH</p>
            <strong>独立创作，完整记录</strong>
            <p>从起点到交付，把做法和取舍都留在案例里。</p>
          </div>
        </aside>
      </div>
      <div id="preview" className="concept-detail-lower">
        <section aria-labelledby="contents-title">
          <p className="mono-label">CONTENTS</p>
          <h2 id="contents-title">内容目录</h2>
          <ol>
            {item.chapters.map((chapter, index) => (
              <li key={chapter.title}>
                <span>{String(index + 1).padStart(2, "0")}</span>
                <span>{chapter.title}</span>
                <small>{chapter.free ? "免费" : "完整内容"}</small>
              </li>
            ))}
          </ol>
        </section>
        <section aria-labelledby="for-title">
          <p className="mono-label">FOR WHOM</p>
          <h2 id="for-title">适合谁</h2>
          <ul>
            <li>想把 AI 用在具体项目里的实践者</li>
            <li>希望看清完整制作过程的人</li>
            <li>愿意从真实案例中提炼自己方法的人</li>
          </ul>
        </section>
        <section aria-labelledby="before-title">
          <p className="mono-label">BEFORE YOU START</p>
          <h2 id="before-title">使用前需要</h2>
          <ul>
            {item.requirements.map((requirement) => (
              <li key={requirement}>{requirement}</li>
            ))}
          </ul>
          <h3>案例说明</h3>
          <p>
            本页展示的是前端设计原型。购买、账号权益和资源交付将在业务方案确定后接入。
          </p>
        </section>
      </div>
    </article>
  );
}
