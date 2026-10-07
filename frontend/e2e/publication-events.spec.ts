import { expect, test } from "@playwright/test";
import { fulfillResult } from "./api-result";
import { mockMemberSession } from "./member-session";

const shareToken = "s".repeat(43);
const interaction = {
  publicationId: "11",
  likeCount: 4,
  bookmarked: false,
  liked: false,
  bookmarkCount: 3,
  shareCount: 8,
  viewCount: 106,
};

test("hover previews without counting and repeated share clicks count once", async ({
  page,
}, info) => {
  await mockMemberSession(page);
  let shares = 0;
  await page.route("**/api/v1/publications/11/interaction", (route) =>
    fulfillResult(route, { json: interaction }),
  );
  await page.route("**/api/v1/publications/11/share-link", (route) =>
    fulfillResult(route, {
      json: {
        publicationId: "11",
        url: `https://community.example.com/s/${shareToken}`,
        token: shareToken,
        cover: {
          url: "/concepts/automation-color.webp",
          expiresAt: "2099-01-01T00:00:00Z",
        },
        summary: "一次完整的创作实践。",
      },
    }),
  );
  await page.route("**/api/v1/publications/11/share", (route) => {
    expect(route.request().postDataJSON().token).toBe(shareToken);
    return fulfillResult(route, {
      json: { ...interaction, shareCount: 8 + ++shares },
    });
  });
  await page.route("**/api/v1/publications/11/view", (route) =>
    fulfillResult(route, { json: interaction }),
  );
  await page.addInitScript(() =>
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: {
        writeText: async (url: string) => {
          document.documentElement.dataset.shareUrl = url;
        },
      },
    }),
  );
  await page.goto("/publications/11");
  const actions = page.getByLabel("文章操作", { exact: true });
  const trigger = actions.getByRole("button", { name: "分享文章" });
  await expect(trigger).toBeEnabled();
  if (info.project.name === "desktop") {
    await trigger.hover();
    await expect(page.locator(".share-preview-code svg")).toBeVisible();
    expect(shares).toBe(0);
  }
  await trigger.click();
  await expect(trigger).toContainText("9");
  expect(shares).toBe(1);
  await expect(page.locator(".share-preview-code svg")).toBeVisible();
  const qq = page.getByRole("link", { name: "QQ", exact: true });
  await expect(qq).toHaveAttribute("href", /connect\.qq\.com\/widget\/shareqq/);
  expect(
    new URL((await qq.getAttribute("href"))!).searchParams.get("url"),
  ).toBe(`https://community.example.com/s/${shareToken}`);
  await page.getByRole("button", { name: "微信", exact: true }).click();
  await expect(
    page.getByText("微信扫一扫上方二维码，打开文章后可转发给好友。"),
  ).toBeVisible();
  await page.getByRole("button", { name: "复制链接", exact: true }).click();
  await expect(page.locator("html")).toHaveAttribute(
    "data-share-url",
    `https://community.example.com/s/${shareToken}`,
  );
  for (let i = 0; i < 6; i++)
    await page.getByRole("button", { name: "链接已复制", exact: true }).click();
  expect(shares).toBe(1);
  await page
    .locator(".publication-share-popup")
    .screenshot({ path: info.outputPath("share-preview-card.png") });
  await page.evaluate(() =>
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: {
        writeText: async () => {
          throw new Error("denied");
        },
      },
    }),
  );
  await page.getByRole("button", { name: "链接已复制", exact: true }).click();
  await expect(
    page.locator(".publication-share-popup").getByRole("alert"),
  ).toContainText("自动复制失败");
  expect(shares).toBe(1);
  await page.keyboard.press("Escape");
  for (let i = 0; i < 3; i++) {
    await trigger.click();
    await page.keyboard.press("Escape");
  }
  expect(shares).toBe(1);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});

test("visible article records reading and updates the displayed count", async ({
  page,
}) => {
  await mockMemberSession(page);
  let views = 0;
  await page.route("**/api/v1/publications/11/interaction", (route) =>
    fulfillResult(route, { json: interaction }),
  );
  await page.route("**/api/v1/publications/11/view", (route) => {
    views++;
    return fulfillResult(route, { json: { ...interaction, viewCount: 107 } });
  });
  await page.goto("/publications/11");
  await expect(
    page.locator(".published-meta .publication-metrics"),
  ).toContainText("107 阅读", { timeout: 10000 });
  expect(views).toBe(1);
});
