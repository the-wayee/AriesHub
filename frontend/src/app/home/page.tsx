import type { Metadata } from "next";
import { HomeView } from "@/components/community/home-view";

export const metadata: Metadata = { title: "成员首页" };
export default function HomePage() {
  return <HomeView />;
}
