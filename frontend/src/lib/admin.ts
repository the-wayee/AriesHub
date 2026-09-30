import type { ApiError } from "@/lib/auth";

export interface AdminCategory {
  id: string;
  slug: string;
  name: string;
}

export interface AdminPublicationSummary {
  id: string;
  slug: string;
  title: string;
  categoryName: string;
  publicationType: "CASE_STUDY" | "ARTICLE" | "COURSE";
  accessType: "FREE" | "CREDIT";
  creditPrice: number;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  deliveryStatus: "AVAILABLE" | "SUSPENDED";
  publishedAt: string | null;
  updatedAt: string;
}

export interface AdminPublicationDetail {
  id: string;
  categoryId: string;
  slug: string;
  title: string;
  summary: string;
  publicationType: "CASE_STUDY" | "ARTICLE" | "COURSE";
  accessType: "FREE" | "CREDIT";
  creditPrice: number;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  deliveryStatus: "AVAILABLE" | "SUSPENDED";
  publishedAt: string | null;
  previewMarkdown: string;
  fullMarkdown: string;
  requirements: string;
  deliverables: string;
  version: string;
  createdAt: string;
  updatedAt: string;
}

export async function adminRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<
  { ok: true; data: T } | { ok: false; status: number; error: ApiError }
> {
  try {
    const response = await fetch(`/api/v1/admin${path}`, {
      ...init,
      credentials: "same-origin",
      headers: {
        Accept: "application/json",
        ...(init.body ? { "Content-Type": "application/json" } : {}),
        ...init.headers,
      },
    });
    if (response.ok) {
      return { ok: true, data: (await response.json()) as T };
    }
    const error = (await response.json().catch(() => ({
      code: "REQUEST_FAILED",
      message: "请求没有成功，请稍后再试",
    }))) as ApiError;
    return { ok: false, status: response.status, error };
  } catch {
    return {
      ok: false,
      status: 503,
      error: { code: "NETWORK_ERROR", message: "暂时无法连接服务，请稍后再试" },
    };
  }
}
