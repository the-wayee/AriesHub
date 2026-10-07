"use client";

import { useCallback, useSyncExternalStore, type MouseEvent } from "react";
import { usePathname } from "next/navigation";

const ENTRY_EVENT = "arieshub:publication-entry";
const ENTRY_KEY = "arieshub:publication-entry:";
const entries = new Map<string, string>();
const sources = new Map([
  ["/home", "返回首页"],
  ["/discover", "返回探索"],
  ["/my-content", "返回我的空间"],
  ["/community", "返回讨论"],
]);

function validSource(value: string | null): string | null {
  if (!value || !value.startsWith("/") || value.startsWith("//")) return null;
  return sources.has(value.split(/[?#]/)[0]) ? value : null;
}

/** 在跳转前记录实际列表地址，统一覆盖封面、标题、阅读记录和动态入口。 */
export function rememberPublicationEntry(event: MouseEvent<HTMLElement>) {
  if (event.button !== 0 || !(event.target instanceof Element)) return;
  const anchor = event.target.closest<HTMLAnchorElement>("a[href]");
  if (!anchor) return;
  const destination = new URL(anchor.href, window.location.href);
  if (
    destination.origin !== window.location.origin ||
    !/^\/publications\/[1-9]\d*$/.test(destination.pathname)
  )
    return;
  const source = validSource(window.location.pathname + window.location.search);
  if (!source) return;
  entries.set(destination.pathname, source);
  try {
    // 会话级保存仅供导航，文章刷新后仍能返回原筛选列表，不影响内容权限。
    sessionStorage.setItem(ENTRY_KEY + destination.pathname, source);
  } catch {
    /* 禁用浏览器存储时仍可使用当前会话的内存记录。 */
  }
  window.dispatchEvent(new Event(ENTRY_EVENT));
}

function subscribe(listener: () => void) {
  window.addEventListener(ENTRY_EVENT, listener);
  return () => window.removeEventListener(ENTRY_EVENT, listener);
}

/** 直接打开文章没有来源时回探索；只允许已知站内列表，避免外链或文章互相回跳。 */
export function usePublicationReturn() {
  const path = usePathname();
  const snapshot = useCallback(() => {
    if (!/^\/publications\/[1-9]\d*$/.test(path)) return "/discover";
    let source = entries.get(path) ?? null;
    try {
      source ??= sessionStorage.getItem(ENTRY_KEY + path);
    } catch {
      /* 存储不可用时保持内存来源。 */
    }
    return validSource(source) ?? "/discover";
  }, [path]);
  const href = useSyncExternalStore(subscribe, snapshot, () => "/discover");
  const section = href.split(/[?#]/)[0];
  return { href, section, label: sources.get(section)! };
}
