"use client";

import { useEffect } from "react";

/**
 * 页面级、不参与渲染的效果：
 * - `[data-reveal]` 元素滚动进入视口时渐显；
 * - `[data-pause-offscreen]` 区块移出屏幕时暂停其 CSS 循环动画；
 * - `[data-spotlight]` 容器在指针下方的 `[data-spot]` 卡片上显示光斑。
 */
export function PageEffects() {
  useEffect(() => {
    const cleanups: (() => void)[] = [];
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    // 滚动渐显
    const revealEls = Array.from(document.querySelectorAll<HTMLElement>("[data-reveal]"));
    const finish = (el: HTMLElement) => {
      el.removeAttribute("data-reveal");
      el.style.removeProperty("--reveal-delay");
      el.setAttribute("data-revealed", "");
    };
    if (reduced) {
      revealEls.forEach(finish);
    } else {
      const io = new IntersectionObserver(
        (entries) => {
          let n = 0;
          for (const entry of entries) {
            if (!entry.isIntersecting) continue;
            const el = entry.target as HTMLElement;
            io.unobserve(el);
            el.style.setProperty("--reveal-delay", `${Math.min(n++, 5) * 0.07}s`);
            el.setAttribute("data-reveal", "in");
            // 渐显结束后移除过渡，让悬停过渡保持灵敏。
            const done = (e: TransitionEvent) => {
              if (e.target !== el || e.propertyName !== "transform") return;
              el.removeEventListener("transitionend", done);
              finish(el);
            };
            el.addEventListener("transitionend", done);
          }
        },
        { rootMargin: "0px 0px -8% 0px", threshold: 0.1 },
      );
      revealEls.forEach((el) => io.observe(el));
      cleanups.push(() => io.disconnect());
    }

    // 暂停屏幕外的动画
    const pauseIo = new IntersectionObserver((entries) => {
      for (const entry of entries) {
        entry.target.setAttribute("data-offscreen", String(!entry.isIntersecting));
      }
    });
    document.querySelectorAll("[data-pause-offscreen]").forEach((el) => pauseIo.observe(el));
    cleanups.push(() => pauseIo.disconnect());

    // 卡片光斑
    if (window.matchMedia("(hover: hover)").matches) {
      const onMove = (e: PointerEvent) => {
        const card = (e.target as Element).closest<HTMLElement>("[data-spot]");
        if (!card) return;
        const r = card.getBoundingClientRect();
        card.style.setProperty("--mx", `${e.clientX - r.left}px`);
        card.style.setProperty("--my", `${e.clientY - r.top}px`);
      };
      document.querySelectorAll<HTMLElement>("[data-spotlight]").forEach((el) => {
        el.addEventListener("pointermove", onMove, { passive: true });
        cleanups.push(() => el.removeEventListener("pointermove", onMove));
      });
    }

    return () => cleanups.forEach((fn) => fn());
  }, []);

  return null;
}
