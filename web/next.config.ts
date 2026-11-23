import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // 页面预渲染为静态外壳；GitHub 数据通过 `use cache` 缓存（见 lib/github.ts）。
  cacheComponents: true,
  partialPrefetching: true,
  reactCompiler: true,
  poweredByHeader: false,
  images: {
    formats: ["image/avif", "image/webp"],
  },
};

export default nextConfig;
