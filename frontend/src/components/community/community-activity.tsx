"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  ArrowUpRight,
  MessagesSquare,
  UserPlus,
  FileText,
  Heart,
  Bookmark,
  Share2,
  MessageCircle,
  Reply,
  ThumbsUp,
  type LucideIcon,
} from "lucide-react";
import gsap from "gsap";
import { useGSAP } from "@gsap/react";
gsap.registerPlugin(useGSAP);
import { apiRequest } from "@/lib/api";
import type { components } from "@/lib/api-schema";
import { UserAvatar } from "../user-avatar";
import { useAuthSession } from "../auth-session";
import { Button } from "../ui/button";

type Activity = components["schemas"]["CommunityActivity"];
type ActivityPage = components["schemas"]["CommunityActivityPage"];
type Filter = components["schemas"]["CommunityActivityFilter"];
const TABS: { key: Filter; label: string }[] = [
  { key: "ALL", label: "全部动态" },
  { key: "COMMENTS", label: "评论与回复" },
  { key: "PUBLICATIONS", label: "新文章" },
  { key: "INTERACTIONS", label: "互动" },
  { key: "MEMBERS", label: "新成员" },
];
const ACTIONS: Record<Activity["kind"], { label: string; Icon: LucideIcon }> = {
  MEMBER_JOINED: { label: "加入了社区", Icon: UserPlus },
  PUBLICATION_PUBLISHED: { label: "发布了文章", Icon: FileText },
  PUBLICATION_LIKE: { label: "点赞了文章", Icon: Heart },
  PUBLICATION_BOOKMARK: { label: "收藏了文章", Icon: Bookmark },
  PUBLICATION_SHARE: { label: "分享了文章", Icon: Share2 },
  DISCUSSION_COMMENTED: { label: "评论了", Icon: MessageCircle },
  DISCUSSION_REPLIED: { label: "回复了", Icon: Reply },
  DISCUSSION_LIKED: { label: "赞了评论", Icon: ThumbsUp },
};
const VISIBLE_ROWS = 3;
const ROW_HEIGHT = 92;
/** 接口与动画各自完成后才提交数据，避免快速接口提前打断一秒动效。 */
type RefreshCycle = {
  id: number;
  ready: Promise<void>;
  finishAnimation: () => void;
};

