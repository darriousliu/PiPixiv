"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState, useSyncExternalStore } from "react";
import { LOCALE_COOKIE, type Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/dictionaries";
import logo from "@/public/logo.svg";
import { GitHubIcon, TelegramIcon } from "./BrandIcons";
import { Icon } from "./Icon";
import styles from "./Nav.module.css";

const SECTIONS = ["features", "showcase", "download", "opensource", "faq"] as const;
const THEME_KEY = "pipixiv-theme";
const THEME_EVENT = "pipixiv-theme-change";

/* ---------- 滚动位置 ---------- */
function subscribeScroll(cb: () => void) {
  window.addEventListener("scroll", cb, { passive: true });
  return () => window.removeEventListener("scroll", cb);
}
const useScrolled = () =>
  useSyncExternalStore(subscribeScroll, () => window.scrollY > 12, () => false);

/* ---------- 主题 ---------- */
type Theme = "light" | "dark";
function subscribeTheme(cb: () => void) {
  const mq = window.matchMedia("(prefers-color-scheme: dark)");
  mq.addEventListener("change", cb);
  window.addEventListener(THEME_EVENT, cb);
  return () => {
    mq.removeEventListener("change", cb);
    window.removeEventListener(THEME_EVENT, cb);
  };
}
function readTheme(): Theme {
  const explicit = document.documentElement.getAttribute("data-theme");
  if (explicit === "light" || explicit === "dark") return explicit;
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}
const useTheme = () => useSyncExternalStore(subscribeTheme, readTheme, () => "light" as Theme);

function setTheme(theme: Theme) {
  document.documentElement.setAttribute("data-theme", theme);
  try {
    localStorage.setItem(THEME_KEY, theme);
  } catch {
    /* 存储不可用 */
  }
  window.dispatchEvent(new Event(THEME_EVENT));
}

/** 访客再次打开 `/` 时，proxy.ts 会按这里记住的语言跳转。 */
function rememberLocale(locale: Locale) {
  document.cookie = `${LOCALE_COOKIE}=${locale}; path=/; max-age=31536000; samesite=lax`;
}

/* ---------- 当前区块 ---------- */
function useActiveSection() {
  const [active, setActive] = useState<string | null>(null);
  useEffect(() => {
    const targets = ["top", ...SECTIONS]
      .map((id) => document.getElementById(id))
      .filter((el): el is HTMLElement => !!el);
    const io = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) setActive(entry.target.id === "top" ? null : entry.target.id);
        }
      },
      { rootMargin: "-45% 0px -50% 0px" },
    );
    targets.forEach((el) => io.observe(el));
    return () => io.disconnect();
  }, []);
  return active;
}

interface LocaleOption {
  locale: Locale;
  label: string;
  short: string;
}

export function Nav({
  locale,
  dict,
  stars,
  localeOptions,
  githubUrl,
  telegramUrl,
}: {
  locale: Locale;
  dict: Dictionary["nav"];
  stars: string | null;
  localeOptions: LocaleOption[];
  githubUrl: string;
  telegramUrl: string;
}) {
  const scrolled = useScrolled();
  const theme = useTheme();
  const active = useActiveSection();
  const [menuOpen, setMenuOpen] = useState(false);
  const [langOpen, setLangOpen] = useState(false);
  const navRef = useRef<HTMLElement>(null);
  const router = useRouter();

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setMenuOpen(false);
        setLangOpen(false);
      }
    };
    const onClick = (e: MouseEvent) => {
      if (!navRef.current?.contains(e.target as Node)) {
        setMenuOpen(false);
        setLangOpen(false);
      }
    };
    document.addEventListener("keydown", onKey);
    document.addEventListener("click", onClick);
    return () => {
      document.removeEventListener("keydown", onKey);
      document.removeEventListener("click", onClick);
    };
  }, []);

  const switchLocale = (target: Locale) => {
    rememberLocale(target);
    // 不同语言使用不同的根布局，因此这里会整页加载。
    router.push(`/${target}${window.location.hash}`);
  };

  const current = localeOptions.find((o) => o.locale === locale);

  return (
    <header
      ref={navRef}
      className={styles.nav}
      data-scrolled={scrolled || undefined}
      data-open={menuOpen || undefined}
    >
      <nav className={styles.inner} aria-label={dict.label}>
        <a className={styles.brand} href="#top" aria-label={dict.home}>
          <Image src={logo} alt="" width={30} height={30} />
          <span>PiPixiv</span>
        </a>

        <ul className={styles.links} id="nav-links">
          {SECTIONS.map((id) => (
            <li key={id}>
              <a
                href={`#${id}`}
                data-active={active === id || undefined}
                onClick={() => setMenuOpen(false)}
              >
                {dict.links[id]}
              </a>
            </li>
          ))}
        </ul>

        <div className={styles.actions}>
          <div className={styles.lang}>
            <button
              type="button"
              className={styles.iconBtn}
              aria-label={dict.language}
              aria-haspopup="menu"
              aria-expanded={langOpen}
              onClick={() => setLangOpen((v) => !v)}
            >
              <Icon name="translate" />
              <span className={styles.langShort}>{current?.short}</span>
            </button>
            {langOpen && (
              <ul className={styles.langMenu} role="menu">
                {localeOptions.map((o) => (
                  <li key={o.locale} role="none">
                    <a
                      role="menuitemradio"
                      aria-checked={o.locale === locale}
                      href={`/${o.locale}`}
                      hrefLang={o.locale}
                      lang={o.locale}
                      onClick={(e) => {
                        e.preventDefault();
                        switchLocale(o.locale);
                      }}
                    >
                      <span>{o.label}</span>
                      {o.locale === locale && <Icon name="check" />}
                    </a>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <button
            type="button"
            className={styles.iconBtn}
            aria-label={theme === "dark" ? dict.toLight : dict.toDark}
            onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
          >
            <Icon name={theme === "dark" ? "light_mode" : "dark_mode"} />
          </button>

          <a
            className={styles.iconBtn}
            href={telegramUrl}
            target="_blank"
            rel="noopener"
            aria-label={dict.telegramLabel}
            title={dict.telegramLabel}
          >
            <TelegramIcon />
          </a>

          <a
            className={styles.ghBtn}
            href={githubUrl}
            target="_blank"
            rel="noopener"
            aria-label={dict.starLabel}
          >
            <GitHubIcon className={styles.ghIcon} />
            <span className={styles.ghLabel}>{dict.star}</span>
            {stars && <span className={styles.ghCount}>{stars}</span>}
          </a>

          <a className={`btn btn-primary btn-sm ${styles.cta}`} href="#download">
            {dict.download}
          </a>

          <button
            type="button"
            className={`${styles.iconBtn} ${styles.menuBtn}`}
            aria-expanded={menuOpen}
            aria-controls="nav-links"
            aria-label={menuOpen ? dict.closeMenu : dict.openMenu}
            onClick={() => setMenuOpen((v) => !v)}
          >
            <Icon name={menuOpen ? "close" : "menu"} />
          </button>
        </div>
      </nav>
    </header>
  );
}
