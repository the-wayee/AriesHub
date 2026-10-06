import type { components } from "./api-schema";
export type Overview = components["schemas"]["OperationsOverview"];
export type Member = components["schemas"]["AdminMember"];
export type CommentRow = components["schemas"]["ModerationComment"];
export type LedgerRow = components["schemas"]["LedgerEntry"];
export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}
