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
import { PageSkeleton } from "./page-skeleton";
/** 图例、柱形与提示共享同一组指标，避免颜色和数据含义不一致。 */
const ACTIVITY_METRICS = [
  { key: "registrations", label: "新成员", unit: "人", tone: "blue" },
  { key: "publications", label: "新发布", unit: "篇", tone: "green" },
  { key: "comments", label: "新评论", unit: "条", tone: "purple" },
] as const;

export function AdminData({ analytics = false }: { analytics?: boolean }) {
  const [activeDay, setActiveDay] = useState<string | null>(null);
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
  const chartMax = Math.max(
    1,
    ...(data?.days.flatMap((day) =>
      ACTIVITY_METRICS.map((metric) => day[metric.key]),
    ) ?? []),
  );
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
        <PageSkeleton
          variant="dashboard"
          heading={false}
          label="正在汇总社区数据"
        />
      ) : (
        <>
          <div className="ops-stats">
            {[
              {
                label: "社区成员",
                value: data.summary.members,
                detail: `近 7 天新增 ${data.summary.newMembers} 人`,
                icon: Users,
                tone: "blue",
                unit: "人",
              },
              {
                label: "已发布内容",
                value: data.summary.published,
                detail: `${data.summary.drafts} 篇草稿待整理`,
                icon: BookOpen,
                tone: "green",
                unit: "篇",
              },
              {
                label: "社区评论",
                value: data.summary.comments,
                detail: "处于公开状态的评论与回复",
                icon: MessageSquare,
                tone: "purple",
                unit: "条",
              },
              {
                label: "内容解锁",
                value: data.summary.unlocks,
                detail: `累计消耗 ${data.summary.creditsSpent.toLocaleString()} 积分`,
                icon: Coins,
                tone: "amber",
                unit: "次",
              },
            ].map((x) => (
              <article key={x.label} className="ops-stat" data-tone={x.tone}>
                <div>
                  <span>{x.label}</span>
                  <button
                    type="button"
                    className="ops-metric-icon"
                    aria-label={`${x.label}指标说明`}
                    aria-describedby={`metric-${x.tone}`}
                  >
                    <x.icon size={18} />
                  </button>
                  <span
                    role="tooltip"
                    id={`metric-${x.tone}`}
                    className="ops-metric-tooltip"
                  >
                    {x.label}：{x.value.toLocaleString()} {x.unit}。{x.detail}
                  </span>
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
                {ACTIVITY_METRICS.map((metric) => (
                  <span key={metric.key} data-tone={metric.tone}>
                    <i />
                    {metric.label}
                  </span>
                ))}
              </div>
              <div
                className="activity-chart"
                role="group"
                aria-label="近七天新成员、发布和评论数量"
                onMouseLeave={() => setActiveDay(null)}
              >
                {data.days.map((day, index) => (
                  <div
                    className="chart-day"
                    key={day.date}
                    data-active={activeDay === day.date}
                    data-tooltip-align={
                      index < 2
                        ? "left"
                        : index >= data.days.length - 2
                          ? "right"
                          : "center"
                    }
                    onMouseEnter={() => setActiveDay(day.date)}
                  >
                    <button
                      type="button"
                      className="chart-day-trigger"
                      aria-label={`${day.date}，${ACTIVITY_METRICS.map((metric) => `${metric.label} ${day[metric.key]} ${metric.unit}`).join("，")}`}
                      aria-describedby={
                        activeDay === day.date
                          ? `activity-${day.date}`
                          : undefined
                      }
                      onFocus={() => setActiveDay(day.date)}
                      onBlur={() => setActiveDay(null)}
                      onClick={() => setActiveDay(day.date)}
                      onKeyDown={(event) => {
                        if (event.key === "Escape") setActiveDay(null);
                      }}
                    >
                      <span className="chart-bars" aria-hidden="true">
                        {ACTIVITY_METRICS.map((metric) => (
                          <span
                            key={metric.key}
                            data-tone={metric.tone}
                            style={{
                              height: `${day[metric.key] === 0 ? 2 : Math.max(6, (day[metric.key] / chartMax) * 160)}px`,
                            }}
                          />
                        ))}
                      </span>
                      <small>{day.date.slice(5).replace("-", "/")}</small>
                    </button>
                    {activeDay === day.date && (
                      <div
                        className="activity-tooltip"
                        id={`activity-${day.date}`}
                        role="tooltip"
                      >
                        <strong>{day.date}</strong>
                        {ACTIVITY_METRICS.map((metric) => (
                          <div key={metric.key} data-tone={metric.tone}>
                            <i />
                            <span>{metric.label}</span>
                            <b>
                              {day[metric.key].toLocaleString()}
                              <small>{metric.unit}</small>
                            </b>
                          </div>
                        ))}
                      </div>
                    )}
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
