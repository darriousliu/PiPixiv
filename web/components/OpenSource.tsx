import type { Dictionary } from "@/i18n/dictionaries";
import type { RepoStats } from "@/lib/github";
import { siteConfig } from "@/lib/site";
import { ComposeMultiplatformLogo, GitHubIcon, KotlinLogo, MaterialDesignLogo, TelegramIcon } from "./BrandIcons";
import { CountUp } from "./CountUp";
import styles from "./OpenSource.module.css";

const TECH = [
  { name: "Kotlin", Logo: KotlinLogo },
  { name: "Compose Multiplatform", Logo: ComposeMultiplatformLogo },
  { name: "Material 3 Expressive", Logo: MaterialDesignLogo },
] as const;

export function OpenSource({
  dict,
  stats,
  numberLocale,
}: {
  dict: Dictionary["opensource"];
  stats: RepoStats | null;
  numberLocale: string;
}) {
  return (
    <section className="section" id="opensource">
      <div className="container">
        <div className={styles.panel} data-reveal>
          <div className={styles.glow} aria-hidden="true" />
          <div className={styles.copy}>
            <p className={`eyebrow ${styles.eyebrow}`}>{dict.eyebrow}</p>
            <h2 className="section-title">
              {dict.title[0]}
              <br />
              {dict.title[1]}
            </h2>
            <p className={styles.body}>{dict.body}</p>
            <div className={styles.actions}>
              <a className="btn btn-light" href={siteConfig.links.github} target="_blank" rel="noopener">
                <GitHubIcon />
                {dict.github}
              </a>
              <a className="btn btn-glass" href={siteConfig.links.telegram} target="_blank" rel="noopener">
                <TelegramIcon />
                {dict.telegram}
              </a>
            </div>
          </div>

          <dl className={styles.stats}>
            <div className={styles.stat}>
              <dt>{dict.stats.stars}</dt>
              <dd>{stats ? <CountUp value={stats.stars} locale={numberLocale} /> : "—"}</dd>
            </div>
            <div className={styles.stat}>
              <dt>{dict.stats.downloads}</dt>
              <dd>{stats ? <CountUp value={stats.downloads} locale={numberLocale} /> : "—"}</dd>
            </div>
            <div className={styles.stat}>
              <dt>{dict.stats.platforms}</dt>
              <dd>{siteConfig.platformCount}</dd>
            </div>
            <div className={styles.stat}>
              <dt>{dict.stats.license}</dt>
              <dd className={styles.small}>{siteConfig.license}</dd>
            </div>
          </dl>
        </div>

        <ul className={styles.tech} aria-label={dict.techLabel} data-reveal>
          {TECH.map(({ name, Logo }) => (
            <li key={name}>
              <Logo className={styles.techLogo} />
              {name}
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
