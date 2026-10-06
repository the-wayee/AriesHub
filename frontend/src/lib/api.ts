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
        code: "INVALID_RESPONSE",
        msg: "服务返回格式不正确，请稍后再试",
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

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<ApiResponse<T>> {
  try {
    const response = await fetch(path, {
      ...init,
      credentials: "same-origin",
      headers: {
        Accept: "application/json",
        ...(init.body && !(init.body instanceof FormData)
          ? { "Content-Type": "application/json" }
          : {}),
        ...init.headers,
      },
    });
    return await readApiResponse<T>(response);
  } catch {
    return {
      ok: false,
      status: 503,
      error: { code: "NETWORK_ERROR", msg: "暂时无法连接服务，请稍后再试" },
    };
  }
}
