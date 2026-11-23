import { NextResponse, type NextRequest } from "next/server";
import { LOCALE_COOKIE, isLocale, locales, matchLocale } from "@/i18n/config";

/**
 * 把没有语言前缀的请求（如 `/`）跳转到访客的语言：
 * 优先使用语言切换时写入的 Cookie，其次是 Accept-Language。
 */
export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const hasLocale = locales.some((l) => pathname === `/${l}` || pathname.startsWith(`/${l}/`));
  if (hasLocale) return;

  const saved = request.cookies.get(LOCALE_COOKIE)?.value;
  const locale = isLocale(saved) ? saved : matchLocale(request.headers.get("accept-language"));

  const url = request.nextUrl.clone();
  url.pathname = `/${locale}${pathname === "/" ? "" : pathname}`;
  const response = NextResponse.redirect(url);
  response.headers.set("Vary", "Accept-Language, Cookie");
  return response;
}

export const config = {
  // 跳过 Next.js 内部路径和看起来像文件的请求（图标、robots.txt、sitemap.xml 等）。
  matcher: ["/((?!_next|.*\\..*).*)"],
};
