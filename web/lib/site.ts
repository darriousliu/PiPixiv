import type { Locale } from "@/i18n/config";

const repo = "darriousliu/PiPixiv";
const github = `https://github.com/${repo}`;

export const siteConfig = {
  name: "PiPixiv",
  /** 站点的公开地址。确定域名后设置 NEXT_PUBLIC_SITE_URL。 */
  url: process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000",
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
