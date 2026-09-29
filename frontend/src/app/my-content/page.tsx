import type { Metadata } from "next";
import Link from "next/link";
import { CaseArtwork } from "@/components/case-card";
import { conceptCases, formatConceptPrice } from "@/lib/concept-cases";

export const metadata: Metadata = { title: "我的内容" };

export default function MyContentPage() {
  const [featured, ...cases] = conceptCases;
  return (
    <div className="concept-library">
      <div className="library-main">
        <header>
          <p className="mono-label">A LIBRARY FOR A MORE CREATIVE YOU.</p>
          <h1>我的内容</h1>
          <p>好的案例，会一直陪伴你的实践。</p>
        </header>
        <section>
          <div className="library-section-heading">
            <h2>继续阅读 / 01</h2>
            <span>PICK UP WHERE YOU LEFT OFF</span>
          </div>
          <Link className="library-feature" href={`/learn/${featured.slug}`}>
            <CaseArtwork item={featured} />
            <div>
              <p className="mono-label">AI × WEB</p>
              <h3>{featured.title}</h3>
              <p>从想法到上线，沿着一个完整项目继续探索。</p>
              <span>进入阅读 →</span>
            </div>
          </Link>
        </section>
        <section>
          <div className="library-section-heading">
            <h2>为你推荐 / 03</h2>
            <span>NEW FOR YOU</span>
          </div>
          {cases.map((item) => (
            <Link
              className="library-row"
              href={`/cases/${item.slug}`}
              key={item.slug}
            >
              <CaseArtwork item={item} />
              <div>
                <p className="mono-label">{item.category}</p>
                <h3>{item.title}</h3>
                <p>{item.summary}</p>
              </div>
              <strong>{formatConceptPrice(item.price)}</strong>
              <span>查看案例 →</span>
            </Link>
          ))}
        </section>
        <p className="community-prototype-note">
          本页为个人内容页面的设计原型，不代表账号已购买或保存这些案例。
        </p>
      </div>
      <aside className="library-aside">
        <p className="mono-label">WHAT’S NEW</p>
        <h2>最新更新</h2>
        <div>
          <small>2024.12.10</small>
          <h3>新案例：从零做一个可上线的网站</h3>
          <p>一个完整项目，从想法到完成。</p>
          <Link href={`/cases/${featured.slug}`}>查看详情 →</Link>
        </div>
        <div>
          <small>2024.12.02</small>
          <h3>探索更顺畅的 AI 工作流</h3>
          <p>把重复交给系统，把注意力留给创作。</p>
          <Link href="/cases/ai-workflow">查看详情 →</Link>
        </div>
        <blockquote>“更好的创作，来自持续的阅读与实践。”</blockquote>
      </aside>
    </div>
  );
}
