"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { authRequest } from "@/lib/auth";

export function useLogout() {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function logout() {
    if (busy) return;
    setBusy(true);
    setError("");
    const result = await authRequest<void>("/logout", { method: "POST" });
    if (!result.ok) {
      setError(result.error.msg);
      setBusy(false);
      return;
    }
    window.dispatchEvent(new CustomEvent("arieshub:auth", { detail: null }));
    router.push("/");
    router.refresh();
    setBusy(false);
  }
  return { logout, busy, error };
}
