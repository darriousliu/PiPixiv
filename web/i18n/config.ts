/**
 * 语言注册表。新增一种语言：
 *   1. 在 `locales` 中加入语言代码，并在 `localeMeta` 中添加对应条目；
 *   2. 创建 `i18n/dictionaries/<code>.json`（结构与 zh.json 相同），并在 `dictionaries.ts` 中登记；
 *   3. （可选）在 `assets/screenshots/<code>/` 放入该语言的截图，并在 `lib/screenshots.ts` 中引用。
 */
export const locales = ["zh", "en"] as const;

export type Locale = (typeof locales)[number];

/** 访客的首选语言都不受支持时使用。 */
export const fallbackLocale: Locale = "en";

export const LOCALE_COOKIE = "NEXT_LOCALE";

export const localeMeta: Record<
  Locale,
  {
    /** 用于 `<html lang>` 和 `hreflang` 的 BCP 47 标签。 */
    htmlLang: string;
    /** Open Graph 的 locale。 */
    ogLocale: string;
    /** 语言切换菜单中显示的名称，用该语言本身书写。 */
    label: string;
    /** 紧凑切换按钮上的简短标签。 */
    short: string;
    /** 映射到该语言的 Accept-Language 主子标签。 */
    matches: string[];
  }
> = {
  zh: { htmlLang: "zh-CN", ogLocale: "zh_CN", label: "简体中文", short: "中", matches: ["zh"] },
  en: { htmlLang: "en", ogLocale: "en_US", label: "English", short: "EN", matches: ["en"] },
};

export function isLocale(value: string | undefined | null): value is Locale {
  return !!value && (locales as readonly string[]).includes(value);
}

/** 根据 Accept-Language 请求头选出最合适的受支持语言。 */
export function matchLocale(acceptLanguage: string | null | undefined): Locale {
  if (!acceptLanguage) return fallbackLocale;
  const ranked = acceptLanguage
    .split(",")
    .map((part) => {
      const [tag, ...params] = part.trim().toLowerCase().split(";");
      const q = params.find((p) => p.trim().startsWith("q="));
      return { tag, q: q ? Number(q.trim().slice(2)) || 0 : 1 };
    })
    .filter((entry) => entry.tag && entry.q > 0)
    .sort((a, b) => b.q - a.q);

  for (const { tag } of ranked) {
    const primary = tag.split("-")[0];
    const found = locales.find((l) => l === tag || localeMeta[l].matches.includes(primary));
    if (found) return found;
  }
  return fallbackLocale;
}
