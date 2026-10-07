"use client";
import { useEffect, useState } from "react";
import { Eye, Share2 } from "lucide-react";
import type { Interaction } from "@/lib/publication-reader";
import { apiRequest } from "@/lib/api";
import { readerToken, recordPublicationEvent } from "@/lib/publication-events";
import { useAuthSession } from "./auth-session";

export function usePublicationMetrics(id: string, initial?: Interaction) {
  const { user, hasSession } = useAuthSession();
  const [snapshot, setSnapshot] = useState<{
    key: string;
    data: Interaction;
    initial?: Interaction;
  }>();
  const key = `${id}:${user?.id ?? "anon"}`;
  const sessionPending = hasSession && user === undefined;
  useEffect(() => {
    if (sessionPending) return;
    let active = true;
    let updated = false;
    if (!initial)
      void apiRequest<Interaction>(`/api/v1/publications/${id}/interaction`, {
        cache: "no-store",
      }).then((r) => {
        if (active && !updated && r.ok)
          setSnapshot({ key, data: r.data, initial });
      });
    const sync = (event: Event) => {
      const data = (event as CustomEvent<Interaction>).detail;
      if (data?.publicationId === id) {
        updated = true;
        setSnapshot({ key, data, initial });
      }
    };
    window.addEventListener("arieshub:publication", sync);
    return () => {
      active = false;
      window.removeEventListener("arieshub:publication", sync);
    };
  }, [id, initial, key, sessionPending]);
  return snapshot?.key === key && snapshot.initial === initial
    ? snapshot.data
    : initial;
}

/** 阅读计数独立于免费正文进度：付费试读也可计入访问，不授予正文权限。 */
export function PublicationMetrics({
  id,
  initial,
  trackView = false,
}: {
  id: string;
  initial?: Interaction;
  trackView?: boolean;
}) {
  const metrics = usePublicationMetrics(id, initial);
  const { user, hasSession } = useAuthSession();
  useEffect(() => {
    if (!trackView || (hasSession && user === undefined)) return;
    // 页面可见且停留五秒后记录一次阅读，切走或卸载时取消定时器。
    let timer: ReturnType<typeof setTimeout> | undefined;
    let recorded = false;
    const schedule = () => {
      clearTimeout(timer);
      if (!recorded && document.visibilityState === "visible")
        timer = setTimeout(() => {
          recorded = true;
          void recordPublicationEvent(id, "view", readerToken());
        }, 5000);
    };
    schedule();
    document.addEventListener("visibilitychange", schedule);
    return () => {
      clearTimeout(timer);
      document.removeEventListener("visibilitychange", schedule);
    };
  }, [id, trackView, user, hasSession]);
  return (
    <div className="publication-metrics" aria-label="文章统计">
      <span>
        <Eye size={14} aria-hidden="true" />
        {metrics?.viewCount?.toLocaleString() ?? "—"} 阅读
      </span>
      {!trackView && (
        <span>
          <Share2 size={13} aria-hidden="true" />
          {metrics?.shareCount?.toLocaleString() ?? "—"} 分享
        </span>
      )}
    </div>
  );
}
