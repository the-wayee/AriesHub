"use client";

import Image from "next/image";
import Link from "next/link";
import { ArrowRight, Check, Send, Sparkles } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { conceptCases } from "@/lib/concept-cases";
import { ContentCard } from "./content-card";
import { useCommunityState, type LocalTopic } from "./local-state";

const seedTopic: LocalTopic = {
  id: "preview-first-version",
  tag: "围绕实战案例",
  title: "第一版应该保留哪些功能？",
  body: "怎样才能既完整，又不过度设计？",
  replies: [
    "陈默：先保留一条能走通的主路径。",
    "Aries：把暂时不会改变结果的功能先放下。",
  ],
};

export function HomeView() {
  const { state, reply } = useCommunityState();
  const [message, setMessage] = useState("");
  const topic =
    state.topics.find((item) => item.id === seedTopic.id) || seedTopic;
  const featured = conceptCases[0];
  const chapter = state.history[featured.slug] ?? 1;

  function sendReply() {
    const value = message.trim();
    if (!value) return;
    reply(topic, `我：${value}`);
    setMessage("");
  }

  return (
    <>
      <header className="hub-home-heading hub-enter">
        <p className="hub-kicker">MEMBER HOME</p>
        <h1>晚上好，欢迎回来。</h1>
        <p>有人分享了新的实践，也有人正在等待你的想法。</p>
      </header>
      <section
        className="hub-activity-strip hub-enter"
        aria-label="社区动态预览"
      >
        <Link href="/members">
          <span className="hub-avatar-stack" aria-hidden="true">
            <i>林</i>
            <i>周</i>
            <i>陈</i>
          </span>
          <strong>看看今天来过的成员</strong>
          <ArrowRight />
        </Link>
        <Link href={`/cases/${conceptCases[1].slug}`}>
          <span className="hub-activity-icon">
            <Sparkles />
          </span>
          <strong>Aries 刚发布了一篇文章</strong>
          <ArrowRight />
        </Link>
        <Link href="/community">
          <span className="hub-activity-icon">聊</span>
          <strong>收藏的内容有新讨论</strong>
          <ArrowRight />
        </Link>
      </section>
      <div className="hub-home-grid">
        <div className="hub-home-feed">
          <section className="hub-resume-card hub-enter">
            <div className="hub-resume-art">
              <Image
                src={featured.image}
                alt={featured.title}
                width={900}
                height={560}
                priority
                unoptimized
              />
              <span>继续阅读</span>
            </div>
            <div className="hub-resume-copy">
              <div className="hub-resume-status">
                <Check /> 已加入我的内容
              </div>
              <span className="hub-type">实战案例</span>
              <h2>{featured.title}</h2>
              <p>
                读到 {String(chapter + 1).padStart(2, "0")} ·{" "}
                {featured.chapters[chapter]?.title}
              </p>
              <div className="hub-progress">
                <span
                  style={{
                    width: `${((chapter + 1) / featured.chapters.length) * 100}%`,
                  }}
                />
              </div>
              <Link
                className="hub-primary"
                href={`/learn/${featured.slug}?chapter=${chapter}`}
              >
                继续阅读 <ArrowRight />
              </Link>
            </div>
          </section>
          <div className="hub-section-heading">
            <div>
              <p className="hub-kicker">CURATED FOR YOU</p>
              <h2>为你推荐</h2>
            </div>
            <Link href="/discover">
              查看全部 <ArrowRight />
            </Link>
          </div>
          <div className="hub-recommend-grid">
            {conceptCases.slice(1, 3).map((item, index) => (
              <ContentCard item={item} index={index + 1} key={item.slug} />
            ))}
          </div>
          <div className="hub-section-heading hub-latest-heading">
            <div>
              <p className="hub-kicker">NEW THIS WEEK</p>
              <h2>最新发布</h2>
            </div>
          </div>
          <ContentCard item={conceptCases[3]} index={3} compact />
        </div>
        <aside className="hub-community-rail hub-enter">
          <section className="hub-people-now">
            <div className="hub-rail-heading">
              <h2>社区正在发生</h2>
              <Link href="/members">看看大家</Link>
            </div>
            <div className="hub-presence" aria-label="成员场景预览">
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
          </section>
          <section className="hub-thread-preview">
            <small>讨论示例 · {topic.tag}</small>
            <h3>{topic.title}</h3>
            <p>{topic.body}</p>
            <div className="hub-thread-replies">
              {topic.replies.slice(-3).map((text) => (
                <p key={text}>{text}</p>
              ))}
            </div>
            <div className="hub-reply-box">
              <Input
                value={message}
                onChange={(event) => setMessage(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter") sendReply();
                }}
                placeholder="写下你的想法…"
                aria-label="回复讨论"
                maxLength={240}
              />
              <Button
                size="icon"
                onClick={sendReply}
                disabled={!message.trim()}
                aria-label="发送回复"
              >
                <Send />
              </Button>
            </div>
          </section>
          <section className="hub-practicing">
            <h3>正在实践</h3>
            <p>
              <i>林</i>
              <span>
                <strong>林小雨</strong> 正在复现部署步骤
              </span>
            </p>
            <p>
              <i>许</i>
              <span>
                <strong>许航</strong> 收藏了检查清单
              </span>
            </p>
            <p>
              <i>宋</i>
              <span>
                <strong>宋言</strong> 分享了自己的工作流
              </span>
            </p>
            <small>社区场景预览</small>
          </section>
          <Link href="/community" className="hub-share-invite">
            <span>芽</span>
            <div>
              <h3>你也做成了什么？</h3>
              <p>分享一次尝试，让下一位成员少走一点弯路。</p>
              <strong>发起讨论</strong>
            </div>
          </Link>
        </aside>
      </div>
    </>
  );
}
