"use client";

import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { authRequest, type CurrentUser } from "@/lib/auth";

type AuthSessionValue = {
  user: CurrentUser | null | undefined;
  hasSession: boolean;
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

  useEffect(() => {
    let active = true;
    const verify = () => {
      void authRequest<CurrentUser>("/me").then((result) => {
        if (active) setUser(result.ok ? result.data : null);
      });
    };
    const sync = (event: Event) => {
      const detail = (event as CustomEvent<CurrentUser | null>).detail;
      if (detail !== undefined) setUser(detail);
      else verify();
    };

    if (initialHasSession) verify();
    window.addEventListener("arieshub:auth", sync);
    return () => {
      active = false;
      window.removeEventListener("arieshub:auth", sync);
    };
  }, [initialHasSession]);

  return (
    <AuthSessionContext.Provider value={{ user, hasSession: user !== null }}>
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
