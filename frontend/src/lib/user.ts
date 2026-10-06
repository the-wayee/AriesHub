import { apiRequest } from "./api";

export function userRequest<T>(path: string, init: RequestInit = {}) {
  return apiRequest<T>(`/api/v1/users${path}`, init);
}

export async function uploadAvatar(file: File) {
  const body = new FormData();
  body.set("file", file);
  const result = await userRequest<{ id: string }>("/me/avatar", {
    method: "POST",
    body,
  });
  return result.ok ? { ok: true as const, id: result.data.id } : result;
}
