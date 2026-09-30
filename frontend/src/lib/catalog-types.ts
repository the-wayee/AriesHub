import type { components } from "./api-schema";

export type AccessType = components["schemas"]["AccessType"];
export type PublicationType = components["schemas"]["PublicationType"];
export type Category = components["schemas"]["Category"];
export type PublicationSummary = components["schemas"]["PublicationSummary"];
export type PublicationDetail = components["schemas"]["PublicationDetail"];
export type PublicationContent = components["schemas"]["PublicationContent"];
export type PublicationPage = components["schemas"]["PublicationPage"];

export interface CatalogFilters {
  q: string;
  category: string;
  type: string;
  access: string;
  page: number;
  size: number;
}
