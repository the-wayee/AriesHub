import type { Metadata } from "next";
import Link from "next/link";
import { CaseArtwork } from "@/components/case-card";
import { conceptCases } from "@/lib/concept-cases";

export const metadata: Metadata = { title: "一起动手的人" };

const members = [
  { name: "林小雨", focus: "喜欢用 AI 记录和重组日常" },
  { name: "周野", focus: "关注 AI 与个人效率" },
  { name: "陈默", focus: "对影像和叙事感兴趣" },
  { name: "许航", focus: "探索 AI 在产品设计中的应用" },
  { name: "宋言", focus: "喜欢折腾各种 AI 工具" },
  { name: "陆川", focus: "关注 AI 与教育的可能性" },
];

export default function MembersPage() {
  return (
    <div className="concept-members">
      <header>
        <p className="mono-label">08 / SAME CURIOSITY, DIFFERENT PATHS</p>
        <h1>一起动手的人</h1>
        <p>在这里，看到提问、实践与分享背后真实而具体的人。</p>
      </header>
      <div className="members-feature">
        <div className="members-portrait" aria-hidden="true">
          A
        </div>
        <div>
          <p className="mono-label">CREATOR / ARIESHUB</p>
          <h2>Aries</h2>
          <h3>创作者 / 内容主理人</h3>
          <p>
            分享在工作与生活中真实发生的 AI
            探索：完整路线、工具选择、踩过的坑，以及最后做出来的结果。
          </p>
          <Link href="/cases">查看发布的全部案例 →</Link>
        </div>
        <div className="members-still">
          <CaseArtwork item={conceptCases[0]} />
        </div>
      </div>
      <section>
        <div className="members-section-heading">
          <h2>社区成员</h2>
          <span>SAME PEOPLE / DIFFERENT PATHS</span>
        </div>
        <div className="members-grid">
          {members.map((member, index) => (
            <article key={member.name}>
              <span className="member-number">
                {String(index + 1).padStart(2, "0")}
              </span>
              <div className="member-portrait" aria-hidden="true">
                {member.name.slice(0, 1)}
              </div>
              <h3>{member.name}</h3>
              <p>{member.focus}</p>
            </article>
          ))}
        </div>
      </section>
      <p className="community-prototype-note">
        人物与内容为版式示例，成员资料将在社区功能上线后由真实用户提供。
      </p>
    </div>
  );
}
