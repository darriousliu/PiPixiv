import localFont from "next/font/local";

/** Google Sans Flex 拉丁字符子集（SIL Open Font License 1.1）。中日韩文字回退到系统字体。 */
export const sans = localFont({
  src: "../assets/fonts/google-sans-flex-latin.woff2",
  weight: "400 800",
  style: "normal",
  display: "swap",
  variable: "--font-sans",
});

/**
 * Material Symbols Rounded，只包含站点用到的图标（Apache License 2.0）。
 * 新增图标时需要重新生成该文件，见 README。
 */
export const icons = localFont({
  src: "../assets/fonts/material-symbols-rounded-subset.woff2",
  weight: "300 600",
  style: "normal",
  display: "block",
  variable: "--font-icons",
  adjustFontFallback: false,
});
