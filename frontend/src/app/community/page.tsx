import type { Metadata } from "next";
import Link from "next/link";
import { Button } from "@/components/ui/button";

export const metadata: Metadata = { title: "讨论室" };

const topics = [
  {
    author: "林小雨",
    date: "4 月 12 日",
    title: "从零做一个可上线的网站：部署时遇到问题，该怎么排查？",
    excerpt: "跟着案例完成本地构建后，想进一步弄清上线前需要检查哪些环节。",
    tag: "案例问答",
    replies: "3 条回复",
  },
  {
    author: "Aries",
    date: "4 月 10 日",
    title: "为什么我更推荐先做一个“小而完整”的版本？",
    excerpt: "完成一个可以交付的最小版本，比追求一步到位更能带来真实反馈。",
    tag: "创作者手记",
    replies: "6 条回复",
  },
  {
    author: "陈默",
    date: "4 月 8 日",
    title: "做演示文稿时，怎样保持内容和视觉的一致？",
    excerpt: "如果只有一个下午，你会优先调整结构、文字，还是画面？",
    tag: "作品反馈",
    replies: "2 条回复",
  },
  {
    author: "周野",
    date: "4 月 6 日",
    title: "哪些重复的工作适合先交给 AI 工作流？",
    excerpt: "想为自己的日常任务建立一条真正用得上的自动化流程。",
    tag: "工具与流程",
    replies: "4 条回复",
  },
];

export default function CommunityPage() {
  return (
    <div className="concept-community">
      <div className="community-main">
        <header>
          <p className="mono-label">COMMUNITY / DISCUSSION</p>
          <h1>讨论室</h1>
          <p>让实践中的问题有地方继续生长。</p>
        </header>
        <div className="community-controls">
          <nav aria-label="讨论分类">
            <Link href="/community" aria-current="page">
              全部
            </Link>
            <span>案例问答</span>
            <span>作品反馈</span>
            <span>工具与流程</span>
          </nav>
          <Button disabled>发起讨论 →</Button>
        </div>
        <div className="community-topics">
          {topics.map((topic) => (
            <article key={topic.title}>
              <div className="community-author">
                <span className="avatar-mark" aria-hidden="true">
                  {topic.author.slice(0, 1)}
                </span>
                <strong>{topic.author}</strong>
                <small>{topic.date}</small>
              </div>
              <div>
                <h2>{topic.title}</h2>
                <p>{topic.excerpt}</p>
                <div className="community-topic-meta">
                  <span>{topic.tag}</span>
                  <span>{topic.replies}</span>
                </div>
              </div>
            </article>
          ))}
        </div>
        <p className="community-prototype-note">
          讨论内容为设计原型示例；发布与回复功能将在社区能力确定后接入。
        </p>
      </div>
      <aside className="community-sidebar">
        <section>
          <p className="mono-label">PINNED</p>
          <h2>阅读社区规则 →</h2>
          <p>
            围绕案例交流问题、经验与作品，让每一次讨论都成为下一次实践的参考。
          </p>
        </section>
        <section>
          <p className="mono-label">A NOTE FROM THE CREATOR</p>
          <h2>好的问题，值得被认真讨论。</h2>
          <p>感谢你来到这里。真实的尝试与具体的问题，比标准答案更有价值。</p>
          <span>— Aries</span>
        </section>
        <section>
          <p className="mono-label">PEOPLE</p>
          <h2>一起动手的人</h2>
          <p>认识那些在这里分享实践与思考的人。</p>
          <Link href="/members">查看成员 →</Link>
        </section>
        <section>
          <h2>返回案例目录</h2>
          <p>从一个案例开始，找到值得继续讨论的问题。</p>
          <Link href="/cases">浏览案例 →</Link>
        </section>
      </aside>
    </div>
  );
}