/** 当前动态与历史共用服务器筛选；切换 Tab 保留旧列表，完成动效后只提交最新请求的结果。 */
export function CommunityActivityFeed() {
  const { user } = useAuthSession();
  const root = useRef<HTMLElement>(null);
  const [filter, setFilter] = useState<Filter>("ALL");
  const refreshBusy = useRef(false);
  const refreshSerial = useRef(0);
  const [refreshCycle, setRefreshCycle] = useState<RefreshCycle | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const startRefresh = useCallback((replace = false) => {
    if (refreshBusy.current && !replace) return;
    refreshBusy.current = true;
    const id = ++refreshSerial.current;
    let finishAnimation!: () => void;
    const ready = new Promise<void>((resolve) => {
      finishAnimation = resolve;
    });
    setRefreshing(true);
    setRefreshCycle({ id, ready, finishAnimation });
  }, []);
  const selectFilter = useCallback(
    (next: Filter) => {
      if (next === filter) return;
      setFilter(next);
      // Tab 切换替换当前刷新代次；旧分类响应和旧完成动画不能提交到新分类。
      startRefresh(true);
    },
    [filter, startRefresh],
  );
  const finishRefresh = useCallback((id: number) => {
    if (id !== refreshSerial.current) return;
    refreshBusy.current = false;
    setRefreshing(false);
  }, []);
  useGSAP(
    () => {
      if (!refreshCycle) return;
      gsap.delayedCall(1, refreshCycle.finishAnimation);
      return refreshCycle.finishAnimation;
    },
    { scope: root, dependencies: [refreshCycle], revertOnUpdate: true },
  );
  useEffect(() => {
    if (!user) return;
    const refresh = () => {
      // 悬浮阅读或键盘操作时保留当前动态，避免自动刷新打断用户。
      if (
        document.visibilityState === "visible" &&
        !root.current?.matches(":hover") &&
        !root.current?.contains(document.activeElement)
      )
        startRefresh();
    };
    window.addEventListener("arieshub:publication", refresh);
    return () => {
      window.removeEventListener("arieshub:publication", refresh);
    };
  }, [user, startRefresh]);
  return (
    <section
      ref={root}
      className="community-activity-widget"
      aria-label="社区动态"
    >
      <div className="hub-section-heading">
        <h2 className="community-title community-title-violet">
          <MessagesSquare size={21} />
          社区的此刻
        </h2>
      </div>
      <div
        className="community-widget-tabs"
        role="tablist"
        aria-label="动态类型"
        onKeyDown={(event) => {
          const index = TABS.findIndex((tab) => tab.key === filter);
          const next =
            event.key === "ArrowRight"
              ? (index + 1) % TABS.length
              : event.key === "ArrowLeft"
                ? (index + TABS.length - 1) % TABS.length
                : event.key === "Home"
                  ? 0
                  : event.key === "End"
                    ? TABS.length - 1
                    : null;
          if (next === null) return;
          event.preventDefault();
          selectFilter(TABS[next].key);
          document.getElementById(`activity-tab-${TABS[next].key}`)?.focus();
        }}
      >
        {TABS.map((tab) => (
          <Button
            key={tab.key}
            variant="ghost"
            role="tab"
            id={`activity-tab-${tab.key}`}
            tabIndex={filter === tab.key ? 0 : -1}
            aria-selected={filter === tab.key}
            aria-controls="community-activity-panel"
            onClick={() => selectFilter(tab.key)}
          >
            {tab.label}
          </Button>
        ))}
      </div>
      {user ? (
        <ActivityStream
          key={user.id}
          filter={filter}
          refreshCycle={refreshCycle}
          refreshing={refreshing}
          onRefreshComplete={finishRefresh}
          onRetry={startRefresh}
        />
      ) : (
        <p className="community-feed-status">登录后查看社区动态。</p>
      )}
    </section>
  );
}

