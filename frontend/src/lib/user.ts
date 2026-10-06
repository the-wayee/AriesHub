import { apiRequest, type ApiError } from "./auth";

export function userRequest<T>(path: string, init: RequestInit = {}) {
  return apiRequest<T>(`/api/v1/users${path}`, init);
}

export async function uploadAvatar(
  file: File,
): Promise<{ ok: true; id: string } | { ok: false; error: ApiError }> {
  const body = new FormData();
  body.set("file", file);
  try {
    const response = await fetch("/api/v1/users/me/avatar", {
      method: "POST",
      credentials: "same-origin",
      body,
    });
    if (response.ok) return { ok: true, id: (await response.json()).id };
    return {
      ok: false,
      error: await response.json().catch(() => ({
        code: "UPLOAD_FAILED",
        message: "头像上传失败，请稍后重试",
      })),
    };
  } catch {
    return {
      ok: false,
      error: {
        code: "NETWORK_ERROR",
        message: "暂时无法上传头像，请检查网络后重试",
      },
    };
  }
}
