import Image from "next/image";
import type { Dictionary } from "@/i18n/dictionaries";
import { siteConfig } from "@/lib/site";
import logo from "@/public/logo.svg";
import { Icon } from "./Icon";
import styles from "./Cta.module.css";

export function Cta({ dict }: { dict: Dictionary["cta"] }) {
  return (
    <section className={`section ${styles.section}`}>
      <div className="container">
        <div className={styles.card} data-reveal data-pause-offscreen>
          <div className={styles.aurora} aria-hidden="true" />
          <Image className={styles.icon} src={logo} alt="" width={72} height={72} />
          <h2 className="section-title">{dict.title}</h2>
          <p>{dict.body}</p>
          <div className={styles.row}>
            <a className="btn btn-light btn-lg" href="#download">
              <Icon name="download" />
              {dict.download}
            </a>
            <a className="btn btn-glass btn-lg" href={siteConfig.links.github} target="_blank" rel="noopener">
              <Icon name="star" />
              {dict.star}
            </a>
          </div>
        </div>
      </div>
    </section>
  );
}
