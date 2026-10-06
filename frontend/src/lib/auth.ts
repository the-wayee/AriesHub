export interface CurrentUser {
  id: string;
  email: string;
  nickname: string;
  bio?: string;
  avatarFileId?: string | null;
  role: "USER" | "ADMIN";
  emailVerified: boolean;
  createdAt: string;
}

export interface ApiError {
  code: string;
  message: string;
  requestId?: string;
  retryAfterSeconds?: number;
}

export async function authRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<{ ok: true; data: T } | { ok: false; error: ApiError }> {
  return apiRequest<T>(`/api/v1/auth${path}`, init);
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<{ ok: true; data: T } | { ok: false; error: ApiError }> {
  try {
    const response = await fetch(path, {
      ...init,
      credentials: "same-origin",
      headers: {
        Accept: "application/json",
        ...(init.body ? { "Content-Type": "application/json" } : {}),
        ...init.headers,
      },
    });
    if (response.ok) {
      const data = response.status === 204 ? undefined : await response.json();
      return { ok: true, data: data as T };
    }
    const error = (await response.json().catch(() => ({
      code: "REQUEST_FAILED",
      message: "请求没有成功，请稍后再试",
    }))) as ApiError;
    const retryHeader = response.headers.get("Retry-After");
    const retrySeconds = retryHeader
      ? /^\d+$/.test(retryHeader)
        ? Number(retryHeader)
        : Math.ceil((Date.parse(retryHeader) - Date.now()) / 1000)
      : error.retryAfterSeconds;
    if (
      typeof retrySeconds === "number" &&
      Number.isFinite(retrySeconds) &&
      retrySeconds > 0
    ) {
      error.retryAfterSeconds = Math.ceil(retrySeconds);
    }
    return { ok: false, error };
  } catch {
    return {
      ok: false,
      error: { code: "NETWORK_ERROR", message: "暂时无法连接服务，请稍后再试" },
    };
  }
}
