import { apiRequest } from "./api";
import type { components } from "./api-schema";
export type AdminCategory = components["schemas"]["AdminCategory"];
export type AdminPublicationSummary =
  components["schemas"]["AdminPublicationSummary"];
export type AdminPublicationDetail =
  components["schemas"]["AdminPublicationDetail"];
export function adminRequest<T>(path: string, init: RequestInit = {}) {
  return apiRequest<T>(`/api/v1/admin${path}`, init);
}
