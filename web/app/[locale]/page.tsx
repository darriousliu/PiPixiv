import { localeMeta, locales } from "@/i18n/config";
import { getDictionary, getLocale } from "@/i18n/dictionaries";
import { getLatestRelease, getRepoStats } from "@/lib/github";
import { getScreenshots } from "@/lib/screenshots";
import { siteConfig } from "@/lib/site";
import { BigScreen } from "@/components/BigScreen";
import { Cta } from "@/components/Cta";
import { Download } from "@/components/Download";
import { Faq } from "@/components/Faq";
import { Features } from "@/components/Features";
import { Footer } from "@/components/Footer";
import { Hero } from "@/components/Hero";
import { Interface } from "@/components/Interface";
import { Nav } from "@/components/Nav";
import { OpenSource } from "@/components/OpenSource";
import { PageEffects } from "@/components/PageEffects";
import { Ticker } from "@/components/Ticker";

export default async function HomePage() {
  const locale = await getLocale();
  const [dict, release, stats] = await Promise.all([getDictionary(locale), getLatestRelease(), getRepoStats()]);
  const shots = getScreenshots(locale);
  const numberLocale = localeMeta[locale].htmlLang;

  const jsonLd = {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    name: siteConfig.name,
    description: dict.meta.description,
    url: new URL(`/${locale}`, siteConfig.url).toString(),
    inLanguage: numberLocale,
    applicationCategory: "MultimediaApplication",
    operatingSystem: "Android, iOS, Windows, macOS, Linux",
    softwareVersion: release?.tag.replace(/^v/, ""),
    downloadUrl: release?.url ?? siteConfig.links.releases,
    license: "https://www.apache.org/licenses/LICENSE-2.0",
    isAccessibleForFree: true,
    offers: { "@type": "Offer", price: "0", priceCurrency: "USD" },
    sameAs: [siteConfig.links.github, siteConfig.links.telegram],
  };

  return (
    <>
      <a className="skip-link" href="#main">
        {dict.common.skipToContent}
      </a>
      <Nav
        locale={locale}
        dict={dict.nav}
        stars={stats ? new Intl.NumberFormat(numberLocale).format(stats.stars) : null}
        localeOptions={locales.map((l) => ({ locale: l, label: localeMeta[l].label, short: localeMeta[l].short }))}
        githubUrl={siteConfig.links.github}
        telegramUrl={siteConfig.links.telegram}
      />
      <main id="main">
        <Hero
          dict={dict}
          release={release}
          phones={[
            { id: "tags", image: shots.phones.tags },
            { id: "search", image: shots.phones.search },
            { id: "detail", image: shots.phones.detail },
          ]}
        />
        <Ticker dict={dict.ticker} />
        <Features dict={dict.features} />
        <Interface dict={dict.showcase} screens={dict.screens.phones} phones={shots.phones} />
        {shots.desktop && <BigScreen dict={dict.bigScreen} screens={dict.screens.desktop} desktop={shots.desktop} />}
        <Download locale={locale} dict={dict.download} platforms={dict.platforms} release={release} />
        <OpenSource dict={dict.opensource} stats={stats} numberLocale={numberLocale} />
        <Faq dict={dict.faq} />
        <Cta dict={dict.cta} />
      </main>
      <Footer locale={locale} dict={dict.footer} nav={dict.nav} />
      <PageEffects />
      <script
        type="application/ld+json"
        // 转义 "<"，防止 JSON 内容提前闭合 script 标签。
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd).replace(/</g, "\\u003c") }}
      />
    </>
  );
}
