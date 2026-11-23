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

截图来自 iPhone 18 Pro Max 模拟器（1320×2868，缩放到 1000px 宽）：

- 状态栏统一为 9:41、满电：`xcrun simctl status_bar <udid> override --time 9:41 --batteryState discharging --batteryLevel 100 --wifiBars 3 --cellularMode notSupported`；
- 英文界面通过启动参数切换，不改应用设置：`xcrun simctl launch <udid> com.mrl.pixiv.PiPixiv -AppleLanguages "(en)" -AppleLocale en_US`；
- 页面跳转用 Maestro 驱动，截图用 `xcrun simctl io <udid> screenshot`。

页面上的灵动岛是 CSS 画的，截图本身不包含。

桌面端截图来自 macOS 桌面版（`./gradlew :desktopApp:run`），窗口 1400×878pt，用 `screencapture -o -l <窗口 ID>` 只截应用窗口（不含阴影），缩放到 1600px 宽。英文版把 JVM 参数改为 `-Duser.language=en -Duser.country=US` 后重新启动。

「大屏」区块（`components/BigScreen.tsx`）只在 `lib/screenshots.ts` 提供了 `desktop` 截图时才会显示。

## 图标字体

`assets/fonts/material-symbols-rounded-subset.woff2` 只包含页面用到的 Material Symbols 图标，清单见 `assets/fonts/material-symbols-icons.txt`。新增图标后需要重新生成：

```bash
ICONS="$(cat assets/fonts/material-symbols-icons.txt),new_icon_name"   # 按字母顺序
curl -s -A "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36" \
  "https://fonts.googleapis.com/css2?family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@20..48,300..600,0..1,0&icon_names=$ICONS&display=block"
```

下载返回 CSS 中的 woff2 地址，覆盖原文件，并更新图标清单。

## 部署

站点部署在 Vercel，域名 `pipixiv.com` 在 Cloudflare 注册，DNS 也由 Cloudflare 托管。站点需要 Node.js 运行时（用到了 `proxy.ts`、`'use cache'` 和图片优化），不能导出为纯静态文件；除 Vercel 外，也可以部署到任意能运行 `next start` 的服务器。

### Vercel 项目设置

| 设置 | 值 |
| --- | --- |
| Root Directory | `web` |
| Production Branch | `develop` |
| Ignored Build Step | `git diff --quiet HEAD^ HEAD -- .`（只在 `web/` 有改动时重新构建） |
| 环境变量 `NEXT_PUBLIC_SITE_URL` | `https://pipixiv.com` |
| 环境变量 `GITHUB_TOKEN` | 只读公开仓库的 fine-grained token。Vercel 的出口 IP 由多个项目共用，不配置时容易触发 GitHub API 限额，版本号、下载链接和 Star 数会退回默认显示 |

在 Settings → Domains 中添加 `pipixiv.com` 和 `www.pipixiv.com`，并将后者设为跳转到前者。

### Cloudflare DNS

| 类型 | 名称 | 内容 | 代理状态 |
| --- | --- | --- | --- |
| A | `@` | Vercel 域名页面显示的 IP | 仅 DNS |
| CNAME | `www` | Vercel 域名页面显示的地址 | 仅 DNS |

- 两条记录保持「仅 DNS」（灰色云朵），开启代理会导致 Vercel 无法验证域名或签发证书；
- 添加前删除 `@` 已有的 A / AAAA 记录；
- 如有 CAA 记录，需要允许 `letsencrypt.org`。
