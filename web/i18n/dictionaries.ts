import { notFound } from "next/navigation";
import { locale as localeParam } from "next/root-params";
import { isLocale, type Locale } from "./config";
import type zh from "./dictionaries/zh.json";

/** 所有语言的文案结构必须与中文一致。 */
export type Dictionary = typeof zh;

const loaders: Record<Locale, () => Promise<Dictionary>> = {
  zh: () => import("./dictionaries/zh.json").then((m) => m.default),
  en: () => import("./dictionaries/en.json").then((m) => m.default),
};

/** 当前路由的语言，从 `[locale]` 根路由段读取。 */
export async function getLocale(): Promise<Locale> {
  const value = await localeParam();
  if (!isLocale(value)) notFound();
  return value;
}

export async function getDictionary(locale: Locale): Promise<Dictionary> {
  return loaders[locale]();
}
