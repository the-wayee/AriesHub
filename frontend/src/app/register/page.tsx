import type { Metadata } from "next";
import { AuthExperience } from "@/components/studio/auth-experience";
export const metadata: Metadata = { title: "注册" };
export default function Page() {
  return <AuthExperience mode="register" />;
}
