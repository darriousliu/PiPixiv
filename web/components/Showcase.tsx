"use client";

import Image, { type StaticImageData } from "next/image";
import { useEffect, useRef, useState, type CSSProperties, type KeyboardEvent, type ReactNode } from "react";
import { Icon } from "./Icon";
import styles from "./Showcase.module.css";

/** 每个标签自动切换前的停留时长（进度条也使用这个时长）。 */
const DURATION = 7000;

export interface ShowcaseTab {
  id: string;
  icon: string;
  title: string;
  body: string;
  image: StaticImageData;
  alt: string;
}

/**
 * 带标签的截图展示，区块可见时自动轮播。
 * `variant` 决定设备边框：iPhone 或桌面窗口。
 */
export function Showcase({
  id,
  tabs,
  tabsLabel,
  variant,
  intro,
  footer,
}: {
  id: string;
  tabs: ShowcaseTab[];
  tabsLabel: string;
  variant: "phone" | "window";
  /** 显示在标签上方的可选标题（手机样式）。 */
  intro?: ReactNode;
  footer?: ReactNode;
}) {
  const [index, setIndex] = useState(0);
  const [auto, setAuto] = useState(false);
  const [visible, setVisible] = useState(false);
  const [paused, setPaused] = useState(false);
  const [docHidden, setDocHidden] = useState(false);
  const stageRef = useRef<HTMLDivElement>(null);
  const tabRefs = useRef<(HTMLButtonElement | null)[]>([]);
  /** 当前标签已显示的时长，暂停期间保留。 */
  const clock = useRef({ index: 0, elapsed: 0 });

  useEffect(() => {
    const el = stageRef.current;
    if (!el) return;
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)");
    const onMotionChange = () => setAuto(!reduced.matches);
    const io = new IntersectionObserver(([entry]) => setVisible(entry.isIntersecting), {
      threshold: 0.35,
    });
    const onVisibility = () => setDocHidden(document.hidden);
    io.observe(el);
    document.addEventListener("visibilitychange", onVisibility);
    reduced.addEventListener("change", onMotionChange);
    // 水合完成且允许动画时才开启自动轮播。
    const id = requestAnimationFrame(() => {
      onVisibility();
      onMotionChange();
    });
    return () => {
      cancelAnimationFrame(id);
      io.disconnect();
      document.removeEventListener("visibilitychange", onVisibility);
      reduced.removeEventListener("change", onMotionChange);
    };
  }, []);

  const playing = auto && visible && !paused && !docHidden;

  // 自动轮播由计时器驱动，而不依赖进度条的 `animationend`：一旦 CSS 动画提前结束
  //（页面打开期间开启了减弱动态效果，或页面被节流后动画时钟滞后），每个新标签都会
  // 立即再次触发 `animationend`，导致截图逐帧来回切换。
  useEffect(() => {
    if (!playing) return;
    if (clock.current.index !== index) clock.current = { index, elapsed: 0 };
    const startedAt = performance.now();
    const timer = window.setTimeout(
      () => setIndex((i) => (i + 1) % tabs.length),
      Math.max(0, DURATION - clock.current.elapsed),
    );
    return () => {
      window.clearTimeout(timer);
      clock.current.elapsed += performance.now() - startedAt;
    };
  }, [playing, index, tabs.length]);

  const select = (i: number, focus = false) => {
    const next = (i + tabs.length) % tabs.length;
    setIndex(next);
    if (focus) tabRefs.current[next]?.focus();
  };

  const onKeyDown = (e: KeyboardEvent<HTMLButtonElement>) => {
    const step = { ArrowDown: 1, ArrowRight: 1, ArrowUp: -1, ArrowLeft: -1 }[e.key];
    if (step) {
      e.preventDefault();
      select(index + step, true);
    } else if (e.key === "Home") {
      e.preventDefault();
      select(0, true);
    } else if (e.key === "End") {
      e.preventDefault();
      select(tabs.length - 1, true);
    }
  };

  return (
    <div
      ref={stageRef}
      className={styles.stage}
      style={{ "--dur": `${DURATION}ms` } as CSSProperties}
      data-variant={variant}
      data-reveal
      data-auto={auto || undefined}
      data-playing={playing || undefined}
      onPointerEnter={() => setPaused(true)}
      onPointerLeave={() => setPaused(false)}
      onFocus={() => setPaused(true)}
      onBlur={() => setPaused(false)}
    >
      <div className={styles.side}>
      {intro}
      <div className={styles.tabs} role="tablist" aria-label={tabsLabel} aria-orientation="vertical">
        {tabs.map((tab, i) => (
          <button
            key={tab.id}
            ref={(el) => {
              tabRefs.current[i] = el;
            }}
            id={`${id}-tab-${tab.id}`}
            type="button"
            role="tab"
            className={styles.tab}
            aria-selected={i === index}
            aria-controls={`${id}-panel-${tab.id}`}
            tabIndex={i === index ? 0 : -1}
            onClick={() => select(i)}
            onKeyDown={onKeyDown}
          >
            <Icon name={tab.icon} className={styles.tabIcon} />
            <span className={styles.tabText}>
              <b>{tab.title}</b>
              <small>{tab.body}</small>
            </span>
            {/* 仅用于显示；当前标签变化时通过 key 重新开始进度条 */}
            <span key={i === index ? `on-${index}` : "off"} className={styles.progress} aria-hidden="true" />
          </button>
        ))}
      </div>
      </div>

      <div className={styles.window}>
        {tabs.map((tab, i) => (
          <figure
            key={tab.id}
            id={`${id}-panel-${tab.id}`}
            role="tabpanel"
            aria-labelledby={`${id}-tab-${tab.id}`}
            aria-hidden={i !== index}
            className={styles.shot}
            data-active={i === index || undefined}
          >
            <Image
              src={tab.image}
              alt={tab.alt}
              sizes={variant === "phone" ? "(max-width: 980px) 70vw, 340px" : "(max-width: 980px) 94vw, 780px"}
              placeholder="blur"
            />
          </figure>
        ))}
      </div>

      {footer && <div className={styles.footer}>{footer}</div>}
    </div>
  );
}
