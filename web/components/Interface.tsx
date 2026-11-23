import type { Dictionary } from "@/i18n/dictionaries";
import type { ScreenshotSet } from "@/lib/screenshots";
import { Showcase, type ShowcaseTab } from "./Showcase";
import styles from "./Showcase.module.css";

const TABS = [
  { id: "ranking", icon: "leaderboard" },
  { id: "detail", icon: "image" },
  { id: "novel", icon: "menu_book" },
  { id: "settings", icon: "blur_on" },
  { id: "network", icon: "dark_mode" },
] as const;

/** 带 iPhone 边框的手机截图，可通过标签切换。 */
export function Interface({
  dict,
  screens,
  phones,
}: {
  dict: Dictionary["showcase"];
  screens: Dictionary["screens"]["phones"];
  phones: ScreenshotSet["phones"];
}) {
  const tabs: ShowcaseTab[] = TABS.map(({ id, icon }) => ({
    id,
    icon,
    title: dict.tabs[id].title,
    body: dict.tabs[id].body,
    image: phones[id],
    alt: screens[id].alt,
  }));

  return (
    <section className="section" id="showcase">
      <div className="container">
        <Showcase
          id="interface"
          variant="phone"
          tabs={tabs}
          tabsLabel={dict.tabsLabel}
          intro={
            <header className={styles.intro}>
              <p className="eyebrow">{dict.eyebrow}</p>
              <h2 className="section-title">{dict.title}</h2>
              <p className={styles.lead}>{dict.lead}</p>
              <ul className={styles.pills}>
                {dict.highlights.map((h) => (
                  <li key={h}>{h}</li>
                ))}
              </ul>
            </header>
          }
        />
      </div>
    </section>
  );
}
