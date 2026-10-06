import { apiRequest } from "./api";

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

export function adminRequest<T>(path: string, init: RequestInit = {}) {
  return apiRequest<T>(`/api/v1/admin${path}`, init);
}
