"use client";
import { useEffect, useState } from "react";
import {
  Download,
  ChevronDown,
  FileArchive,
  FileText,
  ImageIcon,
  LockKeyhole,
} from "lucide-react";
import { apiRequest } from "@/lib/api";
import { KIBIBYTE, MEBIBYTE } from "@/lib/file-types";
import type { components } from "@/lib/api-schema";
export type ArticleAttachment = components["schemas"]["PublicationAttachment"];
export function attachmentSize(size: number) {
  return size >= MEBIBYTE
    ? `${(size / MEBIBYTE).toFixed(1)} MB`
    : `${Math.max(1, Math.ceil(size / KIBIBYTE))} KB`;
}
export function AttachmentIcon({ name }: { name: string }) {
  if (/\.(png|jpe?g|webp)$/i.test(name)) return <ImageIcon size={22} />;
  return name.toLowerCase().endsWith(".zip") ? (
    <FileArchive size={22} />
  ) : (
    <FileText size={22} />
  );
}
/** 只在读者点击下载时请求短期签名；锁定附件不向浏览器请求或缓存下载地址。 */
export function PublicationAttachments({
  publicationId,
  shareToken,
}: {
  publicationId: string;
  shareToken?: string;
}) {
  const [items, setItems] = useState<ArticleAttachment[]>();
  const [expanded, setExpanded] = useState(true);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [pending, setPending] = useState<string>();
  useEffect(() => {
    let active = true;
    void apiRequest<ArticleAttachment[]>(
      shareToken
        ? `/api/v1/shares/${shareToken}/attachments`
        : `/api/v1/publications/${publicationId}/attachments`,
    ).then((result) => {
      if (!active) return;
      if (result.ok) {
        setItems(result.data);
        setError("");
      } else setError(result.error.msg);
    });
    return () => {
      active = false;
    };
  }, [publicationId, attempt, shareToken]);
  async function download(item: ArticleAttachment) {
    if (item.locked || pending) return;
    setPending(item.id);
    setError("");
    const result = await apiRequest<{ url: string }>(
      shareToken
        ? `/api/v1/shares/${shareToken}/media/${item.id}/url`
        : `/api/v1/publications/${publicationId}/media/${item.id}/url`,
    );
    setPending(undefined);
    if (!result.ok) {
      setError(result.error.msg);
      return;
    }
    const link = document.createElement("a");
    link.href = result.data.url;
    link.target = "_blank";
    link.rel = "noopener noreferrer";
    link.download = item.filename;
    link.click();
  }
  if (items?.length === 0 && !error) return null;
  return (
    <section
      className="article-resources reader-resources"
      aria-label="随文附件"
    >
      <button
        type="button"
        className="article-resources-toggle"
        aria-expanded={expanded}
        aria-controls={`attachments-${publicationId}`}
        onClick={() => setExpanded(!expanded)}
      >
        <span className="reader-resources-title">随文附件</span>
        <span className="reader-resources-count">
          {items ? String(items.length).padStart(2, "0") : "…"}
        </span>
        <ChevronDown size={17} />
      </button>
      <div
        id={`attachments-${publicationId}`}
        className="article-resources-collapse"
        data-expanded={expanded}
        inert={!expanded}
      >
        <div>
          {items?.map((item) => (
            <div className="article-resource" key={item.id}>
              <AttachmentIcon name={item.filename} />
              <div>
                <strong>{item.filename}</strong>
                <small>
                  {item.filename.split(".").at(-1)?.toUpperCase()} ·{" "}
                  {attachmentSize(item.size)}
                </small>
              </div>
              {item.locked ? (
                <LockKeyhole
                  className="reader-resource-lock"
                  size={15}
                  aria-label="解锁后可下载"
                />
              ) : (
                <button
                  type="button"
                  disabled={item.locked || !!pending}
                  aria-label={`${item.locked ? "需解锁" : "下载"} ${item.filename}`}
                  onClick={() => void download(item)}
                >
                  <Download size={17} />
                  {pending === item.id ? "准备中…" : "下载"}
                </button>
              )}
            </div>
          ))}
          {items?.some((item) => item.locked) && <small>随文章解锁</small>}
          {error && (
            <p role="alert">
              {error}{" "}
              <button type="button" onClick={() => setAttempt(attempt + 1)}>
                重试
              </button>
            </p>
          )}
        </div>
      </div>
    </section>
  );
}
