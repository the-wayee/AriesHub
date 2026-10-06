import type { Route } from "@playwright/test";

/** 构造 Java Result 响应；与生产解析器共享字段契约。 */
export async function fulfillResult(
  route: Route,
  options: NonNullable<Parameters<Route["fulfill"]>[0]>,
) {
  if (!route.request().url().includes("/api/")) return route.fulfill(options);
  const status = options.status === 204 ? 200 : (options.status ?? 200);
  const payload =
    options.json ??
    (typeof options.body === "string" && options.body
      ? JSON.parse(options.body)
      : null);
  const failed = status >= 400;
  const result = {
    code: failed ? payload.code : "SUCCESS",
    msg: failed ? (payload.msg ?? payload.message) : "操作成功",
    data: failed
      ? payload.retryAfterSeconds
        ? { retryAfterSeconds: payload.retryAfterSeconds }
        : null
      : payload,
    traceId:
      payload?.traceId ??
      payload?.requestId ??
      "11111111-1111-4111-8111-111111111111",
  };
  const { body: _body, json: _json, ...rest } = options;
  void _body;
  void _json;
  return route.fulfill({ ...rest, status, json: result });
}
