"use client";

import Link from "next/link";
import { useState } from "react";
import {
  ArrowRight,
  MessageCircle,
  Send,
  BookOpen,
  ArrowUpRight,
  UsersRound,
  MessagesSquare,
  Sparkles,
} from "lucide-react";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import type { PublicationCardData } from "@/lib/publication-reader";

/** 社区活动接口接入前的独立展示数据，不写入真实评论、成员或文章关系。 */
const ACTIVITY_PREVIEW = [
  {
    id: "comment-lin",
    name: "林小雨",
    avatar: "林",
    action: "评论了",
    title: "让 AI 写代码前，先把需求说清楚",
    text: "验收清单真的很重要，我把异常场景也补进去以后，返工少了很多。",
    time: "12 分钟前",
    kind: "comment",
  },
  {
    id: "publish-zhou",
    name: "周以宁",
    avatar: "周",
    action: "分享了新文章",
    title: "我的第一个自动化工作流，踩过的三个坑",
    text: "把输入格式、失败重试和人工确认拆开，终于跑通了第一版。",
    time: "28 分钟前",
    kind: "publication",
  },
  {
    id: "comment-song",
    name: "宋言",
    avatar: "宋",
    action: "回复了",
    title: "你们怎么维护个人知识库？",
    text: "我现在只收录真正用过的资料，每周清理一次，比什么都存更有用。",
    time: "43 分钟前",
    kind: "comment",
  },
] as const;
const MEMBERS_PREVIEW = [
  { name: "林小雨", avatar: "林", note: "AI 编程 · 正在做自己的第一款产品" },
  { name: "周以宁", avatar: "周", note: "自动化 · 喜欢把重复工作变成流程" },
  { name: "宋言", avatar: "宋", note: "知识管理 · 记录每一次小尝试" },
] as const;
const DISCUSSIONS_PREVIEW = [
  { title: "第一版产品，应该做到什么程度？", replies: 18, tag: "AI 编程" },
  { title: "你最想用 AI 帮你省下哪一步？", replies: 12, tag: "自动化" },
  { title: "从收藏到实践，你的资料怎么用起来？", replies: 7, tag: "知识管理" },
] as const;

/** 原创矢量示例头像；接入公开成员资料后替换为用户上传图片。 */
function PreviewAvatar({ person }: { person: string }) {
  const profile =
    person === "林"
      ? {
          background: "#cfe5fb",
          shirt: "#4c78b9",
          hair: "#38394e",
          skin: "#efd1bb",
          longHair: true,
        }
      : person === "周"
        ? {
            background: "#e2d8f5",
            shirt: "#8771bb",
            hair: "#3e354c",
            skin: "#e9bf9f",
            longHair: false,
          }
        : {
            background: "#d5ece2",
            shirt: "#569a81",
            hair: "#403d3a",
            skin: "#f3d6ba",
            longHair: false,
          };
  return (
    <svg
      className="community-avatar-art"
      viewBox="0 0 48 48"
      aria-hidden="true"
    >
      <rect width="48" height="48" rx="14" fill={profile.background} />
      {profile.longHair && (
        <path d="M12 29V18C12 3 36 3 36 18v19H12Z" fill={profile.hair} />
      )}
      <path d="M6 48v-7c0-13 36-13 36 0v7" fill={profile.shirt} />
      <path d="M20 29h8v9c-2 3-6 3-8 0Z" fill={profile.skin} />
      <ellipse cx="24" cy="21" rx="10" ry="12" fill={profile.skin} />
      <path
        d={
          profile.longHair
            ? "M13 21V16C13 2 37 3 35 20c-5-1-9-5-12-9-1 7-5 9-10 10Z"
            : "M13 19V16C12 2 37 4 35 20l-5-8c-5 5-11 4-17 7Z"
        }
        fill={profile.hair}
      />
      <path
        d="M19 22h1m8 0h1"
        stroke="#55443d"
        strokeWidth="1.5"
        strokeLinecap="round"
      />
      <path
        d="M22 28q2 2 4 0"
        stroke="#bb8874"
        strokeWidth="1.2"
        fill="none"
        strokeLinecap="round"
      />
    </svg>
  );
}

