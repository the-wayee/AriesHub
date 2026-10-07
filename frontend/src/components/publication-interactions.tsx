"use client";
import { useEffect, useState } from "react";
import { Bookmark, Heart } from "lucide-react";
import Link from "next/link";
import { Button } from "./ui/button";
import { apiRequest } from "@/lib/api";
import { useAuthSession } from "./auth-session";
import type { Interaction } from "@/lib/publication-reader";
/** 等待服务端确认后更新，不假装失败的点赞/收藏已经成功。 */
export function PublicationInteractions({
  id,
  initial,
}: {
  id: string;
  initial?: Interaction;
}) {
  const { user, hasSession } = useAuthSession();
  if (hasSession && user === undefined)
    return <span className="reader-status">正在读取互动状态…</span>;
  return (
    <InteractionControls
      key={`${id}:${user?.id ?? "anon"}`}
      id={id}
      initial={initial}
    />
  );
}

function InteractionControls({
  id,
  initial,
}: {
  id: string;
  initial?: Interaction;
}) {
  const { user } = useAuthSession();
  // 列表刷新提供新的服务端快照时直接采用它；账号变化则由外层 key 清空状态。
  const [snapshot, setSnapshot] = useState({ initial, data: initial });
  const value = snapshot.initial === initial ? snapshot.data : initial;
  const setValue = (data: Interaction) => setSnapshot({ initial, data });
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
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
  async function change(kind: "like" | "bookmark") {
    if (!value || pending) return;
    setPending(true);
    setError("");
    const enabled = kind === "like" ? value.liked : value.bookmarked;
    const r = await apiRequest<Interaction>(
      `/api/v1/publications/${id}/${kind}`,
      { method: enabled ? "DELETE" : "PUT" },
    );
    setPending(false);
    if (r.ok) {
      setValue(r.data);
      window.dispatchEvent(
        new CustomEvent("arieshub:publication", { detail: r.data }),
      );
    } else setError(r.error.msg);
  }
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
    <div className="reader-interactions">
      <div className="hub-content-actions" aria-label="文章互动">
        <Button
          variant="ghost"
          aria-label={value?.liked ? "取消文章点赞" : "点赞文章"}
          aria-pressed={value?.liked ?? false}
          disabled={pending || !value}
          onClick={() => void change("like")}
        >
          <Heart fill={value?.liked ? "currentColor" : "none"} />
          <span>{value?.likeCount ?? "—"}</span>
        </Button>
        <Button
          variant="ghost"
          aria-label={value?.bookmarked ? "取消文章收藏" : "收藏文章"}
          aria-pressed={value?.bookmarked ?? false}
          disabled={pending || !value}
          onClick={() => void change("bookmark")}
        >
          <Bookmark fill={value?.bookmarked ? "currentColor" : "none"} />
          <span>{value?.bookmarked ? "已收藏" : "收藏"}</span>
        </Button>
      </div>
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
