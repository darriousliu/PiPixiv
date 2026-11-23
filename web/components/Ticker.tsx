import type { Dictionary } from "@/i18n/dictionaries";
import styles from "./Ticker.module.css";

/** 两行反向滚动的功能名称（纯 CSS 实现）。 */
export function Ticker({ dict }: { dict: Dictionary["ticker"] }) {
  return (
    <section className={styles.ticker} aria-label={dict.label} data-pause-offscreen>
      {dict.rows.map((row, i) => (
        <div key={i} className={styles.row} data-reverse={i % 2 === 1 || undefined}>
          <ul className={styles.track}>
            {row.map((item) => (
              <li key={item}>{item}</li>
            ))}
            {/* 复制一份以实现无缝循环；对辅助技术隐藏，减弱动态效果时也隐藏。 */}
            {row.map((item) => (
              <li key={`dup-${item}`} className={styles.dup} aria-hidden="true">
                {item}
              </li>
            ))}
          </ul>
        </div>
      ))}
    </section>
  );
}
