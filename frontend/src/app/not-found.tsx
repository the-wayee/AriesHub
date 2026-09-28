import { ContentState } from "@/components/content-state";
export default function NotFound() {
  return (
    <ContentState
      title="这份内容暂时找不到了"
      message="链接可能已失效，或案例尚未公开。可以返回案例库继续探索。"
    />
  );
}
