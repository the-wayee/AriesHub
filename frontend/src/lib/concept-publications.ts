/** 落地页与独立预览使用的静态示例；ID 仅在 /preview 下有效，不对应数据库文章。 */
export type ConceptPublication = {
  id: string;
  title: string;
  shortTitle: string;
  summary: string;
  category: string;
  categorySlug: string;
  image: string;
  creditPrice: number;
  date: string;
  tagline: string;
  deliverables: string[];
  requirements: string[];
  chapters: { title: string; free: boolean }[];
};

export const conceptPublications: ConceptPublication[] = [
  {
    id: "1",
    title: "从零做一个可上线的网站",
    shortTitle: "从零做一个可上线的网站",
    summary: "从想法、设计到开发与部署，跟着一个完整项目把 AI 协作真正用起来。",
    category: "网站与开发",
    categorySlug: "web",
    image: "/concepts/architecture-color.webp",
    creditPrice: 790,
    date: "2024.12.10",
    tagline: "从想法到真实上线。",
    deliverables: ["完整图文教程", "项目源码", "部署清单", "可复用提示词"],
    requirements: [
      "一台可以使用现代浏览器的电脑",
      "愿意动手完成自己的第一个网站",
    ],
    chapters: [
      { title: "为什么从一个自己的网站开始", free: true },
      { title: "准备工作：工具、思路与规划", free: true },
      { title: "网站设计与页面实现", free: false },
      { title: "功能开发与 AI 协作实践", free: false },
      { title: "部署上线：从本地到公网", free: false },
      { title: "复盘与延伸：还能做什么", free: false },
    ],
  },
  {
    id: "2",
    title: "AI PPT：从想法到交付",
    shortTitle: "用 AI 做一套高质感演示文稿",
    summary:
      "用 AI 完成一份演示文稿的完整流程：梳理内容、建立视觉秩序与表达重点。",
    category: "内容创作",
    categorySlug: "content",
    image: "/concepts/presentation-color.webp",
    creditPrice: 590,
    date: "2024.11.28",
    tagline: "把想法讲清楚。",
    deliverables: ["制作步骤", "内容结构模板", "设计检查清单"],
    requirements: ["一个想要表达的主题", "基础演示软件使用经验"],
    chapters: [
      { title: "从主题到清晰的叙事", free: true },
      { title: "建立演示的视觉系统", free: true },
      { title: "逐页完善与最终交付", free: false },
    ],
  },
  {
    id: "3",
    title: "构建自己的 AI 自动化流程",
    shortTitle: "搭建一个个人 AI 自动化助手",
    summary: "连接常用工具，让重复的信息收集、整理和推送形成真正可用的工作流。",
    category: "自动化",
    categorySlug: "automation",
    image: "/concepts/automation-color.webp",
    creditPrice: 690,
    date: "2024.12.02",
    tagline: "把重复交给系统。",
    deliverables: ["流程搭建教程", "节点配置示例", "排错清单"],
    requirements: ["一个重复出现的工作任务", "常用工具的账号"],
    chapters: [
      { title: "找到值得自动化的任务", free: true },
      { title: "设计流程与关键节点", free: true },
      { title: "接入工具与稳定运行", free: false },
    ],
  },
  {
    id: "4",
    title: "用 AI 整理个人知识库",
    shortTitle: "用 AI 整理个人知识库",
    summary: "从散乱笔记开始，建立能长期使用、方便检索的个人知识整理方法。",
    category: "内容创作",
    categorySlug: "content",
    image: "/concepts/knowledge-color.webp",
    creditPrice: 0,
    date: "2024.11.20",
    tagline: "让知识真正为你所用。",
    deliverables: ["方法教程", "整理模板", "实际示例"],
    requirements: ["一些尚未整理的资料或笔记"],
    chapters: [
      { title: "梳理已有资料", free: true },
      { title: "建立分类与检索规则", free: true },
      { title: "让 AI 协助日常整理", free: true },
    ],
  },
];

export function formatConceptCredits(credits: number) {
  return credits === 0 ? "免费" : `${credits.toLocaleString("zh-CN")} 积分`;
}
