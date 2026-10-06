"use client";

import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import type { CurrentUser } from "@/lib/auth";
import { userRequest } from "@/lib/user";

type AuthSessionValue = {
  user: CurrentUser | null | undefined;
  hasSession: boolean;
  avatarUrl: string | null;
  sessionError: string | null;
};

const AuthSessionContext = createContext<AuthSessionValue | null>(null);

export function AuthSessionProvider({
  children,
  initialHasSession,
}: {
  children: ReactNode;
  initialHasSession: boolean;
}) {
  const [user, setUser] = useState<CurrentUser | null | undefined>(
    initialHasSession ? undefined : null,
  );
  const [avatar, setAvatar] = useState<{ id: string; url: string } | null>(
    null,
  );
  const [sessionError, setSessionError] = useState<string | null>(null);

  const avatarId = user?.avatarFileId;
  const userId = user?.id;
  useEffect(() => {
    if (!avatarId || !userId) return;
    let active = true;
    const refresh = () => {
      void userRequest<{ url: string }>("/me/avatar-url").then((result) => {
        if (active)
          setAvatar(
            result.ok
              ? { id: `${userId}:${avatarId}`, url: result.data.url }
              : null,
          );
      });
    };
    refresh();
    const timer = window.setInterval(refresh, 4 * 60 * 1000);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, [avatarId, userId]);

  useEffect(() => {
    let active = true;
    let revision = 0;
    const verify = () => {
      const requestRevision = ++revision;
      void userRequest<CurrentUser>("/me").then((result) => {
        if (!active || requestRevision !== revision) return;
        if (result.ok) {
          setUser(result.data);
          setSessionError(null);
        } else if (
          [
            "UNAUTHENTICATED",
            "UNAUTHORIZED",
            "USER_NOT_FOUND",
            "ACCOUNT_DISABLED",
          ].includes(result.error.code)
        ) {
          setUser(null);
          setSessionError(null);
        } else setSessionError(result.error.msg);
      });
    };
    const sync = (event: Event) => {
      const detail = (event as CustomEvent<CurrentUser | null>).detail;
      if (detail !== undefined) {
        ++revision;
        setUser(detail);
        setSessionError(null);
      } else verify();
    };

    if (initialHasSession) verify();
    window.addEventListener("arieshub:auth", sync);
    return () => {
      active = false;
      window.removeEventListener("arieshub:auth", sync);
    };
  }, [initialHasSession]);

  return (
    <AuthSessionContext.Provider
      value={{
        user,
        hasSession: user !== null,
        avatarUrl: avatar?.id === `${userId}:${avatarId}` ? avatar.url : null,
        sessionError,
      }}
    >
      {children}
    </AuthSessionContext.Provider>
  );
}

export function useAuthSession() {
  const session = useContext(AuthSessionContext);
  if (!session) {
    throw new Error("useAuthSession must be used within AuthSessionProvider");
  }
  return session;
}
