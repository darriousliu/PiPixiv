import type { Locale } from "@/i18n/config";

const repo = "darriousliu/PiPixiv";
const github = `https://github.com/${repo}`;

export const siteConfig = {
  name: "PiPixiv",
  /** 站点的正式地址，用于 canonical、hreflang、Open Graph 和 sitemap。可用 NEXT_PUBLIC_SITE_URL 覆盖。 */
  url: process.env.NEXT_PUBLIC_SITE_URL || "https://pipixiv.com",
  repo,
  links: {
    github,
    releases: `${github}/releases`,
    latestRelease: `${github}/releases/latest`,
    issues: `${github}/issues`,
    telegram: "https://t.me/PiPixivChannel",
  },
  readme: {
    zh: `${github}#readme`,
    en: `${github}/blob/master/.github/README-en.md`,
  } satisfies Record<Locale, string>,
  platformCount: 5,
  license: "Apache-2.0",
} as const;
