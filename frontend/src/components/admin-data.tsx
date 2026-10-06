"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import {
  ArrowUpRight,
  Plus,
  Users,
  BookOpen,
  MessageSquare,
  Coins,
  RefreshCw,
} from "lucide-react";
import { adminRequest } from "@/lib/admin";
import type { Overview } from "@/lib/operations";
export function AdminData({ analytics = false }: { analytics?: boolean }) {
  const [data, setData] = useState<Overview>();
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    let active = true;
    void adminRequest<Overview>("/operations/overview").then((r) => {
      if (!active) return;
      if (r.ok) {
        setData(r.data);
        setError("");
      } else setError(r.error.msg);
    });
    return () => {
      active = false;
    };
  }, [attempt]);
  return (
    <div className="ops-page">
      <div className="ops-page-heading">
        <div>
          <p className="ops-kicker">
            ARIESHUB / {analytics ? "INSIGHTS" : "WORKSPACE"}
          </p>
          <h1>{analytics ? "数据分析" : "工作台"}</h1>
          <p>
            {analytics
              ? "从内容与社区的真实记录中，了解每一步增长。"
              : "从一篇好内容开始，连接持续实践的人。"}
          </p>
        </div>
        <Link className="ops-primary" href="/admin/publications/new">
          <Plus size={17} />
          创作内容
        </Link>
      </div>
      {error ? (
        <div className="ops-error" role="alert">
          {error}
          <button onClick={() => setAttempt(attempt + 1)}>
            <RefreshCw size={15} />
            重新加载
          </button>
        </div>
      ) : !data ? (
        <p className="ops-loading">正在汇总社区数据…</p>
      ) : (
        <>
          <div className="ops-stats">
            {[
              {
                label: "社区成员",
                value: data.summary.members,
                detail: `近 7 天新增 ${data.summary.newMembers} 人`,
                icon: Users,
              },
              {
                label: "已发布内容",
                value: data.summary.published,
                detail: `${data.summary.drafts} 篇草稿待整理`,
                icon: BookOpen,
              },
              {
                label: "社区评论",
                value: data.summary.comments,
                detail: "处于公开状态的评论与回复",
                icon: MessageSquare,
              },
              {
                label: "内容解锁",
                value: data.summary.unlocks,
                detail: `累计消耗 ${data.summary.creditsSpent.toLocaleString()} 积分`,
                icon: Coins,
              },
            ].map((x) => (
              <article key={x.label} className="ops-stat">
                <div>
                  <span>{x.label}</span>
                  <x.icon size={18} />
                </div>
                <strong>{x.value.toLocaleString()}</strong>
                <small>{x.detail}</small>
              </article>
            ))}
          </div>
          <div className="ops-dashboard-grid">
            <section className="ops-panel ops-chart">
              <header>
                <div>
                  <p className="ops-kicker">COMMUNITY ACTIVITY</p>
                  <h2>社区正在生长</h2>
                </div>
                <span className="ops-pill">最近 7 天</span>
              </header>
              <div className="chart-legend">
                <span>
                  <i />
                  新成员
                </span>
                <span>
                  <i />
                  新发布
                </span>
                <span>
                  <i />
                  新评论
                </span>
              </div>
              <div
                className="activity-chart"
                role="img"
                aria-label="近七天新成员、发布和评论数量"
              >
                {data.days.map((d) => (
                  <div className="chart-day" key={d.date}>
                    <div className="chart-bars">
                      {[d.registrations, d.publications, d.comments].map(
                        (v, i) => (
                          <div
                            key={i}
                            style={{
                              height: `${v === 0 ? 2 : Math.max(6, (v / Math.max(1, ...data.days.flatMap((x) => [x.registrations, x.publications, x.comments]))) * 160)}px`,
                            }}
                            title={`${d.date} · ${["新成员", "新发布", "新评论"][i]} ${v}`}
                          />
                        ),
                      )}
                    </div>
                    <small>{d.date.slice(5).replace("-", "/")}</small>
                  </div>
                ))}
              </div>
              <p className="ops-footnote">
                统计近 7 日的成员注册、内容发布和公开评论，按日汇总。
              </p>
            </section>
            <section className="ops-panel ops-editorial">
              <p className="ops-kicker">YOUR NEXT STORY</p>
              <h2>
                让想法
                <br />
                成为下一篇好内容。
              </h2>
              <p>分享可复现的 AI 实践，整理学习路径，或把经验写成一门课程。</p>
              <Link href="/admin/publications/new">
                开始写作 <ArrowUpRight size={18} />
              </Link>
              <div className="editorial-lines">
                <span />
                <span />
                <span />
              </div>
            </section>
          </div>
          <div className="ops-dashboard-grid ops-bottom-grid">
            <section className="ops-panel">
              <header>
                <h2>运营待办</h2>
                <Link href="/admin/publications">
                  查看内容 <ArrowUpRight size={14} />
                </Link>
              </header>
              <Link
                className="ops-task"
                href="/admin/publications?status=DRAFT"
              >
                <span className="ops-task-icon">
                  <BookOpen size={19} />
                </span>
                <div>
                  <strong>整理待发布内容</strong>
                  <small>检查正文、封面与积分价格</small>
                </div>
                <b>{data.summary.drafts}</b>
                <ArrowUpRight size={17} />
              </Link>
              <Link className="ops-task" href="/admin/comments">
                <span className="ops-task-icon">
                  <MessageSquare size={19} />
                </span>
                <div>
                  <strong>查看社区讨论</strong>
                  <small>管理评论，维护有价值的交流</small>
                </div>
                <ArrowUpRight size={17} />
              </Link>
            </section>
            <section className="ops-panel">
              <header>
                <h2>社区能力</h2>
                <span className="ops-pill">功能范围</span>
              </header>
              <div className="ops-capability">
                <span>内容与素材</span>
                <small>素材上传 · 私有存储</small>
              </div>
              <div className="ops-capability">
                <span>成员与评论</span>
                <small>账号管理 · 评论审核</small>
              </div>
              <div className="ops-capability">
                <span>推荐运营</span>
                <small>内容精选 · 可在发布设置中管理</small>
              </div>
              <div className="ops-capability">
                <span>聊天与充值支付</span>
                <small>服务尚未接入</small>
              </div>
            </section>
          </div>
          {analytics && (
            <section className="ops-panel ops-financial">
              <h2>积分与内容结构</h2>
              <div className="ops-summary-line">
                <span>
                  已发布积分内容 <b>{data.summary.paid} 篇</b>
                </span>
                <span>
                  成员持有积分{" "}
                  <b>{data.summary.creditBalance.toLocaleString()}</b>
                </span>
                <span>
                  近 7 天登录成员 <b>{data.summary.activeMembers} 人</b>
                </span>
              </div>
              <p className="ops-footnote">
                积分消耗用于衡量内容解锁，不代表人民币营收。充值支付暂未开放。
              </p>
            </section>
          )}
        </>
      )}
    </div>
  );
}
