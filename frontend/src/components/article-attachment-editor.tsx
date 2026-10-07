"use client";
import { useEffect, useRef, useState } from "react";
import { Paperclip, Plus, X, Check, Clock3, CircleAlert } from "lucide-react";
import {
  PUBLICATION_MEDIA,
  IMAGE_MIME_TYPES,
  ARTICLE_ATTACHMENT_ACCEPT,
  ARTICLE_ATTACHMENT_LIMIT,
} from "@/lib/file-types";
import { uploadRequest, type UploadProgress as Progress } from "@/lib/upload";
import { UploadProgress } from "./upload-progress";
import {
  AttachmentIcon,
  attachmentSize,
  type ArticleAttachment,
} from "./publication-attachments";
type QueueItem = {
  id: string;
  dismissed?: boolean;
  file: File;
  state: "waiting" | "uploading" | "failed" | "done";
  error?: string;
};
export function ArticleAttachmentEditor({
  items,
  onChange,
  paid,
  disabled,
  onUploading,
}: {
  items: ArticleAttachment[];
  onChange: (items: ArticleAttachment[]) => void;
  paid: boolean;
  disabled: boolean;
  onUploading: (value: boolean) => void;
}) {
  const input = useRef<HTMLInputElement>(null);
  const controller = useRef<AbortController | null>(null);
  const mounted = useRef(true);
  const running = useRef(false);
  const [queue, setQueue] = useState<QueueItem[]>([]);
  const [progress, setProgress] = useState<Progress>();
  const [error, setError] = useState("");
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      controller.current?.abort();
    };
  }, []);
  async function upload(files: File[]) {
    if (running.current || disabled || !files.length) return;
    if (files.length + items.length > ARTICLE_ATTACHMENT_LIMIT) {
      setError(
        `每篇最多 ${ARTICLE_ATTACHMENT_LIMIT} 个附件，还可以添加 ${ARTICLE_ATTACHMENT_LIMIT - items.length} 个。`,
      );
      return;
    }
    const pending: QueueItem[] = files.map((file) => ({
      id: crypto.randomUUID(),
      file,
      state: "waiting",
    }));
    // 串行队列避免大文件同时占满连接；每次成功立即合并，取消不丢失已完成的文件。
    running.current = true;
    onUploading(true);
    setError("");
    setQueue([...pending]);
    const abort = new AbortController();
    controller.current = abort;
    let completed = [...items];
    try {
      for (const entry of pending) {
        if (abort.signal.aborted || !mounted.current) break;
        const isImage = IMAGE_MIME_TYPES.includes(entry.file.type);
        const extension = `.${entry.file.name.split(".").at(-1)?.toLowerCase()}`;
        const kind = isImage ? "IMAGE" : "ATTACHMENT";
        const valid =
          isImage ||
          PUBLICATION_MEDIA.ATTACHMENT.accept.split(",").includes(extension);
        if (
          !valid ||
          entry.file.size <= 0 ||
          entry.file.size > PUBLICATION_MEDIA[kind].maxBytes
        ) {
          entry.state = "failed";
          entry.error = !valid
            ? "不支持此文件格式，请选择图片、压缩包或文档。"
            : entry.file.size === 0
              ? "文件为空，请重新选择。"
              : isImage
                ? "图片超过 10 MiB，请压缩后重新上传。"
                : "文件超过 20 MiB，请压缩后重新上传。";
          setQueue([...pending]);
          continue;
        }
        entry.state = "uploading";
        setQueue([...pending]);
        setProgress({
          loaded: 0,
          total: null,
          percentage: null,
          phase: "uploading",
        });
        const body = new FormData();
        body.set("kind", kind);
        body.set("file", entry.file);
        const result = await uploadRequest<ArticleAttachment>(
          "/api/v1/admin/media",
          body,
          setProgress,
          abort.signal,
        );
        if (!mounted.current || abort.signal.aborted) break;
        if (result.ok) {
          completed = [...completed, { ...result.data, locked: paid }];
          onChange(completed);
          entry.state = "done";
          setQueue([...pending]);
        } else {
          entry.state = "failed";
          entry.error = result.error.msg;
        }
        setProgress(undefined);
      }
    } finally {
      running.current = false;
      if (mounted.current) {
        setQueue(
          pending.filter((item) => item.state === "failed" && !item.dismissed),
        );
        setProgress(undefined);
        onUploading(false);
        if (abort.signal.aborted)
          setError("已取消剩余上传，已完成的附件已保留。");
      }
    }
  }
  return (
    <section
      className="article-resources writer-resources"
      aria-label="文章附件"
    >
      <header>
        <h2>
          <Paperclip size={18} />
          文章附件
        </h2>
        <span>
          {items.length} / {ARTICLE_ATTACHMENT_LIMIT}
        </span>
      </header>
      <p>
        {paid
          ? "附件随文章解锁，试读用户只能看到文件信息。"
          : "免费文章的附件可免费下载。"}
        支持多选图片、压缩包、提示词文本；图片最大 10 MiB，文件最大 20 MiB。
      </p>
      <input
        ref={input}
        type="file"
        hidden
        multiple
        accept={ARTICLE_ATTACHMENT_ACCEPT}
        aria-label="上传文章附件"
        onChange={(event) => {
          const files = Array.from(event.target.files ?? []);
          event.target.value = "";
          void upload(files);
        }}
      />
      <div className="attachment-card-grid">
        {items.map((item) => (
          <div className="attachment-file-card" key={item.id}>
            <AttachmentIcon name={item.filename} />
            <strong title={item.filename}>{item.filename}</strong>
            <small>
              {attachmentSize(item.size)} · {paid ? "解锁后下载" : "免费下载"}
            </small>
            <span className="attachment-complete">
              <Check size={12} />
              已上传
            </span>
            <button
              type="button"
              className="attachment-remove"
              disabled={disabled}
              aria-label={`移除附件 ${item.filename}`}
              onClick={() =>
                onChange(items.filter((other) => other.id !== item.id))
              }
            >
              <X size={15} />
            </button>
          </div>
        ))}
        {queue
          .filter((entry) => entry.state !== "done" && !entry.dismissed)
          .map((entry) => (
            <div
              className={`attachment-file-card ${entry.state === "failed" ? "attachment-failed" : "attachment-queued"}`}
              key={entry.id}
            >
              {entry.state === "failed" ? (
                <CircleAlert size={22} />
              ) : (
                <Clock3 size={22} />
              )}
              <strong>{entry.file.name}</strong>
              <small role={entry.state === "failed" ? "alert" : undefined}>
                {entry.state === "failed"
                  ? entry.error
                  : entry.state === "uploading"
                    ? "正在上传"
                    : "等待上传"}
              </small>
              {entry.state === "failed" && (
                <button
                  type="button"
                  className="attachment-remove"
                  aria-label={`移除失败附件 ${entry.file.name}`}
                  onClick={() => {
                    // 同步标记队列对象，后续文件完成时重新发布队列也不会复活已移除的失败卡片。
                    entry.dismissed = true;
                    setQueue((current) =>
                      current.filter((item) => item.id !== entry.id),
                    );
                  }}
                >
                  <X size={15} />
                </button>
              )}
            </div>
          ))}
        <button
          type="button"
          className="attachment-add-card"
          aria-label="上传附件"
          disabled={disabled || items.length >= ARTICLE_ATTACHMENT_LIMIT}
          onClick={() => input.current?.click()}
        >
          <Plus size={28} />
          <strong>添加附件</strong>
          <small>图片 / 文件 · 支持多选</small>
        </button>
      </div>
      {progress && (
        <UploadProgress
          filename={
            queue.find((item) => item.state === "uploading")?.file.name ??
            "附件"
          }
          progress={progress}
          onCancel={() => controller.current?.abort()}
        />
      )}
      {error && <p role="alert">{error}</p>}
    </section>
  );
}
