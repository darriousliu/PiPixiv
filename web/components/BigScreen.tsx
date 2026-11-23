import type { Dictionary } from "@/i18n/dictionaries";
import type { ScreenshotSet } from "@/lib/screenshots";
import { Icon } from "./Icon";
import { Showcase, type ShowcaseTab } from "./Showcase";
import styles from "./Showcase.module.css";

const TABS = [
  { id: "search", icon: "search" },
  { id: "detail", icon: "image" },
  { id: "novel", icon: "menu_book" },
  { id: "ai", icon: "translate" },
] as const;

/**
 * 带窗口边框的桌面端 / 平板截图。
 * 仅当提供了 `ScreenshotSet.desktop` 时才渲染（见 lib/screenshots.ts）。
 */
export function BigScreen({
  dict,
  screens,
  desktop,
}: {
  dict: Dictionary["bigScreen"];
  screens: Dictionary["screens"]["desktop"];
  desktop: NonNullable<ScreenshotSet["desktop"]>;
}) {
  const tabs: ShowcaseTab[] = TABS.map(({ id, icon }) => ({
    id,
    icon,
    title: dict.tabs[id].title,
    body: dict.tabs[id].body,
    image: desktop[id],
    alt: screens[id],
  }));

  return (
    <section className="section" id="big-screen">
      <div className="container">
        <header className="section-head" data-reveal>
          <p className="eyebrow">{dict.eyebrow}</p>
          <h2 className="section-title">{dict.title}</h2>
          <p className="section-lead">{dict.lead}</p>
        </header>
        <Showcase
          id="big-screen"
          variant="window"
          tabs={tabs}
          tabsLabel={dict.tabsLabel}
          footer={
            <ul className={styles.keys} aria-label={dict.keysLabel}>
              <li>
                <kbd>R</kbd>
                {dict.keys.refresh}
              </li>
              <li>
                <kbd>↑</kbd>
                <kbd>↓</kbd>
                {dict.keys.scroll}
              </li>
              <li>
                <kbd>Esc</kbd>
                {dict.keys.back}
              </li>
              <li>
                <Icon name="dark_mode" />
                {dict.keys.theme}
              </li>
            </ul>
          }
        />
      </div>
    </section>
  );
}
