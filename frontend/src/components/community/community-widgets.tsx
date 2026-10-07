"use client";

import Link from "next/link";
import { useState } from "react";
import {
  ArrowRight,
  MessageCircle,
  Send,
  BookOpen,
  Sparkles,
} from "lucide-react";
import { Button } from "../ui/button";
import { Input } from "../ui/input";

const DISCUSSIONS_PREVIEW = [
  { title: "第一版产品，应该做到什么程度？", replies: 18, tag: "AI 编程" },
  { title: "你最想用 AI 帮你省下哪一步？", replies: 12, tag: "自动化" },
  { title: "从收藏到实践，你的资料怎么用起来？", replies: 7, tag: "知识管理" },
] as const;

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
