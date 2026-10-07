"use client";
import Image from "next/image";
import { useState } from "react";

/** 后端提供已授权的头像签名；加载失败时回退首字母，不额外请求存储接口。 */
export function UserAvatar({
  name,
  url,
  size = 32,
  className = "",
  tone,
}: {
  name: string;
  url?: string | null;
  size?: number;
  className?: string;
  tone?: number;
}) {
  const [failed, setFailed] = useState<string>();
  return (
    <span
      className={`user-avatar ${className}`}
      style={{ width: size, height: size }}
      data-tone={tone}
      role="img"
      aria-label={`${name}的头像`}
    >
      {url && failed !== url ? (
        <Image
          src={url}
          alt=""
          width={size}
          height={size}
          unoptimized
          onError={() => setFailed(url)}
        />
      ) : (
        name.slice(0, 1)
      )}
    </span>
  );
}
