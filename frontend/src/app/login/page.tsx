import type { Metadata } from "next";
import { AuthExperience } from "@/components/studio/auth-experience";
export const metadata: Metadata = { title: "登录" };
export default async function Page({
  searchParams,
}: {
  searchParams: Promise<{ email?: string | string[] }>;
}) {
  const { email } = await searchParams;
  return (
    <AuthExperience
      mode="login"
      initialEmail={typeof email === "string" ? email : ""}
    />
  );
}
