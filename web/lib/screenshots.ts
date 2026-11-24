import type { StaticImageData } from "next/image";
import type { Locale } from "@/i18n/config";

// 手机截图宽 1000px，桌面截图宽 1600px。每种界面语言一套。
import zhSearch from "@/assets/screenshots/zh/search.jpg";
import zhDetail from "@/assets/screenshots/zh/detail.jpg";
import zhRanking from "@/assets/screenshots/zh/ranking.jpg";
import zhTags from "@/assets/screenshots/zh/tags.jpg";
import zhSettings from "@/assets/screenshots/zh/settings.jpg";
import zhNetwork from "@/assets/screenshots/zh/network.jpg";
import zhNovel from "@/assets/screenshots/zh/novel.jpg";
import zhDesktopSearch from "@/assets/screenshots/zh/desktop-search.jpg";
import zhDesktopDetail from "@/assets/screenshots/zh/desktop-detail.jpg";
import zhDesktopNovel from "@/assets/screenshots/zh/desktop-novel.jpg";
import zhDesktopAi from "@/assets/screenshots/zh/desktop-ai.jpg";

import enSearch from "@/assets/screenshots/en/search.jpg";
import enDetail from "@/assets/screenshots/en/detail.jpg";
import enRanking from "@/assets/screenshots/en/ranking.jpg";
import enTags from "@/assets/screenshots/en/tags.jpg";
import enSettings from "@/assets/screenshots/en/settings.jpg";
import enNetwork from "@/assets/screenshots/en/network.jpg";
import enDesktopSearch from "@/assets/screenshots/en/desktop-search.jpg";
import enDesktopDetail from "@/assets/screenshots/en/desktop-detail.jpg";
import enDesktopNovel from "@/assets/screenshots/en/desktop-novel.jpg";
import enDesktopAi from "@/assets/screenshots/en/desktop-ai.jpg";

// 分享卡片图，1200×630。
import zhSocial from "@/assets/og/zh.jpg";
import enSocial from "@/assets/og/en.jpg";

/** 对应文案中 `screens.phones` 的键。 */
export type PhoneShotId = "search" | "detail" | "ranking" | "tags" | "settings" | "network" | "novel";
/** 对应文案中 `bigScreen.tabs` / `screens.desktop` 的键。 */
export type DesktopShotId = "search" | "detail" | "novel" | "ai";

export interface ScreenshotSet {
  phones: Record<PhoneShotId, StaticImageData>;
  /** 桌面端 / 平板截图。提供时才渲染大屏区块。 */
  desktop?: Record<DesktopShotId, StaticImageData>;
}

/** 完整的一套截图；其他语言缺少的截图都回退到这里。 */
const base: ScreenshotSet = {
  phones: {
    search: zhSearch,
    detail: zhDetail,
    ranking: zhRanking,
    tags: zhTags,
    settings: zhSettings,
    network: zhNetwork,
    novel: zhNovel,
  },
  desktop: {
    search: zhDesktopSearch,
    detail: zhDesktopDetail,
    novel: zhDesktopNovel,
    ai: zhDesktopAi,
  },
};

/** 各语言的截图。缺少的部分回退到 `base`（中文界面）。 */
const overrides: Partial<Record<Locale, { phones?: Partial<ScreenshotSet["phones"]>; desktop?: ScreenshotSet["desktop"] }>> = {
  en: {
    phones: {
      search: enSearch,
      detail: enDetail,
      ranking: enRanking,
      tags: enTags,
      settings: enSettings,
      network: enNetwork,
      // 小说阅读器没有需要翻译的界面文字，因此复用中文截图。
    },
    desktop: {
      search: enDesktopSearch,
      detail: enDesktopDetail,
      novel: enDesktopNovel,
      ai: enDesktopAi,
    },
  },
};

export function getScreenshots(locale: Locale): ScreenshotSet {
  const o = overrides[locale];
  return {
    phones: { ...base.phones, ...o?.phones },
    desktop: o?.desktop ?? base.desktop,
  };
}

const socialImages: Record<Locale, StaticImageData> = { zh: zhSocial, en: enSocial };

/** 用于 Open Graph / Twitter 卡片的图片。 */
export function getSocialImage(locale: Locale): StaticImageData {
  return socialImages[locale];
}
