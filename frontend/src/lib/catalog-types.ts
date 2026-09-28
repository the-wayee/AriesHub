import type { components } from "./api-schema";

export type AccessType = components["schemas"]["AccessType"];
export type Category = components["schemas"]["Category"];
export type CaseSummary = components["schemas"]["CaseSummary"];
export type CaseDetail = components["schemas"]["CaseDetail"];
export type CaseContent = components["schemas"]["CaseContent"];
export type CasePage = components["schemas"]["CasePage"];

export interface CatalogFilters {
  q: string;
  category: string;
  access: string;
  page: number;
  size: number;
}
