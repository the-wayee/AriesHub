export interface CurrentUser {
  id: string;
  email: string;
  nickname: string;
  role: "USER" | "ADMIN";
  emailVerified: boolean;
  createdAt: string;
}

export interface ApiError {
  code: string;
  message: string;
  requestId?: string;
}

export async function authRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<{ ok: true; data: T } | { ok: false; error: ApiError }> {
  try {
    const response = await fetch(`/api/v1/auth${path}`, {
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
    return { ok: false, error };
  } catch {
    return {
      ok: false,
      error: { code: "NETWORK_ERROR", message: "暂时无法连接服务，请稍后再试" },
    };
  }
}
