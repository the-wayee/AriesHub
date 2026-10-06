import type { UploadProgress as Progress } from "@/lib/upload";
import { KIBIBYTE, MEBIBYTE } from "@/lib/file-types";

function formatBytes(bytes: number) {
  if (bytes < KIBIBYTE) return `${bytes} B`;
  if (bytes < MEBIBYTE) return `${(bytes / KIBIBYTE).toFixed(1)} KiB`;
  return `${(bytes / MEBIBYTE).toFixed(1)} MiB`;
}

/** 不用定时器伪造进度；未知长度时保留不确定状态。 */
export function UploadProgress({
  filename,
  progress,
  onCancel,
}: {
  filename: string;
  progress: Progress;
  onCancel: () => void;
}) {
  return (
    <div
      className="writer-upload-progress"
      role="group"
      aria-label={`${filename}上传状态`}
    >
      <div className="writer-upload-heading">
        <strong>{filename}</strong>
        <button type="button" onClick={onCancel}>
          取消上传
        </button>
      </div>
      <progress
        aria-label="文件传输进度"
        max={100}
        value={progress.percentage ?? undefined}
      />
      <div className="writer-upload-meta">
        <span>
          {progress.phase === "processing"
            ? "文件已传到服务器，等待 OSS 上传…"
            : progress.phase === "finalizing"
              ? "OSS 已接收全部字节，正在完成存储…"
              : progress.phase === "oss"
                ? `正在上传到 OSS ${progress.percentage}%`
                : progress.percentage === null
                  ? "正在传到服务器…"
                  : `正在传到服务器 ${progress.percentage}%`}
        </span>
        <span>
          {formatBytes(progress.loaded)}
          {progress.total !== null ? ` / ${formatBytes(progress.total)}` : ""}
        </span>
      </div>
    </div>
  );
}
