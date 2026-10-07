"use client";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { apiRequest } from "@/lib/api";
import { useAuthSession } from "./auth-session";
import { Button } from "./ui/button";
import { PublicationProgressBadge } from "./publication-badges";
import type { ReadingProgress } from "@/lib/publication-reader";

/** 标题文本生成稳定锚点；插入新段落不改变已有标题的位置键。 */
function anchor(text: string) {
  let hash = 2166136261;
  for (const c of text) {
    hash ^= c.charCodeAt(0);
    hash = Math.imul(hash, 16777619);
  }
  return `read-${(hash >>> 0).toString(36)}`;
}
export function ReadingProgressTracker({
  id,
  version,
  children,
}: {
  id: string;
  version: string;
  children: ReactNode;
}) {
  const { user } = useAuthSession();
  const userId = user?.id;
  const root = useRef<HTMLDivElement>(null);
  const [previous, setPrevious] = useState<ReadingProgress | null>(null);
  const [status, setStatus] = useState("");
  const restore = useRef<() => void>(() => {});
  useEffect(() => {
    const element = root.current;
    if (!element || !userId) return;
    let active = true,
      ready = false,
      started = false,
      pending = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let lastPayload = "";
    const headings = Array.from(element.querySelectorAll<HTMLElement>("h2,h3"));
    const repeats = new Map<string, number>();
    for (const h of headings) {
      const key = anchor(h.textContent ?? "");
      const nth = repeats.get(key) ?? 0;
      repeats.set(key, nth + 1);
      h.id = `${key}-${nth}`;
      h.style.scrollMarginTop = "112px";
    }
    const measure = () => {
      const box = element.getBoundingClientRect();
      const percent = Math.max(
        0,
        Math.min(
          100,
          Math.round(
            (Math.max(0, 112 - box.top) * 100) /
              Math.max(1, box.height - innerHeight + 112),
          ),
        ),
      );
      return {
        version,
        position:
          headings.filter((h) => h.getBoundingClientRect().top <= 120).at(-1)
            ?.id ?? "",
        percent,
      };
    };
    const save = async () => {
      if (!active || !ready || !started || pending) return;
      const payload = JSON.stringify(measure());
      if (payload === lastPayload) return;
      pending = true;
      const r = await apiRequest<ReadingProgress>(
        `/api/v1/publications/${id}/reading-progress`,
        { method: "PUT", body: payload },
      );
      pending = false;
      if (!active) return;
      if (r.ok) {
        lastPayload = payload;
        setStatus("阅读位置已保存");
      } else {
        setStatus(r.error.msg);
        if (r.error.code === "READING_VERSION_CHANGED") ready = false;
      }
    };
    const schedule = () => {
      if (!ready || !started) return;
      clearTimeout(timer);
      timer = setTimeout(() => void save(), 1500);
    };
    const start = () => {
      started = true;
      schedule();
    };
    const leave = () => {
      if (!ready || !started || pending) return;
      const payload = JSON.stringify(measure());
      if (payload !== lastPayload)
        void fetch(`/api/v1/publications/${id}/reading-progress`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: payload,
          keepalive: true,
        }).catch(() => {});
    };
    void apiRequest<ReadingProgress | null>(
      `/api/v1/publications/${id}/reading-progress`,
      { cache: "no-store" },
    ).then((r) => {
      if (!active) return;
      if (!r.ok) {
        setStatus(r.error.msg);
        return;
      }
      const old = r.data;
      setPrevious(old);
      ready = true;
      started = !old;
      if (old && old.version !== version)
        setStatus("正文已更新，旧阅读位置不再适用；请从新版正文开始。");
      restore.current = () => {
        started = true;
        if (old && old.version === version) {
          const target = headings.find((h) => h.id === old.position);
          if (target) target.scrollIntoView({ behavior: "instant" });
          else {
            const box = element.getBoundingClientRect();
            window.scrollTo({
              top:
                scrollY +
                box.top +
                (Math.max(0, box.height - innerHeight) * old.percent) / 100 -
                112,
              behavior: "instant",
            });
          }
        } else element.scrollIntoView({ behavior: "instant" });
        schedule();
      };
      if (
        old &&
        old.version === version &&
        new URLSearchParams(location.search).get("resume") === "1"
      )
        restore.current();
      if (!old) schedule();
    });
    window.addEventListener("scroll", schedule, { passive: true });
    window.addEventListener("wheel", start, { passive: true });
    window.addEventListener("touchmove", start, { passive: true });
    window.addEventListener("keydown", start);
    window.addEventListener("pagehide", leave);
    return () => {
      leave();
      active = false;
      clearTimeout(timer);
      window.removeEventListener("scroll", schedule);
      window.removeEventListener("wheel", start);
      window.removeEventListener("touchmove", start);
      window.removeEventListener("keydown", start);
      window.removeEventListener("pagehide", leave);
    };
  }, [id, version, userId]);
  return (
    <>
      <div className="reader-resume">
        {user && previous && (
          <Button variant="ghost" onClick={() => restore.current()}>
            {previous.version === version ? (
              <PublicationProgressBadge
                percent={previous.percent}
                label="继续上次位置 ·"
              />
            ) : (
              "从新版开始阅读"
            )}
          </Button>
        )}
        {user && (
          <span className="reader-status" role="status">
            {status || "阅读位置随账号保存"}
          </span>
        )}
      </div>
      <div ref={root}>{children}</div>
    </>
  );
}
