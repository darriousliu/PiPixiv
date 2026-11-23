import { cacheLife } from "next/cache";
import Image from "next/image";
import type { Locale } from "@/i18n/config";
import type { Dictionary } from "@/i18n/dictionaries";
import { siteConfig } from "@/lib/site";
import logo from "@/public/logo.svg";
import styles from "./Footer.module.css";

async function currentYear() {
  "use cache";
  cacheLife("days");
  return new Date().getFullYear();
}

export async function Footer({ locale, dict, nav }: { locale: Locale; dict: Dictionary["footer"]; nav: Dictionary["nav"] }) {
  const year = await currentYear();
  const { links } = siteConfig;

  return (
    <footer className={styles.footer}>
      <div className={`container ${styles.inner}`}>
        <div className={styles.brandCol}>
          <a className={styles.brand} href="#top">
            <Image src={logo} alt="" width={30} height={30} />
            <span>PiPixiv</span>
          </a>
          <p>{dict.disclaimer}</p>
        </div>
        <nav className={styles.col} aria-label={dict.pages}>
          <h4>{dict.pages}</h4>
          <a href="#features">{nav.links.features}</a>
          <a href="#showcase">{nav.links.showcase}</a>
          <a href="#download">{nav.links.download}</a>
          <a href="#faq">{nav.links.faq}</a>
        </nav>
        <nav className={styles.col} aria-label={dict.project}>
          <h4>{dict.project}</h4>
          <a href={links.github} target="_blank" rel="noopener">
            {dict.repo}
          </a>
          <a href={links.releases} target="_blank" rel="noopener">
            {dict.releases}
          </a>
          <a href={links.issues} target="_blank" rel="noopener">
            {dict.issues}
          </a>
        </nav>
        <nav className={styles.col} aria-label={dict.community}>
          <h4>{dict.community}</h4>
          <a href={links.telegram} target="_blank" rel="noopener">
            {dict.telegram}
          </a>
          <a href={siteConfig.readme[locale]} target="_blank" rel="noopener">
            {dict.readme}
          </a>
        </nav>
      </div>
      <div className={`container ${styles.bottom}`}>
        <span>
          © {year} PiPixiv · {dict.license}
        </span>
        <span>{dict.builtWith}</span>
      </div>
    </footer>
  );
}
