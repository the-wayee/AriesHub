import { NextResponse, type NextRequest } from "next/server";
import { isProtectedRoute } from "@/lib/protected-routes";

// 快速阻止无会话请求；Cookie 真实性仍由 Java /me 和每个业务 API 验证。
export function proxy(request: NextRequest) {
  if (
    isProtectedRoute(request.nextUrl.pathname) &&
    !request.cookies.get("arieshub_token")?.value
  ) {
    const destination = new URL("/login", request.url);
    destination.searchParams.set(
      "next",
      request.nextUrl.pathname + request.nextUrl.search,
    );
    return NextResponse.redirect(destination);
  }
  return NextResponse.next();
}
export const config = {
  matcher: [
    "/home/:path*",
    "/discover/:path*",
    "/publications/:path*",
    "/community/:path*",
    "/my-content/:path*",
    "/members/:path*",
    "/account/:path*",
    "/learn/:path*",
    "/checkout/:path*",
    "/admin/:path*",
    "/studio/:path*",
  ],
};
