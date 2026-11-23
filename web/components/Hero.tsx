import Image from "next/image";
import type { Dictionary } from "@/i18n/dictionaries";
import { fillNodes } from "@/lib/format";
import type { ReleaseInfo } from "@/lib/github";
import type { PhoneShotId } from "@/lib/screenshots";
import type { StaticImageData } from "next/image";
import { siteConfig } from "@/lib/site";
import { AndroidIcon, AppleIcon, GitHubIcon, LinuxIcon, WindowsIcon } from "./BrandIcons";
import { HeroTilt } from "./HeroTilt";
import { Icon } from "./Icon";
import { SmartDownloadHint } from "./SmartDownloadHint";
import styles from "./Hero.module.css";

interface HeroPhone {
  id: PhoneShotId;
  image: StaticImageData;
}

export function Hero({
  dict,
  release,
  phones,
}: {
  dict: Dictionary;
  release: ReleaseInfo | null;
  /** 左、中、右三台手机。 */
  phones: [HeroPhone, HeroPhone, HeroPhone];
}) {
  const t = dict.hero;
  const platforms = [
    { key: "android", Icon: AndroidIcon },
    { key: "ios", Icon: AppleIcon },
    { key: "windows", Icon: WindowsIcon },
    { key: "macos", Icon: AppleIcon },
    { key: "linux", Icon: LinuxIcon },
  ] as const;

  return (
    <section className={styles.hero} id="top" data-pause-offscreen>
      <div className={styles.bg} aria-hidden="true">
        <span className={`${styles.blob} ${styles.b1}`} />
        <span className={`${styles.blob} ${styles.b2}`} />
        <span className={`${styles.blob} ${styles.b3}`} />
        <div className={styles.lines} />
      </div>

      <div className={`container ${styles.inner}`}>
        <div className={styles.copy}>
          <a className={styles.pill} href={release?.url ?? siteConfig.links.latestRelease} target="_blank" rel="noopener">
            <span className={styles.pulse} aria-hidden="true" />
            <span>
              {release
                ? fillNodes(t.releasePill, { version: <b>{release.tag}</b> })
                : t.releasePillFallback}
            </span>
            <Icon name="arrow_forward" />
          </a>

          <h1 className={styles.title}>
            {t.title.map((line, i) => (
              <span key={i} className={styles.titleLine}>
                {fillNodes(line, { pixiv: <span className="grad-text">Pixiv</span> })}
              </span>
            ))}
          </h1>

          <p className={styles.lead}>{t.lead}</p>

          <div className={styles.cta}>
            <a className="btn btn-primary btn-lg" href="#download">
              <Icon name="download" />
              <span className={styles.btnStack}>
                <b>{t.download}</b>
                <SmartDownloadHint fallback={t.downloadHint} hints={dict.platformHints} />
              </span>
            </a>
            <a className="btn btn-soft btn-lg" href={siteConfig.links.github} target="_blank" rel="noopener">
              <GitHubIcon />
              <span>{t.source}</span>
            </a>
          </div>

          <ul className={styles.platforms} aria-label={t.platformsLabel}>
            {platforms.map(({ key, Icon: PlatformIcon }) => (
              <li key={key}>
                <PlatformIcon />
                {dict.platforms[key]}
              </li>
            ))}
          </ul>
        </div>

        <HeroTilt className={styles.visual}>
          <div className={styles.phones}>
            {(["side", "main", "side"] as const).map((role, i) => {
              const shot = phones[i];
              return (
                <div key={shot.id} className={styles.phone} data-role={role} data-index={i}>
                  <div className={styles.screen}>
                  <Image
                    src={shot.image}
                    alt={dict.screens.phones[shot.id].alt}
                    sizes={role === "main" ? "(max-width: 980px) 42vw, 280px" : "(max-width: 980px) 34vw, 230px"}
                    placeholder="blur"
                    preload={role === "main"}
                  />
                  </div>
                </div>
              );
            })}
          </div>
          <div className={`${styles.chip} ${styles.c1}`}>
            <Icon name="gif_box" />
            {t.chips.ugoira}
          </div>
          <div className={`${styles.chip} ${styles.c2}`}>
            <Icon name="translate" />
            {t.chips.translate}
          </div>
          <div className={`${styles.chip} ${styles.c3}`}>
            <Icon name="switch_account" />
            {t.chips.accounts}
          </div>
        </HeroTilt>
      </div>
    </section>
  );
}
