"use client";

import { ArrowRight, MessageCircle, Send } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { conceptPublications } from "@/lib/concept-publications";
import { useCommunityState, type LocalTopic } from "./local-state";

const samples: LocalTopic[] = [
  {
    id: "sample-path",
    tag: "实战问答",
    title: "第一版应该保留哪些功能？",
    body: "我在复现网站案例时，总觉得还能再加一点。怎样判断什么时候该停下来？",
    replies: [
      "陈默：先保证一条主路径从头到尾都能走通。",
      "Aries：把暂时不会改变结果的功能先放下。",
    ],
  },
  {
    id: "sample-slides",
    tag: "作品反馈",
    title: "内容和视觉，应该先调整哪一个？",
    body: "如果只有一个下午完成演示文稿，你会如何安排优先级？",
    replies: ["周野：我会先把叙事顺序写成只有文字的版本。"],
  },
  {
    id: "sample-flow",
    tag: "工具与流程",
    title: "哪些重复工作值得先自动化？",
    body: "我想从一个每天都会做的小任务开始，正在收集大家的判断方式。",
    replies: [],
  },
];

function TopicCard({ topic }: { topic: LocalTopic }) {
  const { reply } = useCommunityState();
  const [value, setValue] = useState("");
  const send = () => {
    const body = value.trim();
    if (!body) return;
    reply(topic, `我：${body}`);
    setValue("");
  };
  return (
    <article className="hub-topic hub-enter">
      <div className="hub-topic-author">
        <i>{topic.id.startsWith("local") ? "我" : "A"}</i>
        <div>
          <strong>{topic.id.startsWith("local") ? "我" : "社区成员"}</strong>
          <small>{topic.tag} · 社区预览</small>
        </div>
      </div>
      <h2>{topic.title}</h2>
      <p>{topic.body}</p>
      {topic.replies.length > 0 && (
        <div className="hub-topic-replies">
          {topic.replies.map((text) => (
            <p key={text}>{text}</p>
          ))}
        </div>
      )}
      <div className="hub-reply-box">
        <Input
          aria-label={`回复：${topic.title}`}
          value={value}
          onChange={(event) => setValue(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") send();
          }}
          placeholder="写下你的想法…"
          maxLength={240}
        />
        <Button
          size="icon"
          onClick={send}
          disabled={!value.trim()}
          aria-label="发送回复"
        >
          <Send />
        </Button>
      </div>
    </article>
  );
}

export function DiscussionView({ about }: { about?: string }) {
  const { state, addTopic } = useCommunityState();
  const [creating, setCreating] = useState(false);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const related = conceptPublications.find((item) => item.slug === about);
  const topics = useMemo(() => [...state.topics, ...samples], [state.topics]);
  function publish() {
    if (!title.trim() || !body.trim()) return;
    addTopic({
      id: `local-${crypto.randomUUID()}`,
      title: title.trim(),
      body: body.trim(),
      tag: related ? `围绕《${related.shortTitle}》` : "实践分享",
      replies: [],
    });
    setTitle("");
    setBody("");
    setCreating(false);
  }
  return (
    <>
      <header className="hub-page-heading hub-enter">
        <p className="hub-kicker">COMMUNITY</p>
        <h1>
          问题有回声，
          <br />
          <span>实践有同路人。</span>
        </h1>
        <p>围绕主理人的内容，交流具体问题、复现过程和自己的发现。</p>
      </header>
      <div className="hub-discussion-toolbar">
        <div className="hub-presence">
          <i>
            林<span />
          </i>
          <i>
            周<span />
          </i>
          <i>
            陈<span />
          </i>
          <i>
            宋<span />
          </i>
        </div>
        <p>社区场景预览</p>
        <Button onClick={() => setCreating(!creating)}>
          <MessageCircle />
          {creating ? "收起" : "发起讨论"}
        </Button>
      </div>
      {creating && (
        <section className="hub-compose hub-enter">
          <small>
            {related ? `围绕《${related.shortTitle}》` : "分享一次真实尝试"}
          </small>
          <Input
            aria-label="讨论标题"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="你想和大家讨论什么？"
            maxLength={80}
          />
          <Textarea
            aria-label="讨论内容"
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="写下背景、已经尝试过什么，以及卡在哪里…"
            maxLength={800}
          />
          <div>
            <span>{body.length}/800</span>
            <Button onClick={publish} disabled={!title.trim() || !body.trim()}>
              发布到当前浏览器
            </Button>
          </div>
        </section>
      )}
      <div className="hub-discussion-grid">
        <div className="hub-topic-list">
          {topics.map((topic) => (
            <TopicCard key={topic.id} topic={topic} />
          ))}
        </div>
        <aside className="hub-community-note hub-enter">
          <p className="hub-kicker">A NOTE FROM ARIES</p>
          <h2>好的问题，值得被认真讨论。</h2>
          <p>
            具体的背景、真实的尝试，以及你为什么做这个选择，比一个标准答案更有价值。
          </p>
          <hr />
          <h3>讨论从这里开始</h3>
          <Link href={`/publications/${conceptPublications[0].slug}`}>
            先看一篇实战案例 <ArrowRight />
          </Link>
          <Link href="/members">
            认识同路人 <ArrowRight />
          </Link>
        </aside>
      </div>
    </>
  );
}
