# PiPixiv 官网

PiPixiv 的项目介绍页。基于 **Next.js 16（App Router）+ React 19**，开启了 Cache Components 与 React Compiler：

- 页面在构建时预渲染为静态 HTML（`/zh`、`/en`），首屏不依赖客户端 JS，对 SEO 友好；
- 版本号、下载链接、Star 数和累计下载量在服务端从 GitHub API 获取，用 `'use cache'` + `cacheLife('hours')` 缓存，每小时后台刷新；
- 只有导航、截图轮播、平台识别、数字动画等交互部分是客户端组件。

## 开发

需要 Node.js 20.9 及以上。

```bash
npm install
npm run dev        # http://localhost:3000
npm run build      # 生产构建
npm run start      # 运行生产构建
npm run lint
npm run typecheck
```

用 WebStorm 打开 `web/` 目录即可，`package.json` 里的脚本可以直接在编辑器中运行。

### 环境变量（可选）

| 变量 | 作用 |
| --- | --- |
| `NEXT_PUBLIC_SITE_URL` | 站点正式地址，用于 canonical、hreflang、Open Graph、sitemap。未设置时为 `https://pipixiv.com` |
| `GITHUB_TOKEN` | 提高 GitHub API 的请求额度（未认证时每小时 60 次） |

## 目录

```
app/[locale]/        根布局与首页（每种语言一个静态页面）
app/sitemap.ts       sitemap.xml，含多语言 alternates
app/robots.ts        robots.txt
proxy.ts             访问 / 时按 Cookie 或 Accept-Language 跳转到对应语言
components/          页面区块，样式为同名 *.module.css
i18n/                语言配置与文案（dictionaries/*.json）
lib/github.ts        GitHub Release / 统计数据（服务端缓存）
lib/screenshots.ts   各语言的截图清单
assets/screenshots/  截图（按语言分目录）
assets/fonts/        自托管字体
```

## 多语言

新增一种语言：

1. 在 `i18n/config.ts` 的 `locales`、`localeMeta` 中注册；
2. 复制 `i18n/dictionaries/zh.json` 为 `<code>.json` 并翻译，在 `i18n/dictionaries.ts` 中登记；
3. （可选）把该语言界面的截图放到 `assets/screenshots/<code>/`，并在 `lib/screenshots.ts` 的 `overrides` 中引用。未提供的截图会回退到中文截图。

语言切换会写入 `NEXT_LOCALE` Cookie，之后访问 `/` 会直接进入该语言。

## 截图

截图按界面语言放在 `assets/screenshots/<code>/` 下，并在 `lib/screenshots.ts` 中引用：

- 手机截图宽 1000px，不含灵动岛（页面上的灵动岛由 CSS 绘制）；
- 桌面截图宽 1600px，只截应用窗口，不含阴影。

「大屏」区块（`components/BigScreen.tsx`）只在提供了 `desktop` 截图时才会显示。

分享卡片图（Open Graph / Twitter）放在 `assets/og/<code>.jpg`，尺寸 1200×630，更换截图后需要同步更新。

## 图标字体

`assets/fonts/material-symbols-rounded-subset.woff2` 只包含页面用到的 Material Symbols 图标，清单见 `assets/fonts/material-symbols-icons.txt`。新增图标后需要重新生成：

```bash
ICONS="$(cat assets/fonts/material-symbols-icons.txt),new_icon_name"   # 按字母顺序
curl -s -A "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36" \
  "https://fonts.googleapis.com/css2?family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@20..48,300..600,0..1,0&icon_names=$ICONS&display=block"
```

下载返回 CSS 中的 woff2 地址，覆盖原文件，并更新图标清单。

## 部署

站点需要 Node.js 运行时（用到了 `proxy.ts`、`'use cache'` 和图片优化），不能导出为纯静态文件。
