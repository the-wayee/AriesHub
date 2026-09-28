import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  poweredByHeader: false,
  async rewrites() {
    // 开发阶段转发到 Java；生产环境由部署网关统一路由 /api/v1。
    if (process.env.NODE_ENV !== "development") return [];
    const backend = new URL(
      process.env.BACKEND_ORIGIN ?? "http://127.0.0.1:8080",
    );
    if (
      !["http:", "https:"].includes(backend.protocol) ||
      backend.username ||
      backend.password ||
      backend.pathname !== "/" ||
      backend.search ||
      backend.hash
    ) {
      throw new Error(
        "BACKEND_ORIGIN must be an HTTP(S) origin without credentials or a path.",
      );
    }
    return [
      {
        source: "/api/v1/:path*",
        destination: `${backend.origin}/api/v1/:path*`,
      },
    ];
  },
};
export default nextConfig;
