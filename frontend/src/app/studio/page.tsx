import { redirect } from "next/navigation";

// 旧视觉原型统一进入真实内容后台。
export default function StudioPage() {
  redirect("/admin");
}
