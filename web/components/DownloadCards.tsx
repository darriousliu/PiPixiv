"use client";

import { usePlatform, type Platform } from "@/lib/platform";
import { AndroidIcon, AppleIcon, LinuxIcon, WindowsIcon } from "./BrandIcons";
import styles from "./Download.module.css";

export interface DownloadAction {
  variant: "primary" | "soft" | "link";
  label: string;
  href: string;
  size?: string;
}

export interface DownloadCard {
  platform: Platform;
  title: string;
  requirement: string;
  actions: DownloadAction[];
}

const LOGOS = {
  android: AndroidIcon,
  ios: AppleIcon,
  windows: WindowsIcon,
  macos: AppleIcon,
  linux: LinuxIcon,
} satisfies Record<Platform, unknown>;

/** 链接跳转到常见问题条目（<details>）时将其展开。 */
function openTarget(href: string) {
  if (!href.startsWith("#")) return;
  const target = document.getElementById(href.slice(1));
  if (target instanceof HTMLDetailsElement) target.open = true;
}

/** 各平台的下载卡片；识别出访客的平台后高亮对应卡片。 */
export function DownloadCards({ cards, recommendedLabel }: { cards: DownloadCard[]; recommendedLabel: string }) {
  const current = usePlatform();

  return (
    <div className={styles.grid}>
      {cards.map((card) => {
        const Logo = LOGOS[card.platform];
        const recommended = card.platform === current;
        return (
          <article
            key={card.platform}
            className={styles.card}
            data-platform={card.platform}
            data-recommended={recommended || undefined}
            data-reveal
          >
            <div className={styles.top}>
              <Logo className={styles.logo} />
              {recommended && <span className={styles.badge}>{recommendedLabel}</span>}
            </div>
            <h3>{card.title}</h3>
            <p className={styles.req}>{card.requirement}</p>
            <div className={styles.actions}>
              {card.actions.map((action) =>
                action.variant === "link" ? (
                  <a
                    key={action.label}
                    className={styles.link}
                    href={action.href}
                    onClick={() => openTarget(action.href)}
                  >
                    {action.label}
                  </a>
                ) : (
                  <a
                    key={action.label}
                    className={`btn btn-block ${action.variant === "primary" ? "btn-primary" : "btn-soft"}`}
                    href={action.href}
                  >
                    <span>{action.label}</span>
                    {action.size && <small>{action.size}</small>}
                  </a>
                ),
              )}
            </div>
          </article>
        );
      })}
    </div>
  );
}
