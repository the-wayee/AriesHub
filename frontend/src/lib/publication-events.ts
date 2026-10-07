import { apiRequest } from "./api";
import type { Interaction } from "./publication-reader";

/** 所有计数与当前账号状态都采用服务端返回，组件通过同一事件同步快照。 */
export async function recordPublicationEvent(
  id: string,
  kind: "view" | "share",
  token: string,
) {
  const result = await apiRequest<Interaction>(
    `/api/v1/publications/${id}/${kind}`,
    {
      method: "POST",
      body: JSON.stringify({ token }),
    },
  );
  if (result.ok)
    window.dispatchEvent(
      new CustomEvent("arieshub:publication", { detail: result.data }),
    );
  return result;
}

export function readerToken() {
  const key = "arieshub:reader-token";
  try {
    const existing = localStorage.getItem(key);
    if (existing && /^[a-f0-9-]{36}$/i.test(existing)) return existing;
    const token = crypto.randomUUID();
    localStorage.setItem(key, token);
    return token;
  } catch {
    return crypto.randomUUID();
  }
}