export function CommunityActivityFeed() {
  const [filter, setFilter] = useState("all");
  return (
    <section className="community-activity-widget" aria-label="社区动态示例">
      <div className="hub-section-heading">
        <div>
          <p className="hub-kicker">AROUND THE COMMUNITY</p>
          <h2 className="community-title community-title-violet">
            <MessagesSquare size={21} />
            社区的此刻
          </h2>
        </div>
        <span className="community-mock-label">动态示例</span>
      </div>
      <div className="community-widget-tabs" aria-label="动态类型筛选">
        {[
          { key: "all", label: "全部动态" },
          { key: "comment", label: "评论与回复" },
          { key: "publication", label: "新文章" },
        ].map((tab) => (
          <Button
            key={tab.key}
            variant="ghost"
            aria-pressed={filter === tab.key}
            onClick={() => setFilter(tab.key)}
          >
            {tab.label}
          </Button>
        ))}
      </div>
      <div className="community-activity-list">
        {ACTIVITY_PREVIEW.filter(
          (item) => filter === "all" || item.kind === filter,
        ).map((item) => (
          <article key={item.id} data-kind={item.kind}>
            <span
              className={`community-member-mark member-${item.avatar}`}
              aria-hidden="true"
            >
              <PreviewAvatar person={item.avatar} />
            </span>
            <div>
              <p className="community-activity-byline">
                <strong>{item.name}</strong>
                <span>{item.action}</span>
                <time>{item.time}</time>
              </p>
              <Link href="/community" className="community-activity-title">
                {item.title}
                <ArrowUpRight size={14} />
              </Link>
              <p className="community-activity-excerpt">{item.text}</p>
              <Link href="/community" className="community-activity-link">
                <MessageCircle size={14} />
                去讨论里聊聊
                <ArrowRight size={14} />
              </Link>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

export function CommunityPreviewRail() {
  const [draft, setDraft] = useState("");
  const [reply, setReply] = useState("");
  function previewReply() {
    if (!draft.trim()) return;
    setReply(draft.trim());
    setDraft("");
  }
  return (
    <>
      <section className="community-members-widget">
        <div className="hub-rail-heading">
          <h2 className="community-title community-title-blue">
            <UsersRound size={19} />
            遇见同路人
          </h2>
          <span className="community-mock-label">成员示例</span>
        </div>
        <p className="community-widget-lede">有人刚开始，有人正走在你前面。</p>
        <div className="community-member-list">
          {MEMBERS_PREVIEW.map((member) => (
            <Link href="/members" key={member.name}>
              <span
                className={`community-member-mark member-${member.avatar}`}
                aria-hidden="true"
              >
                <PreviewAvatar person={member.avatar} />
              </span>
              <div>
                <strong>{member.name}</strong>
                <p>{member.note}</p>
              </div>
              <ArrowUpRight size={14} />
            </Link>
          ))}
        </div>
      </section>
      <section className="community-discussions-widget">
        <div className="hub-rail-heading">
          <h2 className="community-title community-title-rose">
            <MessageCircle size={19} />
            大家在聊
          </h2>
          <span className="community-mock-label">讨论示例</span>
        </div>
        <ol>
          {DISCUSSIONS_PREVIEW.map((item, index) => (
            <li key={item.title}>
              <span>0{index + 1}</span>
              <Link href="/community">
                <strong>{item.title}</strong>
                <small>
                  {item.tag} · {item.replies} 条回复
                </small>
              </Link>
            </li>
          ))}
        </ol>
      </section>
      <section className="community-question-widget">
        <div className="hub-rail-heading">
          <span className="hub-kicker">ONE SMALL QUESTION</span>
          <span className="community-mock-label">互动示例</span>
        </div>
        <h3 className="community-question-title">
          <Sparkles size={18} />
          这周，你用 AI 做成了什么？
        </h3>
        <p>不必是完整产品。一个跑通的脚本、一次顺利的演示，也值得分享。</p>
        <blockquote>
          “第一次把重复的日报整理自动化，省下的时间刚好用来读一篇文章。”
          <span>周以宁 · 示例回答</span>
        </blockquote>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            previewReply();
          }}
          className="hub-reply-box"
        >
          <Input
            aria-label="预览你的回答"
            placeholder="写下一次小尝试…"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            maxLength={240}
          />
          <Button
            type="submit"
            size="icon"
            disabled={!draft.trim()}
            aria-label="预览回答"
          >
            <Send size={15} />
          </Button>
        </form>
        {reply && (
          <p className="community-own-preview" role="status">
            你的回答预览：{reply}
            <small>仅在当前页面展示，尚未发布。</small>
          </p>
        )}
        <Link href="/community" className="hub-inline-link">
          正式发起讨论 <ArrowRight size={14} />
        </Link>
      </section>
      <Link
        href="/discover?type=CASE_STUDY"
        className="community-practice-invite"
      >
        <BookOpen size={22} />
        <div>
          <small>从一个小目标开始</small>
          <h3>本周，复现一个案例。</h3>
          <p>读一遍，做一遍，再把遇到的问题带回来。</p>
          <strong>
            挑选一个实践 <ArrowRight size={14} />
          </strong>
        </div>
      </Link>
    </>
  );
}

/** 发布动态从真实文章摘要构建，其余没有接口的社区场景显式标为示例。 */
export function CommunityPulse({ latest }: { latest?: PublicationCardData }) {
  return (
    <section
      className="hub-activity-strip community-pulse"
      aria-label="社区速览"
    >
      <Link href="/members">
        <span className="hub-avatar-stack" aria-hidden="true">
          <i>
            <PreviewAvatar person="林" />
          </i>
          <i>
            <PreviewAvatar person="周" />
          </i>
          <i>
            <PreviewAvatar person="宋" />
          </i>
        </span>
        <div>
          <strong>和同路人一起实践</strong>
          <small>成员场景示例</small>
        </div>
        <ArrowRight size={15} />
      </Link>
      <Link
        href={latest ? `/publications/${latest.publication.id}` : "/discover"}
      >
        <span className="hub-activity-icon">
          <BookOpen />
        </span>
        <div>
          <strong>{latest ? "有新的内容，值得一读" : "发现下一份实践"}</strong>
          <small>
            {latest?.publication.title ?? "看看主理人整理的案例与文章"}
          </small>
        </div>
        <ArrowRight size={15} />
      </Link>
      <Link href="/community">
        <span className="hub-activity-icon">
          <MessageCircle />
        </span>
        <div>
          <strong>你的想法，也很重要</strong>
          <small>带着问题，加入一次讨论</small>
        </div>
        <ArrowRight size={15} />
      </Link>
    </section>
  );
}