function ActivityStream({
  filter,
  refreshCycle,
  refreshing,
  onRefreshComplete,
  onRetry,
}: {
  filter: Filter;
  refreshCycle: RefreshCycle | null;
  refreshing: boolean;
  onRefreshComplete: (id: number) => void;
  onRetry: () => void;
}) {
  const viewport = useRef<HTMLDivElement>(null);
  const track = useRef<HTMLDivElement>(null);
  const hovered = useRef(false);
  const focused = useRef(false);
  const animation = useRef<gsap.core.Tween | null>(null);
  const [index, setIndex] = useState(0);
  const [batch, setBatch] = useState(0);
  const refreshingNow = useRef(false);
  const generation = useRef(0);
  const pendingCommit = useRef<(() => void) | null>(null);
  const pendingFinish = useRef<(() => void) | null>(null);
  const [completedCycle, setCompletedCycle] = useState<number | null>(null);
  const revealFeedback = useCallback(() => {
    pendingCommit.current?.();
  }, []);
  const finishFeedback = useCallback(() => {
    pendingFinish.current?.();
  }, []);

  const active = useRef(true);
  const busy = useRef(false);
  const cursor = useRef<string | null>(null);
  const [items, setItems] = useState<Activity[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const path = `/api/v1/community/activities?filter=${filter}&size=12`;
  useEffect(() => {
    active.current = true;
    generation.current++;
    let current = true;
    // 保留旧列表和轨道位置；接口与一秒动画并行，二者完成才一次性替换。
    void Promise.all([
      apiRequest<ActivityPage>(path, { cache: "no-store" }),
      refreshCycle?.ready ?? Promise.resolve(),
    ]).then(([result]) => {
      if (!current) return;
      const commit = () => {
        if (!current) return;
        if (result.ok) {
          setItems(result.data.items);
          setIndex(0);
          setBatch((value) => value + 1);
          cursor.current = result.data.nextCursor;
          setNextCursor(result.data.nextCursor);
          setLoaded(true);
          setError(undefined);
        } else setError(result.error.msg);
        pendingCommit.current = null;
        busy.current = false;
        setLoading(false);
      };
      const finish = () => {
        if (!current) return;
        pendingFinish.current = null;
        setCompletedCycle(null);
        if (refreshCycle) onRefreshComplete(refreshCycle.id);
      };
      if (result.ok && refreshCycle && refreshingNow.current) {
        // 完成动效开始退场时提交数据，让遮罩和新列表同时交叉渐变，避免先留白再跳出。
        pendingCommit.current = commit;
        pendingFinish.current = finish;
        setCompletedCycle(refreshCycle.id);
      } else {
        commit();
        finish();
      }
    });
    return () => {
      current = false;
      active.current = false;
      pendingCommit.current = null;
      pendingFinish.current = null;
    };
  }, [path, refreshCycle, onRefreshComplete]);

  const loadMore = useCallback(async () => {
    const before = cursor.current;
    if (!before || busy.current || !active.current) return;
    const requestGeneration = generation.current;
    busy.current = true;
    setLoading(true);
    setError(undefined);
    const result = await apiRequest<ActivityPage>(
      `${path}&before=${encodeURIComponent(before)}`,
      { cache: "no-store" },
    );
    if (active.current && generation.current === requestGeneration) {
      if (result.ok) {
        // 唯一事件 ID 去重，快速滚动或重试不重复插入；失败时保留已有列表和游标。
        setItems((previous) => [
          ...new Map(
            [...previous, ...result.data.items].map((item) => [item.id, item]),
          ).values(),
        ]);
        cursor.current = result.data.nextCursor;
        setNextCursor(result.data.nextCursor);
      } else setError(result.error.msg);
      setLoading(false);
    }
    if (generation.current === requestGeneration) busy.current = false;
  }, [path]);

  useEffect(() => {
    // 快到已加载列表末尾时预取历史；读取失败保留当前轮播，等待用户重试。
    if (
      loaded &&
      nextCursor &&
      !error &&
      !loading &&
      !refreshing &&
      index + 5 >= items.length
    )
      void loadMore();
  }, [
    loaded,
    nextCursor,
    error,
    loading,
    refreshing,
    index,
    items.length,
    loadMore,
  ]);

  const displayedRows = Math.min(items.length, VISIBLE_ROWS);
  const canAnimate = items.length > 1;
  const syncPlayback = useCallback(() => {
    const shouldPause =
      hovered.current ||
      focused.current ||
      refreshingNow.current ||
      document.visibilityState !== "visible" ||
      window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (shouldPause) animation.current?.pause();
    else animation.current?.resume();
  }, []);
  useGSAP(
    () => {
      if (!canAnimate || !track.current) return;
      // 底部预放一条缓冲行，匀速向上移动。结束后换行并复位，视觉位置保持连续。
      // 预取历史只补充数据，不重建当前 tween，因此不会突然跳回起点。
      animation.current = gsap.fromTo(
        track.current,
        { y: 0 },
        {
          y: -ROW_HEIGHT,
          duration: 5,
          ease: "none",
          paused: true,
          onComplete: () => setIndex((value) => value + 1),
        },
      );
      syncPlayback();
      return () => {
        animation.current = null;
      };
    },
    {
      scope: viewport,
      dependencies: [index, canAnimate, syncPlayback, batch],
      revertOnUpdate: true,
    },
  );
  useGSAP(
    () => {
      // 仅暂停/恢复当前 tween，不重建轨道，刷新点击的那一帧也不跳回起点。
      refreshingNow.current = refreshing;
      syncPlayback();
    },
    { scope: viewport, dependencies: [refreshing, syncPlayback] },
  );
  useGSAP(
    () => {
      if (batch > 1)
        gsap.fromTo(
          viewport.current,
          { opacity: 0 },
          { opacity: 1, duration: 0.42, ease: "power1.inOut" },
        );
    },
    { scope: viewport, dependencies: [batch], revertOnUpdate: true },
  );
  useEffect(() => {
    const preference = window.matchMedia("(prefers-reduced-motion: reduce)");
    document.addEventListener("visibilitychange", syncPlayback);
    preference.addEventListener("change", syncPlayback);
    return () => {
      document.removeEventListener("visibilitychange", syncPlayback);
      preference.removeEventListener("change", syncPlayback);
    };
  }, [syncPlayback]);

  useEffect(() => {
    // 重试按钮随错误消失时浏览器不会触发 blur；重新检查真实焦点，避免轮播永久暂停。
    focused.current = Boolean(
      viewport.current?.parentElement?.contains(document.activeElement),
    );
    syncPlayback();
  }, [error, items.length, syncPlayback]);

  const visible = Array.from(
    { length: displayedRows + (canAnimate ? 1 : 0) },
    (_, slot) => items[canAnimate ? (index + slot) % items.length : slot],
  );
  return (
    <div
      id="community-activity-panel"
      className="community-activity-panel"
      role="tabpanel"
      aria-labelledby={`activity-tab-${filter}`}
      onMouseEnter={() => {
        hovered.current = true;
        syncPlayback();
      }}
      onMouseLeave={() => {
        hovered.current = false;
        syncPlayback();
      }}
      onFocusCapture={() => {
        focused.current = true;
        syncPlayback();
      }}
      onBlurCapture={(event) => {
        focused.current = event.currentTarget.contains(event.relatedTarget);
        syncPlayback();
      }}
    >
      <div
        ref={viewport}
        className="community-activity-list community-feed-scroll"
        style={{ height: VISIBLE_ROWS * ROW_HEIGHT }}
        aria-label="社区动态轮播"
        aria-busy={loading || refreshing}
      >
        <div ref={track} className="community-feed-track">
          {visible.map((item, slot) => {
            const { label, Icon } = ACTIONS[item.kind];
            const hidden = canAnimate && slot === displayedRows;
            return (
              <article
                key={`${slot}:${item.id}`}
                data-kind={item.kind}
                aria-hidden={hidden || undefined}
              >
                <div className="community-event-avatar">
                  <UserAvatar
                    name={item.actorName}
                    url={item.actorAvatarUrl}
                    size={42}
                    tone={Number(BigInt(item.actorId) % BigInt(4))}
                  />
                  <span className="community-event-icon" aria-hidden="true">
                    <Icon size={12} />
                  </span>
                </div>
                <div>
                  <p className="community-activity-byline">
                    <strong>{item.actorName}</strong>
                    <span>{label}</span>
                    <time
                      dateTime={item.createdAt}
                      title={new Date(item.createdAt).toLocaleString("zh-CN")}
                    >
                      {relativeTime(item.createdAt)}
                    </time>
                  </p>
                  {item.href ? (
                    <Link
                      href={item.href}
                      tabIndex={hidden ? -1 : undefined}
                      className={`community-activity-title${item.content ? " community-activity-content" : ""}`}
                    >
                      {item.content ?? item.title}
                      <ArrowUpRight size={14} />
                    </Link>
                  ) : (
                    <p
                      className={`community-activity-title${item.content ? " community-activity-content" : ""}`}
                    >
                      {item.content ?? item.title}
                    </p>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      </div>
      {refreshing && (
        <CommunityRefreshFeedback
          key={refreshCycle?.id}
          phase={completedCycle === refreshCycle?.id ? "complete" : "loading"}
          onReveal={revealFeedback}
          onComplete={finishFeedback}
        />
      )}
      {error ? (
        <div
          className="community-feed-status community-feed-feedback"
          role="alert"
        >
          <p>{error}</p>
          {loaded && nextCursor ? (
            <Button variant="ghost" onClick={() => void loadMore()}>
              重试加载
            </Button>
          ) : (
            <Button variant="ghost" onClick={onRetry}>
              重新加载
            </Button>
          )}
        </div>
      ) : !items.length ? (
        <p className="community-feed-status community-feed-empty" role="status">
          {loading ? "正在加载动态…" : "这里还没有动态。"}
        </p>
      ) : null}
    </div>
  );
}

/** 轨道流动 → 聚拢 → 扩散，退场时与新列表交叉渐变，不使用身份识别图形。 */
function CommunityRefreshFeedback({
  phase,
  onReveal,
  onComplete,
}: {
  phase: "loading" | "complete";
  onReveal: () => void;
  onComplete: () => void;
}) {
  const root = useRef<HTMLDivElement>(null);
  const orbit = useRef<gsap.core.Tween | null>(null);
  useGSAP(
    () => {
      gsap.fromTo(root.current, { opacity: 0 }, { opacity: 1, duration: 0.16 });
      if (!window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
        orbit.current = gsap.to(".community-orbit-points", {
          rotation: 360,
          svgOrigin: "36 36",
          duration: 1,
          ease: "none",
          repeat: -1,
        });
      }
    },
    { scope: root },
  );
  useGSAP(
    () => {
      if (phase !== "complete") return;
      // 保留加载结束时的真实角度，只减速，不把旋转图形突然复位。
      orbit.current?.pause();
      const timeline = gsap.timeline({ onComplete });
      timeline.to(
        ".community-orbit-points",
        {
          rotation: "+=42",
          svgOrigin: "36 36",
          duration: 0.36,
          ease: "power2.out",
        },
        0,
      );
      timeline.to(
        ".community-orbit-dot",
        {
          attr: { cx: 36, cy: 36, r: 3 },
          duration: 0.36,
          ease: "power2.inOut",
        },
        0,
      );
      timeline.to(
        ".community-orbit-core",
        { attr: { r: 4.5 }, opacity: 1, duration: 0.22, ease: "power2.out" },
        0.24,
      );
      timeline.to(".community-orbit-dot", { opacity: 0, duration: 0.12 }, 0.32);
      timeline.fromTo(
        ".community-orbit-halo",
        { attr: { r: 6 }, opacity: 0.7 },
        { attr: { r: 28 }, opacity: 0, duration: 0.48, ease: "power2.out" },
        0.28,
      );
      timeline.call(onReveal, [], 0.42);
      timeline.to(
        root.current,
        { opacity: 0, duration: 0.42, ease: "power1.inOut" },
        0.42,
      );
    },
    {
      scope: root,
      dependencies: [phase, onReveal, onComplete],
      revertOnUpdate: true,
    },
  );
  return (
    <div
      ref={root}
      className="community-refresh-feedback"
      data-phase={phase}
      role="status"
      aria-live="polite"
      aria-label={phase === "loading" ? "正在更新动态" : "动态已更新"}
    >
      <svg
        className="community-refresh-symbol"
        viewBox="0 0 72 72"
        aria-hidden="true"
      >
        <circle className="community-orbit-halo" cx="36" cy="36" r="6" />
        <g className="community-orbit-points">
          <circle className="community-orbit-dot" cx="54" cy="36" r="3.5" />
          <circle className="community-orbit-dot" cx="27" cy="51.6" r="3.5" />
          <circle className="community-orbit-dot" cx="27" cy="20.4" r="3.5" />
        </g>
        <circle className="community-orbit-core" cx="36" cy="36" r="3" />
      </svg>
    </div>
  );
}

function relativeTime(value: string): string {
  const seconds = Math.max(0, (Date.now() - new Date(value).getTime()) / 1000);
  if (seconds < 60) return "刚刚";
  if (seconds < 3600) return `${Math.floor(seconds / 60)} 分钟前`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)} 小时前`;
  if (seconds < 86400 * 7) return `${Math.floor(seconds / 86400)} 天前`;
  return new Date(value).toLocaleDateString("zh-CN", {
    month: "numeric",
    day: "numeric",
  });
}
