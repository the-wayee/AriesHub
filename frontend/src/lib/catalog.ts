import "server-only";
import { cache } from "react";
import type {
  CaseContent,
  CaseDetail,
  CasePage,
  Category,
} from "./catalog-types";

export type ApiResult<T> =
  { ok: true; data: T } | { ok: false; status: number; requestId?: string };

function backendOrigin() {
  const url = new URL(process.env.BACKEND_ORIGIN ?? "http://127.0.0.1:8080");
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.username ||
    url.password ||
    url.pathname !== "/" ||
    url.search ||
    url.hash
  ) {
    throw new Error("Invalid BACKEND_ORIGIN configuration");
  }
  return url.origin;
}

/** 只在服务端读取可信 Java API；禁用缓存，避免下架后仍显示旧内容。 */
async function get<T>(path: string): Promise<ApiResult<T>> {
  const origin = backendOrigin();
  try {
    const response = await fetch(`${origin}/api/v1${path}`, {
      cache: "no-store",
      signal: AbortSignal.timeout(6000),
      headers: { Accept: "application/json" },
    });
    if (!response.ok) {
      return {
        ok: false,
        status: response.status,
        requestId: response.headers.get("X-Request-Id") ?? undefined,
      };
    }
    return { ok: true, data: (await response.json()) as T };
  } catch {
    return { ok: false, status: 503 };
  }
}

export const getCategories = () => get<Category[]>("/categories");
export const getCases = (query = "") =>
  get<CasePage>(`/cases${query ? `?${query}` : ""}`);
export const getCase = cache((slug: string) =>
  get<CaseDetail>(`/cases/${encodeURIComponent(slug)}`),
);
export const getContent = (id: string) =>
  get<CaseContent>(`/cases/${encodeURIComponent(id)}/content`);
