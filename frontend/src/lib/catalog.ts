import { readApiResponse } from "./api";
import "server-only";
import { cache } from "react";
import type {
  PublicationContent,
  PublicationDetail,
  PublicationPage,
  Category,
} from "./catalog-types";

export type ApiResult<T> =
  { ok: true; data: T } | { ok: false; status: number; traceId?: string };

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
    const result = await readApiResponse<T>(response);
    if (!result.ok) {
      return {
        ok: false,
        status: result.status,
        traceId: result.error.traceId,
      };
    }
    return result;
  } catch {
    return { ok: false, status: 503 };
  }
}

export const getCategories = () => get<Category[]>("/categories");
export const getPublications = (query = "") =>
  get<PublicationPage>(`/publications${query ? `?${query}` : ""}`);
export const getPublication = cache((id: string) =>
  get<PublicationDetail>(`/publications/${encodeURIComponent(id)}`),
);
export const getContent = (id: string) =>
  get<PublicationContent>(`/publications/${encodeURIComponent(id)}/content`);
