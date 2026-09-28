import Link from "next/link";

const directions = [
  { number: "01", category: "CREATE", title: "用 AI 做一份好演示", description: "从内容结构到视觉表达，探索 AI PPT 的完整制作过程。", tags: "AI PPT / 模板 / 工作流程", className: "presentation", illustration: "把想法\n讲清楚。" },
  { number: "02", category: "BUILD", title: "把小需求写成工具", description: "借助 Codex，把日常遇到的问题变成一个可以运行的小作品。", tags: "Codex / 网页 / 源码", className: "code", illustration: "> 一个想法\n  一次实践\n  一个作品_" },
  { number: "03", category: "SIMPLIFY", title: "让重复的事简单一点", description: "探索素材整理与日常工作中的自动化，留下可以复用的方法。", tags: "自动化 / 素材 / 效率", className: "workflow", illustration: "收集 → 整理\n       ↓\n     变成成果" },
];

export default function Home() {
  return (
    <>
      <a className="skip-link" href="#main">跳到主要内容</a>
      <header className="site-header wrap">
        <Link className="brand" href="/" aria-label="AriesHub 首页"><span className="brand-mark" aria-hidden="true">a.</span>AriesHub</Link>
        <nav aria-label="主导航"><a href="#explore">探索方向</a><a href="#about">关于这里</a></nav>
        <span className="status"><span aria-hidden="true" />筹备中</span>
      </header>
      <main id="main" className="wrap">
        <section className="hero" aria-labelledby="hero-title">
          <div>
            <p className="eyebrow">A SPACE FOR IDEAS & MAKING</p>
            <h1 id="hero-title">学一点 AI，<br />做一点<span className="accent">真东西。</span></h1>
            <p className="hero-description">让每一次好奇，都留下一个作品。<br />这里将收录 AI 实战案例、制作过程与可复用素材，<br className="desktop-break" />陪你把「我想试试」变成「我做出来了」。</p>
            <a className="primary-link" href="#explore">看看探索方向 <span aria-hidden="true">↗</span></a>
            <p className="launch-note">第一批案例正在准备中，暂未开放购买。</p>
          </div>
          <aside className="field-note" aria-label="AriesHub 的实践理念">
            <div className="note-top"><span>THE MAKER’S NOTEBOOK</span><span>VOL. 001</span></div>
            <div className="note-art" aria-hidden="true"><div className="orbit" /><div className="orbit second" /><span className="asterisk">✳</span></div>
            <div className="note-bottom"><p>从好奇出发，<br />在实践里找到答案。</p><span aria-hidden="true">↗</span></div>
            <div className="note-footer"><span>IDEA → PROCESS → WORK</span><span>ARIESHUB</span></div>
          </aside>
        </section>
        <section id="explore" className="section" aria-labelledby="explore-title">
          <div className="section-heading"><div><p className="eyebrow">FIELDS OF EXPLORATION</p><h2 id="explore-title">从一个具体问题开始</h2></div><p>计划探索的内容方向 · 尚未上架</p></div>
          <div className="direction-grid">
            {directions.map((item) => (
              <article key={item.number}>
                <div className={`direction-art ${item.className}`} aria-hidden="true"><div className="art-label"><span>{item.category}</span><span>/{item.number}</span></div><div className="art-text">{item.illustration}</div></div>
                <p className="tags">{item.tags}</p><h3>{item.title}</h3><p className="direction-description">{item.description}</p>
              </article>
            ))}
          </div>
        </section>
        <section id="about" className="about section" aria-labelledby="about-title">
          <div><p className="eyebrow">LEARN. MAKE. SHARE.</p><h2 id="about-title">把过程留下来，<br />让下一个想法更容易开始。</h2></div>
          <div className="about-copy"><p>AriesHub 是一个从个人实践出发的案例库。我们希望记录一个作品如何诞生：用到的工具、尝试过的方法，以及走过的弯路。</p><p>未来每个案例都会说明适用条件、提供的素材与使用范围。先看懂，再决定是否适合自己。</p><details><summary>现在可以购买或下载案例吗？</summary><p>还不可以。平台正在筹备内容，目前展示的是探索方向。案例准备完成后，将开放详细介绍、预览和获取入口。</p></details></div>
        </section>
      </main>
      <footer className="site-footer wrap"><Link className="brand" href="/">AriesHub<span className="accent">.</span></Link><p>保持好奇，持续动手。</p><a href="#main">回到顶部 ↑</a></footer>
    </>
  );
}
