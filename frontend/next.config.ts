import type { NextConfig } from "next";
import { MEBIBYTE, PUBLICATION_MEDIA } from "./src/lib/file-types";

const nextConfig: NextConfig = {
  poweredByHeader: false,
  // 与 Java 的 101 MiB multipart 请求上限对齐，为最大视频保留表单边界空间。
  experimental: {
    proxyClientMaxBodySize: PUBLICATION_MEDIA.VIDEO.maxBytes + MEBIBYTE,
    // 后端分片上传期间不会返回最终 Result，避免默认 30 秒切断仍在推进的任务。
    proxyTimeout: 10 * 60 * 1000,
  },
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
