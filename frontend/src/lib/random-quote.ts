import { readApiResponse } from "./api";
import type { components } from "./api-schema";

export type RandomQuote = components["schemas"]["PhilosophyQuoteView"];

const QUOTE_API = "/api/v1/inspiration/quote";
let pending: Promise<RandomQuote | null> | null = null;

/** 只合并进行中的读取，随机性由后端缓存池提供；浏览器不再访问一言。 */
export function getRandomQuote(): Promise<RandomQuote | null> {
  if (pending) return pending;
  pending = (async () => {
    try {
      const response = await fetch(QUOTE_API, {
        cache: "no-store",
        signal: AbortSignal.timeout(1200),
      });
      const result = await readApiResponse<RandomQuote | null>(response);
      if (!result.ok || !result.data || typeof result.data.text !== "string")
        return null;
      return result.data;
    } catch {
      // 冷启动空池或连接失败时省略文案，不回退到浏览器请求上游。
      return null;
    } finally {
      pending = null;
    }
  })();
  return pending;
}
