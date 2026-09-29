import Link from "next/link";

import { getCases } from "@/lib/catalog";
import { CaseCard } from "@/components/case-card";
import { ContentState } from "@/components/content-state";
import {
  MotionAsterisk,
  MotionHeroCopy,
  MotionHeroNote,
  MotionOrbit,
  MotionSection,
} from "@/components/page-motion";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";

export const dynamic = "force-dynamic";

export default async function Home() {
  const cases = await getCases("size=3");
  return (
    <>
      <section className="hero" aria-labelledby="hero-title">
        <MotionHeroCopy>
          <p className="eyebrow">A SPACE FOR IDEAS & MAKING</p>
          <h1 id="hero-title">
            学一点 AI，
            <br />
            做一点<span className="accent">真东西。</span>
          </h1>
          <p className="hero-description">
            让每一次好奇，都留下一个作品。
            <br />
            这里将收录 AI 实战案例、制作过程与可复用素材，
            <br className="desktop-break" />
            陪你把「我想试试」变成「我做出来了」。
          </p>
          <div className="hero-actions">
            <Link className="primary-link" href="/cases">
              探索案例库 <span aria-hidden="true">↗</span>
            </Link>
            <Link className="secondary-link" href="/register">
              免费加入社区
            </Link>
          </div>
          <p className="launch-note">
            从免费内容开始阅读，付费案例暂未开放购买。
          </p>
        </MotionHeroCopy>
        <MotionHeroNote>
          <div className="note-top">
            <span>THE MAKER’S NOTEBOOK</span>
            <span>VOL. 001</span>
          </div>
          <div className="note-art" aria-hidden="true">
            <MotionOrbit />
            <MotionOrbit secondary />
            <MotionAsterisk />
          </div>
          <div className="note-bottom">
            <p>
              从好奇出发，
              <br />
              在实践里找到答案。
            </p>
            <span aria-hidden="true">↗</span>
          </div>
          <div className="note-footer">
            <span>IDEA → PROCESS → WORK</span>
            <span>ARIESHUB</span>
          </div>
        </MotionHeroNote>
      </section>
      <MotionSection
        id="how-it-works"
        className="section process-section"
        aria-labelledby="process-title"
        direction="left"
      >
        <div className="section-heading">
          <div>
            <p className="eyebrow">FROM WATCHING TO MAKING</p>
            <h2 id="process-title">少一点收藏吃灰，多一点动手完成。</h2>
          </div>
          <p>每份内容都围绕一个能完成的具体结果。</p>
        </div>
        <ol className="process-grid">
          <li>
            <span>01</span>
            <h3>先看真实案例</h3>
            <p>从结果、适用条件和制作过程判断，这个方法是否适合你。</p>
          </li>
          <li>
            <span>02</span>
            <h3>复用方法与素材</h3>
            <p>使用整理好的提示词、步骤和文件，减少从零摸索的时间。</p>
          </li>
          <li>
            <span>03</span>
            <h3>做出自己的版本</h3>
            <p>把案例变成作品，也把踩坑和心得沉淀为下一次的经验。</p>
          </li>
        </ol>
      </MotionSection>
      <MotionSection
        id="explore"
        className="section"
        aria-labelledby="explore-title"
        direction="right"
      >
        <div className="section-heading">
          <div>
            <p className="eyebrow">THE LATEST FIELD NOTES</p>
            <h2 id="explore-title">从一个具体问题开始</h2>
          </div>
          <Link className="text-link" href="/cases">
            全部案例 ↗
          </Link>
        </div>
        {cases.ok ? (
          cases.data.items.length > 0 ? (
            <div className="direction-grid">
              {cases.data.items.map((item, index) => (
                <CaseCard key={item.id} item={item} index={index} />
              ))}
            </div>
          ) : (
            <ContentState
              title="第一批案例正在准备中"
              message="内容发布后会出现在这里，欢迎稍后再来。"
            />
          )
        ) : (
          <ContentState
            title="案例暂时加载不了"
            message="内容服务暂时不可用，请稍后重试。"
            href="/"
            reload
            label="重新加载首页"
            requestId={cases.requestId}
          />
        )}
      </MotionSection>
      <MotionSection
        className="section join-section"
        aria-labelledby="join-title"
        direction="scale"
      >
        <p className="eyebrow">BUILD YOUR OWN PRACTICE</p>
        <h2 id="join-title">
          今天先学会一个方法，
          <br />
          明天就多一种可能。
        </h2>
        <p>从免费案例开始。账号将用于保存后续的购买、收藏和社区权益。</p>
        <Link className="primary-link" href="/register">
          创建免费账号 <span aria-hidden="true">↗</span>
        </Link>
      </MotionSection>
      <MotionSection
        id="about"
        className="about section"
        aria-labelledby="about-title"
        direction="left"
      >
        <div>
          <p className="eyebrow">LEARN. MAKE. SHARE.</p>
          <h2 id="about-title">
            把过程留下来，
            <br />
            让下一个想法更容易开始。
          </h2>
        </div>
        <div className="about-copy">
          <p>
            AriesHub
            是一个从个人实践出发的案例库。我们希望记录一个作品如何诞生：用到的工具、尝试过的方法，以及走过的弯路。
          </p>
          <p>
            未来每个案例都会说明适用条件、提供的素材与使用范围。先看懂，再决定是否适合自己。
          </p>
          <Accordion className="about-accordion">
            <AccordionItem value="reading">
              <AccordionTrigger>现在可以阅读哪些内容？</AccordionTrigger>
              <AccordionContent>
                <p>
                  已发布的免费案例可以直接阅读；付费案例目前提供预览，暂未开放购买和下载。标注为演示的案例用于体验浏览流程，不作为商品销售。
                </p>
              </AccordionContent>
            </AccordionItem>
          </Accordion>
        </div>
      </MotionSection>
    </>
  );
}
