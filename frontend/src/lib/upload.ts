import { readApiResponse, type ApiResponse } from "./api";

export interface UploadProgress {
  loaded: number;
  total: number | null;
  percentage: number | null;
  phase: "uploading" | "processing" | "oss" | "finalizing";
}

interface StorageProgress {
  phase: string;
  loaded: number;
  total: number;
}

/** 浏览器传输与 OSS 确认字节分开显示，后者通过当前用户隔离的任务状态读取。 */
export function uploadRequest<T>(
  path: string,
  body: FormData,
  onProgress: (progress: UploadProgress) => void,
  signal?: AbortSignal,
): Promise<ApiResponse<T>> {
  return new Promise((resolve) => {
    const xhr = new XMLHttpRequest();
    const uploadId = crypto.randomUUID();
    const statusPath = `/api/v1/admin/media/uploads/${uploadId}`;
    let done = false;
    let pollTimer: ReturnType<typeof setTimeout> | undefined;
    const pollController = new AbortController();
    let progress: UploadProgress = {
      loaded: 0,
      total: null,
      percentage: null,
      phase: "uploading",
    };
    const abort = () => xhr.abort();
    const finish = (result: ApiResponse<T>) => {
      done = true;
      clearTimeout(pollTimer);
      pollController.abort();
      signal?.removeEventListener("abort", abort);
      resolve(result);
    };
    const poll = async () => {
      try {
        const response = await fetch(statusPath, {
          cache: "no-store",
          signal: pollController.signal,
        });
        const result = await readApiResponse<StorageProgress>(response);
        if (!done && result.ok) {
          const { phase, loaded, total } = result.data;
          if (
            ["OSS", "FINALIZING", "COMPLETED"].includes(phase) &&
            Number.isFinite(loaded) &&
            Number.isFinite(total) &&
            total > 0
          ) {
            onProgress({
              phase: phase === "OSS" ? "oss" : "finalizing",
              loaded,
              total,
              percentage: Math.min(100, Math.floor((loaded * 100) / total)),
            });
          }
        }
      } catch {
        // 状态读取失败不伪造百分比，也不把它误当成上传失败；上传 Result 才决定最终成功。
      } finally {
        if (!done) pollTimer = setTimeout(poll, 500);
      }
    };
    xhr.open("POST", `${path}?uploadId=${uploadId}`);
    xhr.setRequestHeader("Accept", "application/json");
    // 不手动设置 Content-Type，浏览器负责 multipart boundary；沿用同源 HttpOnly 会话。
    xhr.upload.onprogress = (event) => {
      progress = {
        loaded: event.loaded,
        total: event.lengthComputable ? event.total : null,
        percentage:
          event.lengthComputable && event.total > 0
            ? Math.min(100, Math.floor((event.loaded * 100) / event.total))
            : null,
        phase: "uploading",
      };
      onProgress(progress);
    };
    xhr.upload.onload = () => {
      onProgress({
        loaded: 0,
        total: null,
        percentage: null,
        phase: "processing",
      });
      void poll();
    };
    xhr.onload = async () => {
      const headers = new Headers();
      for (const name of ["X-Trace-Id", "Retry-After"]) {
        const value = xhr.getResponseHeader(name);
        if (value) headers.set(name, value);
      }
      finish(
        await readApiResponse<T>(
          new Response(xhr.responseText || null, {
            status: xhr.status,
            headers,
          }),
        ),
      );
    };
    xhr.onerror = () =>
      finish({
        ok: false,
        status: 503,
        error: {
          code: "NETWORK_ERROR",
          msg: "上传连接中断，请重新选择文件重试",
        },
      });
    xhr.onabort = () => {
      // 服务端会在下一次分片确认时停止并清理 multipart，不只取消浏览器等待。
      void fetch(statusPath, { method: "DELETE", keepalive: true }).catch(
        () => {},
      );
      finish({
        ok: false,
        status: 0,
        error: { code: "UPLOAD_CANCELLED", msg: "已取消上传" },
      });
    };
    signal?.addEventListener("abort", abort, { once: true });
    if (signal?.aborted) {
      finish({
        ok: false,
        status: 0,
        error: { code: "UPLOAD_CANCELLED", msg: "已取消上传" },
      });
      return;
    }
    xhr.send(body);
  });
}
