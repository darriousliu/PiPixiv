"use client";

import { useRef, type ReactNode } from "react";

/** 让内容朝指针方向倾斜（仅限精确指针设备，并遵循减弱动态效果设置）。 */
export function HeroTilt({ className, children }: { className?: string; children: ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);
  const frame = useRef(0);

  const update = (rx: number, ry: number, tilting: boolean) => {
    cancelAnimationFrame(frame.current);
    frame.current = requestAnimationFrame(() => {
      const el = ref.current;
      if (!el) return;
      el.style.setProperty("--rx", `${rx.toFixed(2)}deg`);
      el.style.setProperty("--ry", `${ry.toFixed(2)}deg`);
      el.toggleAttribute("data-tilting", tilting);
    });
  };

  const enabled = () =>
    window.matchMedia("(hover: hover) and (pointer: fine)").matches &&
    !window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  return (
    <div
      ref={ref}
      className={className}
      onPointerMove={(e) => {
        if (!enabled()) return;
        const r = e.currentTarget.getBoundingClientRect();
        const px = (e.clientX - r.left) / r.width - 0.5;
        const py = (e.clientY - r.top) / r.height - 0.5;
        update(-py * 7, px * 9, true);
      }}
      onPointerLeave={() => update(0, 0, false)}
    >
      {children}
    </div>
  );
}
