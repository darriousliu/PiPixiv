import type { Dictionary } from "@/i18n/dictionaries";
import { richText } from "@/lib/format";
import styles from "./Faq.module.css";

export function Faq({ dict }: { dict: Dictionary["faq"] }) {
  return (
    <section className="section" id="faq">
      <div className="container container-narrow">
        <header className="section-head" data-reveal>
          <p className="eyebrow">{dict.eyebrow}</p>
          <h2 className="section-title">{dict.title}</h2>
        </header>
        <div className={styles.list}>
          {dict.items.map((item) => (
            <details key={item.id} id={`faq-${item.id}`} className={styles.item} data-reveal>
              <summary>
                {item.q}
                <span className={styles.toggle} aria-hidden="true" />
              </summary>
              <div className={styles.body}>
                <p>{richText(item.a)}</p>
              </div>
            </details>
          ))}
        </div>
      </div>
    </section>
  );
}
