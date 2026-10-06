export interface CurrentUser {
  id: string;
  email: string;
  nickname: string;
  bio?: string;
  avatarFileId?: string | null;
  role: "USER" | "ADMIN";
  emailVerified: boolean;
  createdAt: string;
}

export { apiRequest } from "./api";
export type { ApiError } from "./api";
import { apiRequest } from "./api";

export function authRequest<T>(path: string, init: RequestInit = {}) {
  return apiRequest<T>(`/api/v1/auth${path}`, init);
}
