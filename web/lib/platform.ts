"use client";

import { useSyncExternalStore } from "react";

export type Platform = "android" | "ios" | "windows" | "macos" | "linux";

function detectPlatform(): Platform | null {
  const ua = navigator.userAgent || "";
  const nav = navigator as Navigator & { userAgentData?: { platform?: string } };
  const p = nav.userAgentData?.platform || navigator.platform || "";
  if (/android/i.test(ua)) return "android";
  if (/iPhone|iPad|iPod/i.test(ua) || (/Mac/i.test(p) && navigator.maxTouchPoints > 1)) return "ios";
  if (/Win/i.test(p) || /Windows/i.test(ua)) return "windows";
  if (/Mac/i.test(p) || /Mac OS X/i.test(ua)) return "macos";
  if (/Linux|X11/i.test(p) || /Linux|X11/i.test(ua)) return "linux";
  return null;
}

const subscribe = () => () => {};

/** 访客的平台；服务端渲染和水合期间为 null。 */
export function usePlatform(): Platform | null {
  return useSyncExternalStore(subscribe, detectPlatform, () => null);
}
