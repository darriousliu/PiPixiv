"use client";

import { usePlatform, type Platform } from "@/lib/platform";

/** 识别出访客的平台后，显示下载按钮对应的平台。 */
export function SmartDownloadHint({
  fallback,
  hints,
}: {
  fallback: string;
  hints: Record<Platform, string>;
}) {
  const platform = usePlatform();
  return <small>{platform ? hints[platform] : fallback}</small>;
}
