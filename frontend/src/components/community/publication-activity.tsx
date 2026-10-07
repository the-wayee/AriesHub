"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import {
  ArrowRight,
  Heart,
  Bookmark,
  Share2,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { useReaderResource } from "../use-reader-resource";
import type { components } from "@/lib/api-schema";
type Activity = components["schemas"]["PublicationActivity"];
const ACTIONS = {
  LIKE: { text: "点赞了", Icon: Heart },
  BOOKMARK: { text: "收藏了", Icon: Bookmark },
  SHARE: { text: "分享了", Icon: Share2 },
};

/** 只轮播服务端真实互动，悬浮、键盘焦点或减少动态效果时暂停自动切换。 */
export function PublicationActivity() {
  const [revision, setRevision] = useState(0);
  const { data, error } = useReaderResource<Activity[]>(
    "/api/v1/home/activity",
    revision,
  );
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  useEffect(() => {
    const refresh = () => setRevision((value) => value + 1);
    window.addEventListener("arieshub:publication", refresh);
    window.addEventListener("focus", refresh);
    return () => {
      window.removeEventListener("arieshub:publication", refresh);
      window.removeEventListener("focus", refresh);
    };
  }, []);
  useEffect(() => {
    if (paused || !data || data.length < 2) return;
    const media = matchMedia("(prefers-reduced-motion: reduce)");
    let timer: ReturnType<typeof setInterval> | undefined;
    const schedule = () => {
      clearInterval(timer);
      if (!media.matches && document.visibilityState === "visible")
        timer = setInterval(
          () => setIndex((value) => (value + 1) % data.length),
          6000,
        );
    };
    schedule();
    media.addEventListener("change", schedule);
    document.addEventListener("visibilitychange", schedule);
    return () => {
      clearInterval(timer);
      media.removeEventListener("change", schedule);
      document.removeEventListener("visibilitychange", schedule);
    };
  }, [data, paused]);
  const item = data?.[index % data.length];
  const action = item ? ACTIONS[item.kind] : undefined;
  return (
    <div
      className="publication-activity"
      aria-label="社区互动动态"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocusCapture={() => setPaused(true)}
      onBlurCapture={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget))
          setPaused(false);
      }}
    >
      {item && action ? (
        <Link
          key={item.id}
          href={`/publications/${item.publicationId}`}
          className="publication-activity-entry"
        >
          <span className="hub-activity-icon" data-kind={item.kind}>
            <action.Icon />
          </span>
          <div>
            <strong>
              {item.actorName} <span>{action.text}</span>
            </strong>
            <small>{item.title}</small>
          </div>
          <ArrowRight size={15} />
        </Link>
      ) : (
        <div className="publication-activity-empty">
          <strong>社区动态</strong>
          <small>
            {error
              ? "动态暂时无法加载"
              : data
                ? "新的互动会出现在这里"
                : "正在加载…"}
          </small>
          {error && (
            <button
              type="button"
              onClick={() => setRevision((value) => value + 1)}
            >
              重试
            </button>
          )}
        </div>
      )}
      {data && data.length > 1 && (
        <div className="publication-activity-controls">
          <button
            type="button"
            aria-label="上一条动态"
            onClick={() =>
              setIndex((value) => (value - 1 + data.length) % data.length)
            }
          >
            <ChevronLeft size={13} />
          </button>
          <span>
            {(index % data.length) + 1} / {data.length}
          </span>
          <button
            type="button"
            aria-label="下一条动态"
            onClick={() => setIndex((value) => (value + 1) % data.length)}
          >
            <ChevronRight size={13} />
          </button>
        </div>
      )}
    </div>
  );
}
