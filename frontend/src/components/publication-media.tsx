"use client";
import { useEffect, useState } from "react";
import Image from "next/image";
import { Dialog } from "@base-ui/react/dialog";
import { FileDown, RotateCw, ZoomIn, X } from "lucide-react";
import { apiRequest } from "@/lib/api";
const MEDIA_URL_REFRESH_INTERVAL_MS = 4 * 60 * 1000;

export function PublicationMedia({
  id,
  label,
  kind = "IMAGE",
  admin = false,
  publicationId,
  signedUrl,
  preview = false,
}: {
  id: string;
  label: string;
  kind?: "IMAGE" | "VIDEO" | "ATTACHMENT";
  admin?: boolean;
  publicationId?: string;
  signedUrl?: string;
  preview?: boolean;
}) {
  const [url, setUrl] = useState("");
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    let active = true;
    if (!admin && !publicationId) return;
    // 列表已携带签名地址时直接展示；仅用户明确重试才重新请求单素材地址。
    if (signedUrl && retry === 0) return;
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
    // 图片下载完成后不再依赖签名有效期，定时换 URL 只会让列表持续请求并重载封面。
    // 视频后续分段请求与附件下载仍需有效签名，暂保留其原有续期行为。
    const timer =
      kind === "IMAGE"
        ? undefined
        : setInterval(refresh, MEDIA_URL_REFRESH_INTERVAL_MS);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [id, admin, publicationId, retry, kind, signedUrl]);
  const resolvedUrl = signedUrl && retry === 0 ? signedUrl : url;
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
  if (!resolvedUrl)
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
        href={resolvedUrl}
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
        src={resolvedUrl}
        controls
        playsInline
        preload="metadata"
        aria-label={label}
        onError={() => setError("视频暂时无法播放")}
      />
    );
  const picture = (
    <Image
      className="publication-image"
      unoptimized
      src={resolvedUrl}
      width={900}
      height={600}
      alt={label}
      onError={() => setError("图片暂时无法加载")}
    />
  );
  if (!preview) return picture;
  return (
    <Dialog.Root>
      <Dialog.Trigger
        className="publication-cover-trigger"
        aria-label={`放大查看${label}`}
      >
        {picture}
        <ZoomIn size={14} aria-hidden="true" />
      </Dialog.Trigger>
      <Dialog.Portal>
        <Dialog.Backdrop className="publication-cover-backdrop" />
        <Dialog.Popup className="publication-cover-dialog">
          <header>
            <Dialog.Title>{label}</Dialog.Title>
            <Dialog.Close aria-label="关闭封面预览">
              <X size={22} />
            </Dialog.Close>
          </header>
          <Dialog.Description className="sr-only">
            封面大图预览，按 Escape 或点击遮罩关闭。
          </Dialog.Description>
          <Image
            unoptimized
            src={resolvedUrl}
            width={1600}
            height={1200}
            alt={label}
            className="publication-cover-full"
          />
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
