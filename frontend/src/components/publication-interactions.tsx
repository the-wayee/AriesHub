"use client";
import { useEffect, useRef, useState } from "react";
import { Bookmark, Heart, LoaderCircle } from "lucide-react";
import gsap from "gsap";
import { useGSAP } from "@gsap/react";
import Link from "next/link";
import { Button } from "./ui/button";
import { apiRequest } from "@/lib/api";
import { useAuthSession } from "./auth-session";
import type { Interaction } from "@/lib/publication-reader";
gsap.registerPlugin(useGSAP);
type InteractionKind = "like" | "bookmark";
const BURST_PARTICLES = Array.from({ length: 8 }, (_, index) => index);
/** 等待服务端确认后更新，不假装失败的点赞/收藏已经成功。 */
export function PublicationInteractions({
  id,
  initial,
  prominent = false,
}: {
  id: string;
  initial?: Interaction;
  prominent?: boolean;
}) {
  const { user, hasSession } = useAuthSession();
  if (hasSession && user === undefined)
    return <span className="reader-status">正在读取互动状态…</span>;
  return (
    <InteractionControls
      key={`${id}:${user?.id ?? "anon"}`}
      id={id}
      initial={initial}
      prominent={prominent}
    />
  );
}

function InteractionControls({
  id,
  initial,
  prominent = false,
}: {
  id: string;
  initial?: Interaction;
  prominent?: boolean;
}) {
  const { user } = useAuthSession();
  // 列表刷新提供新的服务端快照时直接采用它；账号变化则由外层 key 清空状态。
  const [snapshot, setSnapshot] = useState({ initial, data: initial });
  const value = snapshot.initial === initial ? snapshot.data : initial;
  const setValue = (data: Interaction) => setSnapshot({ initial, data });
  const root = useRef<HTMLDivElement>(null);
  const [pending, setPending] = useState<InteractionKind | null>(null);
  const [feedback, setFeedback] = useState<{
    kind: InteractionKind;
    enabled: boolean;
    sequence: number;
  } | null>(null);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  useGSAP(
    () => {
      if (!feedback || !root.current) return;
      // 仅主动操作成功后播放；服务端同步、首屏加载不会触发庆祝动效。
      const media = gsap.matchMedia();
      media.add("(prefers-reduced-motion: no-preference)", () => {
        const button = root.current?.querySelector(
          `[data-interaction="${feedback.kind}"]`,
        );
        if (!button) return;
        const icon = button.querySelector(".interaction-symbol");
        const label = button.querySelector(".interaction-label");
        const timeline = gsap.timeline();
        timeline
          .fromTo(
            icon,
            {
              scale: feedback.enabled ? 0.65 : 1.15,
              rotation: feedback.kind === "bookmark" ? -12 : 0,
            },
            {
              scale: feedback.enabled ? 1.3 : 0.85,
              rotation: 0,
              duration: 0.18,
              ease: "power2.out",
            },
          )
          .to(icon, { scale: 1, duration: 0.4, ease: "elastic.out(1, 0.55)" })
          .fromTo(
            label,
            { y: 5, opacity: 0.45 },
            { y: 0, opacity: 1, duration: 0.25 },
            0,
          );
        if (feedback.enabled) {
          button
            .querySelectorAll(".interaction-particle")
            .forEach((particle, index) => {
              const angle = (index / BURST_PARTICLES.length) * Math.PI * 2;
              timeline
                .fromTo(
                  particle,
                  { x: 0, y: 0, opacity: 0, scale: 0.3 },
                  {
                    x: Math.cos(angle) * 23,
                    y: Math.sin(angle) * 23,
                    opacity: 0.8,
                    scale: 1,
                    duration: 0.22,
                    ease: "power2.out",
                  },
                  0,
                )
                .to(particle, { opacity: 0, scale: 0.3, duration: 0.28 }, 0.22);
            });
          timeline.fromTo(
            button.querySelector(".interaction-ring"),
            { scale: 0.45, opacity: 0.55 },
            { scale: 1.9, opacity: 0, duration: 0.5, ease: "power2.out" },
            0,
          );
        }
      });
      return () => media.revert();
    },
    { scope: root, dependencies: [feedback], revertOnUpdate: true },
  );
  useEffect(() => {
    let active = true;
    const refresh = () => {
      void apiRequest<Interaction>(`/api/v1/publications/${id}/interaction`, {
        cache: "no-store",
      }).then((r) => {
        if (!active) return;
        if (r.ok) setSnapshot({ initial, data: r.data });
        else setError(r.error.msg);
      });
    };
    if (!initial) refresh();
    const sync = (e: Event) => {
      const data = (e as CustomEvent<Interaction>).detail;
      if (data?.publicationId === id) setSnapshot({ initial, data });
    };
    window.addEventListener("arieshub:publication", sync);
    return () => {
      active = false;
      window.removeEventListener("arieshub:publication", sync);
    };
  }, [id, initial, revision]);
  async function change(kind: InteractionKind) {
    if (!value || pending) return;
    setPending(kind);
    setError("");
    setFeedback(null);
    const enabled = kind === "like" ? value.liked : value.bookmarked;
    const r = await apiRequest<Interaction>(
      `/api/v1/publications/${id}/${kind}`,
      { method: enabled ? "DELETE" : "PUT" },
    );
    setPending(null);
    if (r.ok) {
      setValue(r.data);
      setFeedback((previous) => ({
        kind,
        enabled: kind === "like" ? r.data.liked : r.data.bookmarked,
        sequence: (previous?.sequence ?? 0) + 1,
      }));
      window.dispatchEvent(
        new CustomEvent("arieshub:publication", { detail: r.data }),
      );
    } else setError(r.error.msg);
  }
  if (!user && prominent)
    return (
      <div className="reader-interactions">
        <div className="hub-content-actions" aria-label="文章互动">
          <Link
            className="interaction-button"
            href={`/login?next=${encodeURIComponent(`/publications/${id}`)}`}
          >
            <Heart aria-hidden="true" />
            点赞{" "}
            <span className="publication-action-number">
              {value?.likeCount?.toLocaleString() ?? "—"}
            </span>
          </Link>
          <Link
            className="interaction-button"
            href={`/login?next=${encodeURIComponent(`/publications/${id}`)}`}
          >
            <Bookmark aria-hidden="true" />
            收藏{" "}
            <span className="publication-action-number">
              {value?.bookmarkCount?.toLocaleString() ?? "—"}
            </span>
          </Link>
        </div>
      </div>
    );
  if (!user)
    return (
      <Link
        className="hub-inline-link"
        href={`/login?next=${encodeURIComponent(`/publications/${id}`)}`}
      >
        登录后收藏与点赞
      </Link>
    );
  return (
    <div ref={root} className="reader-interactions">
      <div className="hub-content-actions" aria-label="文章互动">
        <Button
          variant="ghost"
          className="interaction-button"
          data-interaction="like"
          aria-busy={pending === "like"}
          aria-label={value?.liked ? "取消文章点赞" : "点赞文章"}
          aria-pressed={value?.liked ?? false}
          disabled={!!pending || !value}
          onClick={() => void change("like")}
        >
          <span className="interaction-icon" aria-hidden="true">
            <span className="interaction-ring" />
            {BURST_PARTICLES.map((index) => (
              <i key={index} className="interaction-particle" />
            ))}
            <Heart
              className="interaction-symbol"
              fill={value?.liked ? "currentColor" : "none"}
            />
          </span>
          <span className="interaction-label">
            {prominent && <span>{value?.liked ? "已点赞" : "点赞"}</span>}
            <span
              className={prominent ? "publication-action-number" : undefined}
            >
              {value?.likeCount?.toLocaleString() ?? "—"}
            </span>
          </span>
          {pending === "like" && (
            <LoaderCircle className="interaction-spinner" aria-hidden="true" />
          )}
        </Button>
        <Button
          variant="ghost"
          className="interaction-button"
          data-interaction="bookmark"
          aria-busy={pending === "bookmark"}
          aria-label={value?.bookmarked ? "取消文章收藏" : "收藏文章"}
          aria-pressed={value?.bookmarked ?? false}
          disabled={!!pending || !value}
          onClick={() => void change("bookmark")}
        >
          <span className="interaction-icon" aria-hidden="true">
            <span className="interaction-ring" />
            {BURST_PARTICLES.map((index) => (
              <i key={index} className="interaction-particle" />
            ))}
            <Bookmark
              className="interaction-symbol"
              fill={value?.bookmarked ? "currentColor" : "none"}
            />
          </span>
          <span className="interaction-label">
            {value?.bookmarked ? "已收藏" : "收藏"}
            <span
              className={
                prominent
                  ? "publication-action-number"
                  : "interaction-small-count"
              }
            >
              {value?.bookmarkCount?.toLocaleString() ?? "—"}
            </span>
          </span>
          {pending === "bookmark" && (
            <LoaderCircle className="interaction-spinner" aria-hidden="true" />
          )}
        </Button>
      </div>
      <span className="sr-only" role="status">
        {pending
          ? "正在保存…"
          : feedback
            ? `${feedback.enabled ? "已" : "已取消"}${feedback.kind === "like" ? "点赞" : "收藏"}`
            : ""}
      </span>
      {error && (
        <p role="alert" className="reader-error">
          {error}
          {!value && (
            <Button
              variant="ghost"
              onClick={() => {
                setError("");
                setRevision(revision + 1);
              }}
            >
              重试
            </Button>
          )}
        </p>
      )}
    </div>
  );
}
