import type { CSSProperties, ReactNode } from "react";
import type { Dictionary } from "@/i18n/dictionaries";
import { Icon } from "./Icon";
import { TranslateDemo } from "./TranslateDemo";
import styles from "./Features.module.css";

function Card({
  icon,
  title,
  body,
  span,
  accent,
  children,
}: {
  icon: string;
  title: string;
  body: string;
  span: 4 | 5 | 6 | 7;
  accent?: boolean;
  children?: ReactNode;
}) {
  return (
    <article className={`${styles.card} ${styles[`span${span}`]}`} data-reveal data-spot data-accent={accent || undefined}>
      <div className={styles.cardIcon} data-grad={accent || undefined}>
        <Icon name={icon} />
      </div>
      <h3>{title}</h3>
      <p>{body}</p>
      {children && <div className={styles.visual}>{children}</div>}
    </article>
  );
}

export function Features({ dict }: { dict: Dictionary["features"] }) {
  return (
    <section className="section" id="features">
      <div className="container">
        <header className="section-head" data-reveal>
          <p className="eyebrow">{dict.eyebrow}</p>
          <h2 className="section-title">
            {dict.title[0]}
            <br />
            {dict.title[1]}
          </h2>
          <p className="section-lead">{dict.lead}</p>
        </header>

        <div className={styles.bento} data-spotlight>
          <Card icon="leaderboard" title={dict.ranking.title} body={dict.ranking.body} span={7}>
            <div className={styles.chips} aria-hidden="true">
              {dict.ranking.chips.map((chip, i) => (
                <span key={chip} className={styles.chip} data-on={i === 0 || undefined}>
                  {chip}
                </span>
              ))}
            </div>
          </Card>

          <Card icon="photo_library" title={dict.illust.title} body={dict.illust.body} span={5}>
            <ul className={styles.miniStats} aria-hidden="true">
              {dict.illust.stats.map((s) => (
                <li key={s.value}>
                  <b>{s.value}</b>
                  <span>{s.label}</span>
                </li>
              ))}
            </ul>
          </Card>

          <Card icon="translate" title={dict.ai.title} body={dict.ai.body} span={7} accent>
            <TranslateDemo
              samples={dict.ai.samples}
              labels={{ translated: dict.ai.translated, original: dict.ai.original, badge: dict.ai.badge }}
            />
          </Card>

          <Card icon="menu_book" title={dict.reader.title} body={dict.reader.body} span={5}>
            <div className={styles.reader} aria-hidden="true">
              <div className={styles.readerRow}>
                <span>{dict.reader.fontSize}</span>
                <span className={styles.track} style={{ "--v": 0.58 } as CSSProperties}>
                  <i />
                </span>
                <span className={styles.readerValue}>18</span>
              </div>
              <div className={styles.readerRow}>
                <span>{dict.reader.lineHeight}</span>
                <span className={styles.track} style={{ "--v": 0.42 } as CSSProperties}>
                  <i />
                </span>
                <span className={styles.readerValue}>1.6</span>
              </div>
              <div className={styles.readerProgress}>
                <span>{dict.reader.progress}</span>
                <b>62%</b>
              </div>
            </div>
          </Card>

          <Card icon="switch_account" title={dict.accounts.title} body={dict.accounts.body} span={4}>
            <div className={styles.avatars} aria-hidden="true">
              {[198, 262, 330].map((h, i) => (
                <span key={h} style={{ "--h": h } as CSSProperties}>
                  {String.fromCharCode(65 + i)}
                </span>
              ))}
              <span className={styles.avatarAdd}>+</span>
            </div>
          </Card>

          <Card icon="network_check" title={dict.network.title} body={dict.network.body} span={4}>
            <ul className={styles.checks} aria-hidden="true">
              {dict.network.checks.map((c) => (
                <li key={c}>
                  <Icon name="check_circle" />
                  {c}
                </li>
              ))}
            </ul>
          </Card>

          <Card icon="shield" title={dict.privacy.title} body={dict.privacy.body} span={4}>
            <div className={styles.toggles} aria-hidden="true">
              {dict.privacy.toggles.map((label, i) => (
                <div key={label}>
                  <span>{label}</span>
                  <i className={styles.toggle} data-on={i === 1 || undefined} />
                </div>
              ))}
            </div>
          </Card>

          <Card icon="search" title={dict.search.title} body={dict.search.body} span={6}>
            <div className={styles.search} aria-hidden="true">
              <div className={styles.searchField}>
                <Icon name="search" />
                <span className={styles.searchQuery} lang="ja">
                  {dict.search.query}
                </span>
                <span className={styles.searchSort}>{dict.search.sort} ▾</span>
              </div>
              <div className={styles.searchTags} lang="ja">
                {dict.search.tags.map((tag) => (
                  <span key={tag}>{tag}</span>
                ))}
              </div>
            </div>
          </Card>

          <Card icon="settings_backup_restore" title={dict.data.title} body={dict.data.body} span={6}>
            <div className={styles.pills} aria-hidden="true">
              {dict.data.pills.map((pill, i) => (
                <span key={pill}>
                  <Icon name={["history", "download", "settings_backup_restore"][i] ?? "history"} />
                  {pill}
                </span>
              ))}
            </div>
          </Card>
        </div>
      </div>
    </section>
  );
}
