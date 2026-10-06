import { expect, test } from "@playwright/test";
import { apiRequest } from "../src/lib/api";

function success() {
  return Response.json({
    code: "SUCCESS",
    msg: "成功",
    data: { id: "1" },
    traceId: "test-trace",
  });
}

/** 独立验证请求层；恢复全局对象，避免污染同一测试进程的后续用例。 */
async function withFetch(
  browser: boolean,
  fetcher: typeof fetch,
  run: () => Promise<void>,
) {
  const originalFetch = globalThis.fetch;
  const originalWindow = Object.getOwnPropertyDescriptor(globalThis, "window");
  Object.defineProperty(globalThis, "window", {
    configurable: true,
    value: browser ? {} : undefined,
  });
  globalThis.fetch = fetcher;
  try {
    await run();
  } finally {
    globalThis.fetch = originalFetch;
    if (originalWindow)
      Object.defineProperty(globalThis, "window", originalWindow);
    else Reflect.deleteProperty(globalThis, "window");
  }
}

test("identical pending reads share one response, completed reads are fetched again", async () => {
  let requests = 0;
  await withFetch(
    true,
    async () => {
      requests++;
      return success();
    },
    async () => {
      const first = apiRequest("/api/v1/users/me");
      const second = apiRequest("/api/v1/users/me", {
        method: "get",
        headers: new Headers({ Accept: "application/json" }),
      });
      expect(first).toBe(second);
      expect(await first).toEqual({ ok: true, data: { id: "1" } });
      await second;
      expect(requests).toBe(1);
      await apiRequest("/api/v1/users/me");
      expect(requests).toBe(2);
    },
  );
});

test("different paths, headers, cache policies and cancellation signals stay independent", async () => {
  let requests = 0;
  await withFetch(
    true,
    async () => {
      requests++;
      return success();
    },
    async () => {
      await Promise.all([
        apiRequest("/api/v1/users/me"),
        apiRequest("/api/v1/users/me?page=2"),
        apiRequest("/api/v1/users/me", {
          headers: { Authorization: "Bearer other-session" },
        }),
        apiRequest("/api/v1/users/me", { cache: "no-store" }),
        apiRequest("/api/v1/users/me", {
          signal: new AbortController().signal,
        }),
        apiRequest("/api/v1/users/me", {
          signal: new AbortController().signal,
        }),
      ]);
      expect(requests).toBe(6);
    },
  );
});

test("server reads and browser writes are never shared", async () => {
  let requests = 0;
  const fetcher: typeof fetch = async () => {
    requests++;
    return success();
  };
  await withFetch(false, fetcher, async () => {
    await Promise.all([
      apiRequest("/api/v1/users/me"),
      apiRequest("/api/v1/users/me"),
    ]);
    expect(requests).toBe(2);
  });
  await withFetch(true, fetcher, async () => {
    await Promise.all([
      apiRequest("/api/v1/auth/logout", { method: "POST" }),
      apiRequest("/api/v1/auth/logout", { method: "POST" }),
    ]);
    expect(requests).toBe(4);
  });
});

test("writes invalidate earlier reads and late responses cannot remove a newer request", async () => {
  const resolvers: Array<(response: Response) => void> = [];
  await withFetch(
    true,
    () => new Promise<Response>((resolve) => resolvers.push(resolve)),
    async () => {
      const oldRead = apiRequest("/api/v1/users/me");
      const write = apiRequest("/api/v1/users/me", {
        method: "PUT",
        body: "{}",
      });
      const duringWrite = apiRequest("/api/v1/users/me");
      expect(resolvers).toHaveLength(3);
      resolvers[1](success());
      await write;
      const freshRead = apiRequest("/api/v1/users/me");
      expect(resolvers).toHaveLength(4);
      resolvers[0](success());
      await oldRead;
      expect(apiRequest("/api/v1/users/me")).toBe(freshRead);
      resolvers[2](success());
      resolvers[3](success());
      await Promise.all([duringWrite, freshRead]);
    },
  );
});
