import type { CatalogFilters } from "./catalog-types";

type SearchParams = Record<string, string | string[] | undefined>;

export function parseFilters(params: SearchParams): CatalogFilters | null {
  const keys = ["q", "category", "type", "access", "page", "size"] as const;
  if (keys.some((key) => Array.isArray(params[key]))) return null;
  const q = (params.q as string | undefined)?.trim() ?? "";
  const category = (params.category as string | undefined) ?? "";
  const type = (params.type as string | undefined) ?? "";
  const access = (params.access as string | undefined) ?? "";
  const pageText = (params.page as string | undefined) ?? "1";
  const sizeText = (params.size as string | undefined) ?? "9";
  if (!/^\d+$/.test(pageText) || !/^\d+$/.test(sizeText)) return null;
  const page = Number(pageText);
  const size = Number(sizeText);
  if (
    page < 1 ||
    page > 10000 ||
    size < 1 ||
    size > 24 ||
    q.length > 120 ||
    !/^[a-z0-9-]{0,80}$/.test(category) ||
    !["", "CASE_STUDY", "ARTICLE", "COURSE"].includes(type) ||
    !["", "FREE", "CREDIT"].includes(access)
  )
    return null;
  return { q, category, type, access, page, size };
}

export function filtersQuery(
  filters: CatalogFilters,
  page = filters.page,
): string {
  const query = new URLSearchParams();
  if (filters.q) query.set("q", filters.q);
  if (filters.category) query.set("category", filters.category);
  if (filters.type) query.set("type", filters.type);
  if (filters.access) query.set("access", filters.access);
  query.set("page", String(page));
  query.set("size", String(filters.size));
  return query.toString();
}
