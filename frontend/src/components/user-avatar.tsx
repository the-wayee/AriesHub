"use client";

import Image from "next/image";

export function UserAvatar({
  name,
  url,
  className = "",
}: {
  name: string;
  url?: string | null;
  className?: string;
}) {
  return (
    <span className={`user-avatar ${className}`}>
      {url ? (
        <Image
          src={url}
          alt={`${name}的头像`}
          width={160}
          height={160}
          unoptimized
        />
      ) : (
        <span aria-hidden="true">{Array.from(name)[0] || "A"}</span>
      )}
    </span>
  );
}
