import { expect, test } from "@playwright/test";
import { MEBIBYTE } from "../src/lib/file-types";

/** 走真实 Next → Java multipart 链路，未登录只验证转发完整性，不产生 OSS 对象。 */
test("multipart above the old 10 MiB proxy limit reaches Java and receives its Result", async ({
  request,
}) => {
  const response = await request.post("/api/v1/admin/media", {
    multipart: {
      kind: "ATTACHMENT",
      file: {
        name: "relay-check.txt",
        mimeType: "text/plain",
        buffer: Buffer.alloc(12 * MEBIBYTE),
      },
    },
  });
  expect(response.status()).toBe(401);
  expect(await response.json()).toMatchObject({
    code: "UNAUTHENTICATED",
    data: null,
  });
});
