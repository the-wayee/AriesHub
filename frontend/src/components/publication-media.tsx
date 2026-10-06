"use client";
import { useEffect, useState } from "react";
import Image from "next/image";
import { FileDown, RotateCw } from "lucide-react";
import { apiRequest } from "@/lib/api";
export function PublicationMedia({
  id,
  label,
  kind = "IMAGE",
  admin = false,
  publicationId,
}: {
  id: string;
  label: string;
  kind?: "IMAGE" | "VIDEO" | "ATTACHMENT";
  admin?: boolean;
  publicationId?: string;
}) {
  const [url, setUrl] = useState("");
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    let active = true;
    if (!admin && !publicationId) return;
    const refresh = () => {
      void apiRequest<{ url: string }>(
        admin
          ? `/api/v1/admin/media/${id}/url`
          : `/api/v1/publications/${publicationId}/media/${id}/url`,
      ).then((r) => {
        if (!active) return;
        if (r.ok) {
          setUrl(r.data.url);
          setError("");
        } else setError(r.error.msg);
      });
    };
    refresh();
    const timer = setInterval(refresh, 240000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [id, admin, publicationId, retry]);
  if (error)
    return (
      <span className="publication-media-error">
        {label} · {error}
        <button
          type="button"
          onClick={() => setRetry(retry + 1)}
          aria-label="重试加载素材"
        >
          <RotateCw size={14} />
        </button>
      </span>
    );
  if (!url)
    return (
      <span className="publication-media-loading">
        正在加载
        {kind === "VIDEO" ? "视频" : kind === "ATTACHMENT" ? "附件" : "图片"}…
      </span>
    );
  if (kind === "ATTACHMENT")
    return (
      <a
        className="publication-attachment"
        href={url}
        target="_blank"
        rel="noopener noreferrer"
      >
        <FileDown size={19} />
        <span>{label}</span>
        <small>下载附件</small>
      </a>
    );
  if (kind === "VIDEO")
    return (
      <video
        className="publication-video"
        src={url}
        controls
        playsInline
        preload="metadata"
        aria-label={label}
        onError={() => setError("视频暂时无法播放")}
      />
    );
  return (
    <Image
      className="publication-image"
      unoptimized
      src={url}
      width={900}
      height={600}
      alt={label}
      onError={() => setError("图片暂时无法加载")}
    />
  );
}
