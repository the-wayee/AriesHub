import type { Metadata } from "next";
import Link from "next/link";
import { ArrowRight } from "lucide-react";

export const metadata: Metadata = { title: "同路人" };
const members = [
  ["林", "林小雨", "正在复现部署步骤"],
  ["周", "周野", "关注 AI 与个人效率"],
  ["陈", "陈默", "对影像和叙事感兴趣"],
  ["许", "许航", "探索 AI 与产品设计"],
  ["宋", "宋言", "喜欢折腾实用工作流"],
  ["陆", "陆川", "关注 AI 与教育"],
];
export default function MembersPage() {
  return (
    <>
      <header className="hub-page-heading hub-enter">
        <p className="hub-kicker">SAME CURIOSITY, DIFFERENT PATHS</p>
        <h1>
          因为实践，
          <br />
          <span>在这里相遇。</span>
        </h1>
        <p>看看同路人正在关注什么，也分享你自己的发现。</p>
      </header>
      <section className="hub-creator-card hub-enter">
        <i>A</i>
        <div>
          <p className="hub-kicker">CREATOR / ARIESHUB</p>
          <h2>Aries</h2>
          <p>
            持续分享真实发生的 AI
            探索：完整路线、工具选择、踩过的坑，以及最后做出来的结果。
          </p>
          <Link href="/discover">
            查看主理人的内容 <ArrowRight />
          </Link>
        </div>
      </section>
      <div className="hub-members-grid">
        {members.map(([initial, name, focus]) => (
          <article className="hub-enter" key={name}>
            <i>
              {initial}
              <span />
            </i>
            <h2>{name}</h2>
            <p>{focus}</p>
            <small>成员资料版式示例</small>
          </article>
        ))}
      </div>
    </>
  );
}
