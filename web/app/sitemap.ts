import type { MetadataRoute } from "next";
import { localeMeta, locales } from "@/i18n/config";
import { siteConfig } from "@/lib/site";

export default function sitemap(): MetadataRoute.Sitemap {
  const url = (locale: string) => new URL(`/${locale}`, siteConfig.url).toString();
  const languages = Object.fromEntries(locales.map((l) => [localeMeta[l].htmlLang, url(l)]));

  return locales.map((locale) => ({
    url: url(locale),
    changeFrequency: "weekly",
    priority: 1,
    alternates: { languages },
  }));
}
