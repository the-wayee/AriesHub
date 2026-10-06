export interface Result<T> {
  code: string;
  msg: string;
  data: T;
  traceId: string;
}

export interface ApiError {
  code: string;
  msg: string;
  traceId?: string;
  retryAfterSeconds?: number;
}

export type ApiResponse<T> =
  { ok: true; data: T } | { ok: false; status: number; error: ApiError };

/** 只接受统一返回契约；HTTP 状态和业务码都成功时才交付 data。 */
export async function readApiResponse<T>(
  response: Response,
): Promise<ApiResponse<T>> {
  const body: unknown = await response.json().catch(() => null);
  if (
    !body ||
    typeof body !== "object" ||
    !("code" in body) ||
    !("msg" in body) ||
    !("data" in body) ||
    !("traceId" in body) ||
    typeof body.code !== "string" ||
    typeof body.msg !== "string" ||
    typeof body.traceId !== "string"
  ) {
    return {
      ok: false,
      status: response.ok ? 502 : response.status,
      error: {
        code: response.status >= 500 ? "UPSTREAM_ERROR" : "INVALID_RESPONSE",
        msg:
          response.status >= 500
            ? "服务端或转发连接异常，请稍后重试"
            : "服务返回格式不正确，请稍后再试",
        traceId: response.headers.get("X-Trace-Id") ?? undefined,
      },
    };
  }
  const result = body as Result<T>;
  if (response.ok && result.code === "SUCCESS")
    return { ok: true, data: result.data };
  const error: ApiError = {
    code: result.code,
    msg: result.msg,
    traceId: result.traceId,
  };
  const retryHeader = response.headers.get("Retry-After");
  const retryData =
    result.data &&
    typeof result.data === "object" &&
    "retryAfterSeconds" in result.data
      ? result.data.retryAfterSeconds
      : undefined;
  const seconds = retryHeader
    ? /^\d+$/.test(retryHeader)
      ? Number(retryHeader)
      : Math.ceil((Date.parse(retryHeader) - Date.now()) / 1000)
    : retryData;
  if (typeof seconds === "number" && Number.isFinite(seconds) && seconds > 0)
    error.retryAfterSeconds = Math.ceil(seconds);
  return { ok: false, status: response.status, error };
}

// 只共享浏览器中尚未完成的读取，避免开发模式 Effect 重放和多个组件重复读取。
// 不缓存已完成的响应，也不在服务端跨用户共享带会话的请求。
const pendingReads = new Map<string, Promise<ApiResponse<unknown>>>();
const shareableOptions = new Set(["method", "headers", "cache", "credentials"]);

export function apiRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<ApiResponse<T>> {
  const headers = new Headers(init.headers);
  if (!headers.has("Accept")) headers.set("Accept", "application/json");
  if (
    init.body &&
    !(init.body instanceof FormData) &&
    !headers.has("Content-Type")
  )
    headers.set("Content-Type", "application/json");
  const method = (init.method ?? "GET").toUpperCase();
  const options: RequestInit = {
    ...init,
    method,
    credentials: "same-origin",
    headers,
  };
  const browser = typeof window !== "undefined";
  const mutation = !["GET", "HEAD"].includes(method);

  // 写操作开始和结束均失效在途读取，后续刷新不能复用写入前的旧结果。
  if (browser && mutation) pendingReads.clear();
  // 带独立取消信号或特殊 Fetch 选项的请求不共享，保留调用方的控制语义。
  const key =
    browser &&
    method === "GET" &&
    Object.keys(init).every((name) => shareableOptions.has(name))
      ? JSON.stringify([path, init.cache ?? "default", [...headers.entries()]])
      : null;
  if (key) {
    const pending = pendingReads.get(key);
    if (pending) return pending as Promise<ApiResponse<T>>;
  }
  const request = sendRequest<T>(path, options).finally(() => {
    if (browser && mutation) pendingReads.clear();
    // 写操作失效后可能已有同 URL 的新请求，旧请求不能删除新请求的记录。
    if (key && pendingReads.get(key) === request) pendingReads.delete(key);
  });
  if (key) pendingReads.set(key, request);
  return request;
}

async function sendRequest<T>(
  path: string,
  init: RequestInit,
): Promise<ApiResponse<T>> {
  try {
    const response = await fetch(path, init);
    return await readApiResponse<T>(response);
  } catch {
    return {
      ok: false,
      status: 503,
      error: { code: "NETWORK_ERROR", msg: "暂时无法连接服务，请稍后再试" },
    };
  }
}
