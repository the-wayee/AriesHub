import type { components } from "./api-schema";
// 读取契约统一从 OpenAPI 生成，避免 UI 与后端字段分别维护。
export type Interaction = components["schemas"]["PublicationInteraction"];
export type ReadingProgress = components["schemas"]["ReadingProgress"];
export type PublicationCardData = components["schemas"]["PublicationCard"];
export type ReaderPage = components["schemas"]["PublicationCardPage"];
export type MemberHome = components["schemas"]["MemberHome"];
export const CONTENT_FORM_LABELS: Record<string, string> = {
  CASE_STUDY: "实战案例",
  ARTICLE: "学习文章",
  COURSE: "课程",
};
