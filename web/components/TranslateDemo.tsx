"use client";

import { useEffect, useRef, useState } from "react";
import { Icon } from "./Icon";
import styles from "./Features.module.css";

interface Sample {
  source: string;
  output: string;
}

/** 像流式响应一样逐字打出译文，仅在可见时运行。 */
export function TranslateDemo({
  samples,
  labels,
}: {
  samples: Sample[];
  labels: { translated: string; original: string; badge: string };
}) {
  const ref = useRef<HTMLDivElement>(null);
  const [index, setIndex] = useState(0);
  // 服务端渲染及无 JS 时显示完整的第一句。
  const [typed, setTyped] = useState<number | null>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

    let timer = 0;
    let running = false;
    let i = 0;

    const play = () => {
      const text = samples[i].output;
      let n = 0;
      setIndex(i);
      setTyped(0);
      const step = () => {
        if (!running) return;
        n = Math.min(text.length, n + 1 + (Math.random() < 0.3 ? 1 : 0));
        setTyped(n);
        if (n < text.length) {
          timer = window.setTimeout(step, 45 + Math.random() * 70);
        } else {
          timer = window.setTimeout(() => {
            i = (i + 1) % samples.length;
            play();
          }, 2600);
        }
      };
      timer = window.setTimeout(step, 450);
    };

    const io = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !running) {
          running = true;
          play();
        } else if (!entry.isIntersecting && running) {
          running = false;
          window.clearTimeout(timer);
        }
      },
      { threshold: 0.4 },
    );
    io.observe(el);
    return () => {
      running = false;
      window.clearTimeout(timer);
      io.disconnect();
    };
  }, [samples]);

  const sample = samples[index];
  const output = typed === null ? sample.output : sample.output.slice(0, typed);

  return (
    <div ref={ref} className={styles.translate} aria-hidden="true">
      <div className={styles.tdHead}>
        <span className={styles.tdTab} data-on>
          {labels.translated}
        </span>
        <span className={styles.tdTab}>{labels.original}</span>
        <span className={styles.tdBadge}>
          <Icon name="auto_awesome" />
          {labels.badge}
        </span>
      </div>
      <p className={styles.tdSource} lang="ja">
        {sample.source}
      </p>
      <p className={styles.tdOutput}>
        {output}
        <span className={styles.caret} />
      </p>
    </div>
  );
}
