import type { Metadata, Viewport } from "next";
import Script from "next/script";
import { Analytics } from "@vercel/analytics/next";
import { fallbackLocale, localeMeta, locales } from "@/i18n/config";
import { getDictionary, getLocale } from "@/i18n/dictionaries";
import { icons, sans } from "@/lib/fonts";
import { getSocialImage } from "@/lib/screenshots";
import { siteConfig } from "@/lib/site";
import "../globals.css";

export async function generateStaticParams() {
  return locales.map((locale) => ({ locale }));
}

export async function generateMetadata(): Promise<Metadata> {
  const locale = await getLocale();
  const dict = await getDictionary(locale);
  const image = getSocialImage(locale);
  const languages: Record<string, string> = Object.fromEntries(
    locales.map((l) => [localeMeta[l].htmlLang, `/${l}`]),
  );

  return {
    metadataBase: new URL(siteConfig.url),
    title: dict.meta.title,
    description: dict.meta.description,
    keywords: dict.meta.keywords,
    applicationName: siteConfig.name,
    alternates: {
      canonical: `/${locale}`,
      languages: { ...languages, "x-default": `/${fallbackLocale}` },
    },
    openGraph: {
      type: "website",
      siteName: siteConfig.name,
      title: dict.meta.ogTitle,
      description: dict.meta.description,
      url: `/${locale}`,
      locale: localeMeta[locale].ogLocale,
      alternateLocale: locales.filter((l) => l !== locale).map((l) => localeMeta[l].ogLocale),
      images: [{ url: image.src, width: image.width, height: image.height }],
    },
    twitter: {
      card: "summary_large_image",
      title: dict.meta.ogTitle,
      description: dict.meta.description,
      images: [image.src],
    },
    formatDetection: { telephone: false },
  };
}

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f6f8fc" },
    { media: "(prefers-color-scheme: dark)", color: "#070a12" },
  ],
  colorScheme: "light dark",
};

/** 在水合前执行：启用仅 JS 环境下的样式，并应用已保存的主题，避免闪烁。 */
const bootScript = `(function(){var d=document.documentElement;d.classList.add('js');try{var t=localStorage.getItem('pipixiv-theme');if(t==='light'||t==='dark')d.setAttribute('data-theme',t);}catch(e){}})();`;

export default async function RootLayout({ children }: LayoutProps<"/[locale]">) {
  const locale = await getLocale();

  return (
    <html
      lang={localeMeta[locale].htmlLang}
      className={`${sans.variable} ${icons.variable}`}
      suppressHydrationWarning
    >
      <body>
        <Script id="boot" strategy="beforeInteractive">
          {bootScript}
        </Script>
        {children}
        <Analytics />
      </body>
    </html>
  );
}
