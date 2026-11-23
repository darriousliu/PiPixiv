import { localeMeta, type Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/dictionaries";
import { fill, formatBytes } from "@/lib/format";
import type { AssetKey, ReleaseInfo } from "@/lib/github";
import { siteConfig } from "@/lib/site";
import { DownloadCards, type DownloadCard } from "./DownloadCards";
import styles from "./Download.module.css";

export function Download({
  locale,
  dict,
  platforms,
  release,
}: {
  locale: Locale;
  dict: Dictionary["download"];
  platforms: Dictionary["platforms"];
  release: ReleaseInfo | null;
}) {
  // 没有 Release 数据时，所有按钮都回退到 Releases 页面。
  const asset = (key: AssetKey) => {
    const a = release?.assets[key];
    return { href: a?.url ?? siteConfig.links.latestRelease, size: a ? formatBytes(a.size) : undefined };
  };

  const cards: DownloadCard[] = [
    {
      platform: "android",
      title: platforms.android,
      requirement: dict.requirements.android,
      actions: [
        { variant: "primary", label: dict.actions.androidDefault, ...asset("android-default") },
      ],
    },
    {
      platform: "ios",
      title: platforms.ios,
      requirement: dict.requirements.ios,
      actions: [
        { variant: "primary", label: dict.actions.ios, ...asset("ios") },
        { variant: "link", label: dict.actions.iosHelp, href: "#faq-ios" },
      ],
    },
    {
      platform: "windows",
      title: platforms.windows,
      requirement: dict.requirements.windows,
      actions: [{ variant: "primary", label: dict.actions.windows, ...asset("windows") }],
    },
    {
      platform: "macos",
      title: platforms.macos,
      requirement: dict.requirements.macos,
      actions: [{ variant: "primary", label: dict.actions.macos, ...asset("macos") }],
    },
    {
      platform: "linux",
      title: platforms.linux,
      requirement: dict.requirements.linux,
      actions: [{ variant: "primary", label: dict.actions.linux, ...asset("linux") }],
    },
  ];

  const date = release
    ? new Intl.DateTimeFormat(localeMeta[locale].htmlLang, { dateStyle: "long", timeZone: "UTC" }).format(
        new Date(release.publishedAt),
      )
    : null;

  return (
    <section className="section" id="download">
      <div className="container">
        <header className="section-head" data-reveal>
          <p className="eyebrow">{dict.eyebrow}</p>
          <h2 className="section-title">{dict.title}</h2>
          <p className="section-lead">{dict.lead}</p>
          <p className={styles.meta}>
            {release && (
              <>
                <span>
                  {dict.latest} <b>{release.tag}</b>
                </span>
                {date && (
                  <>
                    <span className={styles.dot} aria-hidden="true" />
                    <span>{fill(dict.released, { date })}</span>
                  </>
                )}
                <span className={styles.dot} aria-hidden="true" />
              </>
            )}
            <a href={release?.url ?? siteConfig.links.releases} target="_blank" rel="noopener">
              {dict.changelog}
            </a>
          </p>
        </header>

        <DownloadCards cards={cards} recommendedLabel={dict.recommended} />
      </div>
    </section>
  );
}
