"use client";

import Link from "next/link";
import { useAuthSession } from "@/components/auth-session";
import { Menu } from "@base-ui/react/menu";
import {
  Bookmark,
  ChevronDown,
  LogOut,
  Settings2,
  SlidersHorizontal,
  ArrowUpRight,
} from "lucide-react";
import { UserAvatar } from "./user-avatar";
import { useLogout } from "./use-logout";

/** 用服务端可见的会话 Cookie 决定首帧，随后向 /me 核实用户信息。 */
export function AuthNav() {
  const { user, avatarUrl } = useAuthSession();
  const { logout, busy, error } = useLogout();

  if (user === undefined) {
    return <span className="auth-nav-placeholder" aria-hidden="true" />;
  }
  if (!user) {
    return (
      <div className="auth-nav">
        <Link href="/login">登录</Link>
        <Link className="header-register" href="/register">
          注册
        </Link>
      </div>
    );
  }
  return (
    <div className="signed-in-nav user-menu">
      <Menu.Root>
        <Menu.Trigger className="user-menu-trigger" aria-label="打开用户菜单">
          <UserAvatar name={user.nickname} url={avatarUrl} />
          <span className="user-menu-name">{user.nickname}</span>
          <ChevronDown aria-hidden="true" />
        </Menu.Trigger>
        <Menu.Portal>
          <Menu.Positioner
            sideOffset={10}
            align="end"
            className="user-menu-positioner"
          >
            <Menu.Popup className="user-menu-popup" aria-label="用户快捷操作">
              <div className="user-menu-summary">
                <UserAvatar name={user.nickname} url={avatarUrl} />
                <div>
                  <strong>{user.nickname}</strong>
                  <small>{user.email}</small>
                </div>
              </div>
              <Menu.LinkItem
                render={<Link href="/account" />}
                closeOnClick
                className="user-menu-item"
              >
                <Settings2 />
                账号设置
                <ArrowUpRight className="menu-arrow" />
              </Menu.LinkItem>
              <Menu.LinkItem
                render={<Link href="/my-content" />}
                closeOnClick
                className="user-menu-item"
              >
                <Bookmark />
                我的空间
                <ArrowUpRight className="menu-arrow" />
              </Menu.LinkItem>
              {user.role === "ADMIN" && (
                <Menu.LinkItem
                  render={<Link href="/admin" />}
                  closeOnClick
                  className="user-menu-item"
                >
                  <SlidersHorizontal />
                  内容后台
                  <ArrowUpRight className="menu-arrow" />
                </Menu.LinkItem>
              )}
              <Menu.Separator className="user-menu-separator" />
              <Menu.Item
                className="user-menu-item user-menu-logout"
                disabled={busy}
                closeOnClick={false}
                onClick={() => void logout()}
              >
                <LogOut />
                {busy ? "正在退出…" : "退出登录"}
              </Menu.Item>
              {error && (
                <p className="user-menu-error" role="alert">
                  {error}
                </p>
              )}
            </Menu.Popup>
          </Menu.Positioner>
        </Menu.Portal>
      </Menu.Root>
    </div>
  );
}
