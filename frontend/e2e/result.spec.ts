import { expect, test } from "@playwright/test";
import { readApiResponse } from "../src/lib/api";
const traceId = "11111111-1111-4111-8111-111111111111";

function response(code: string, data: unknown, status = 200, headers = {}) {
  return Response.json(
    { code, msg: "提示", data, traceId },
    { status, headers },
  );
}

test("Result parser unwraps success and keeps empty results", async () => {
  expect(await readApiResponse(response("SUCCESS", { id: "41" }))).toEqual({
    ok: true,
    data: { id: "41" },
  });
  expect(await readApiResponse(response("SUCCESS", null))).toEqual({
    ok: true,
    data: null,
  });
});

test("Result parser requires both HTTP and business success", async () => {
  expect(await readApiResponse(response("FORBIDDEN", null))).toMatchObject({
    ok: false,
    error: { code: "FORBIDDEN", msg: "提示", traceId },
  });
  expect(await readApiResponse(response("SUCCESS", null, 503))).toMatchObject({
    ok: false,
    status: 503,
  });
});

test("Result parser rejects old and malformed payloads", async () => {
  expect(await readApiResponse(Response.json({ id: "41" }))).toMatchObject({
    ok: false,
    status: 502,
    error: { code: "INVALID_RESPONSE" },
  });
  expect(await readApiResponse(new Response("invalid-json"))).toMatchObject({
    ok: false,
    error: { code: "INVALID_RESPONSE" },
  });
});

test("Result parser preserves trace and retry countdown", async () => {
  expect(
    await readApiResponse(
      response("AUTH_RATE_LIMITED", { retryAfterSeconds: 10 }, 429),
    ),
  ).toMatchObject({ ok: false, error: { traceId, retryAfterSeconds: 10 } });
  expect(
    await readApiResponse(
      response("AUTH_RATE_LIMITED", null, 429, { "Retry-After": "30" }),
    ),
  ).toMatchObject({ ok: false, error: { traceId, retryAfterSeconds: 30 } });
});
