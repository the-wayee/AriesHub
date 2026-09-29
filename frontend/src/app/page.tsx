import Link from "next/link";
import { CaseArtwork, CaseCard } from "@/components/case-card";
import { MotionHeroCopy, MotionSection } from "@/components/page-motion";
import { conceptCases, formatConceptPrice } from "@/lib/concept-cases";

const [featured, ...moreCases] = conceptCases;

export default function Home() {
  return (
    <div className="landing-page">
      <section className="landing-intro" aria-labelledby="home-title">
        <div className="landing-wordmark" aria-label="AriesHub">
          AriesHub
        </div>
        <div className="landing-intro-note">
          <span>
            REAL PROJECTS
            <br />
            FOR A MORE
            <br />
            CREATIVE TOMORROW.
          </span>
          <p>
            独立创作，
            <br />让 AI 真正为你所用。
          </p>
        </div>
        <MotionHeroCopy>
          <div className="landing-intro-statement">
            <h1 id="home-title">把想法做出来。</h1>
            <p>一人创作的 AI 实战案例与可复用方法</p>
          </div>
        </MotionHeroCopy>
      </section>

      <section className="landing-feature" aria-labelledby="featured-title">
        <div className="landing-feature-copy">
          <p className="mono-label">01 / FEATURED CASE</p>
          <h2 id="featured-title">{featured.title}</h2>
          <p>{featured.summary}</p>
          <div className="landing-feature-purchase">
            <strong>{formatConceptPrice(featured.price)}</strong>
            <Link className="solid-action" href={`/cases/${featured.slug}`}>
              查看案例 <span aria-hidden="true">→</span>
            </Link>
          </div>
          <p className="landing-feature-footnote">
            {featured.tagline}
            <br />
            从构思到完成，看看每一步如何发生。
          </p>
          <small>
            {featured.date} &nbsp; / &nbsp; {featured.category.toUpperCase()}{" "}
            &nbsp; / &nbsp; AI WORKFLOW
          </small>
        </div>
        <Link
          href={`/cases/${featured.slug}`}
          className="landing-feature-image"
          aria-label={`查看${featured.title}`}
        >
          <CaseArtwork item={featured} priority />
        </Link>
        <div className="landing-feature-aside">
          <h3>
            完整过程
            <br />
            实践方法
            <br />
            复盘清单
          </h3>
          <p>从需求拆解、AI 协作到最终完成，记录一个案例真正走过的路径。</p>
          <div className="landing-feature-mini">
            <CaseArtwork item={conceptCases[1]} />
          </div>
          <Link href={`/cases/${featured.slug}`} className="outline-action">
            阅读免费预览 <span aria-hidden="true">→</span>
          </Link>
          <small>
            A PRACTICAL GUIDE
            <br />
            FROM IDEA TO LAUNCH.
          </small>
        </div>
      </section>

      <MotionSection
        id="explore"
        className="landing-case-list"
        aria-label="更多案例"
        direction="up"
      >
        {moreCases.map((item, index) => (
          <CaseCard item={item} index={index + 1} key={item.slug} compact />
        ))}
        <Link className="landing-more" href="/cases">
          浏览全部案例档案 <span aria-hidden="true">↗</span>
        </Link>
      </MotionSection>

      <section id="about" className="landing-about">
        <p className="mono-label">ABOUT / ARIESHUB</p>
        <h2>
          从一个想法，
          <br />
          到一个真正完成的作品。
        </h2>
        <p>
          我们关心创作的过程：为什么这样做、具体怎么做，以及哪些方法值得留下来。每份案例都是一次可以借鉴的实践记录。
        </p>
        <Link href="/cases">从案例开始 →</Link>
      </section>
      <div id="how-it-works" className="landing-endnote">
        LEARN &nbsp; / &nbsp; MAKE &nbsp; / &nbsp; SHARE
      </div>
    </div>
  );
}
