<div align="center">

<a href="https://github.com/darriousliu/PiPixiv">
<img src=".idea/icon.svg" width="80" alt="PiPixiv Logo">
</a>

### [English README](./.github/README-en.md)

# PiPixiv [应用](#)

### 第三方 Pixiv 客户端

一个完全由 [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) 编写的跨平台第三方
Pixiv 应用

[![Kotlin 2.4.21](https://img.shields.io/badge/Kotlin-2.4.21-blue?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform 1.12.2](https://img.shields.io/badge/Compose_Multiplatform-1.12.2-blue)](https://www.jetbrains.com/compose-multiplatform/)
[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/darriousliu/PiPixiv/release.yml)](https://github.com/darriousliu/PiPixiv/actions/workflows/release.yml)
[![License: Apache-2.0](https://img.shields.io/github/license/darriousliu/PiPixiv?labelColor=27303D&color=0877d2)](./LICENSE)

## 📥 下载

当前源码版本：**2.7.0**。已发布版本请查看下方 GitHub Release。

[![GitHub Release](https://img.shields.io/github/v/release/darriousliu/PiPixiv?label=稳定版)](https://github.com/darriousliu/PiPixiv/releases/latest)
[![GitHub downloads](https://img.shields.io/github/downloads/darriousliu/PiPixiv/total?label=下载量&labelColor=27303D&color=0D1117&logo=github&logoColor=FFFFFF&style=flat)](https://github.com/darriousliu/PiPixiv/releases)
[![F-Droid Version](https://img.shields.io/f-droid/v/com.mrl.pixiv)](https://f-droid.org/packages/com.mrl.pixiv/)
[![Telegram @PiPixivChannel](https://img.shields.io/badge/Telegram-%40PiPixivChannel-26A5E4?logo=telegram&logoColor=white)](https://t.me/PiPixivChannel)

## 📸 应用预览

<img width="100%" alt="preview" src="docs/preview.png" />

### 📱 支持平台

<div align="left">

- **Android**: Android 8.0 或更高版本
- **iOS**: iOS 18 或更高版本
- **Windows**: x86_64
- **macOS**: arm64 (Apple Silicon)
- **Linux**: x86_64

</div>

## ✨ 功能

<div align="left">

### 🔐 账号与登录

* 🔐 使用 Pixiv 账号登录（OAuth 认证）。
* 🍪 通过网页端 Cookie（PHPSESSID）登录，无需手动填写 Token。
* 👥 在「设置 → 切换账户」添加、即时切换或移除 Pixiv 账号。浏览历史、搜索历史、稍后阅读和小说阅读进度按账号保存，移除账号时默认保留其本地数据。
* 🔒 登出前二次确认。

### 🏠 内容浏览

* 🏠 首页推荐插图瀑布流展示。
* 🆕 查看最新动态（发现、收藏、关注分类）。
* 🏆 查看排行榜（日榜、周榜、月榜、男性/女性向、AI 生成等多种类型）。
* 📜 插画和小说的本地浏览历史，支持搜索、清空和自动清理；Pixiv 高级会员可启用云端历史。

### 🖼️ 插图

* 🖼️ 查看插图详情（多图、UGOIRA 动图、推荐插图）。
* 🔎 图片预览支持选择画质、缩放，以及按原图比例查看相关作品。
* ⬇️ 下载原图或动图 GIF，支持自定义文件名和管理下载队列。多图作品可从详情批量下载所选或全部原图。

### 📖 小说

* 📖 插画/小说视图一键切换，首页、动态、收藏、排行榜、搜索页均支持，偏好设置持久化保存。
* 📝 沉浸式小说阅读，支持调节字体大小与行距、拖动滚动条定位；回到顶部与章节切换按钮纵向排列，调整排版时保持阅读位置。
* ℹ️ 从小说卡片的「查看更多」查看完整标题、标签、简介、发布时间和统计数据。阅读时可查看系列信息，并跳转作者主页或系列目录。
* 🖼️ 在应用内放大查看小说内嵌图片。
* ▶️ 系列目录提供继续阅读入口，分别显示上次阅读的章节标题与进度百分比。
* 🤖 小说 AI 翻译：支持 OpenAI / Claude / Gemini 和兼容接口，可配置局域网服务、模型、超时及请求参数；支持获取可用模型列表、限制全局并发，以及正文、标题和简介翻译。
* 🌐 译文支持流式显示、本地缓存和原文/译文切换。
* 📚 稍后阅读翻译队列，支持查看任务状态、重试失败任务和重新生成译文。
* 💾 自动保存和恢复阅读进度，支持 Pixiv 小说书签及 TXT 导出。

### 🔍 搜索与发现

* 🔍 搜索插图/小说和用户，按人气、最新等排序。
* 🛠️ 配置默认匹配方式、排序和 AI 内容筛选；插画与小说分别记住上次应用的排序和筛选，重启后仍然保留。
* 📄 搜索结果支持连续滚动或分页显示；支持分页的结果可前后翻页和跳页。
* 🖼️ 插画搜索可选择方图或原图比例瀑布流；小说搜索使用更宽的列表和紧凑标题。
* 🏷️ 长按 Tag 可收藏标签或复制到剪贴板。

### 👤 用户与社交

* 👤 查看作者简介、个人资料和工作环境，以及插画、漫画、小说投稿和公开收藏。点击头像可放大查看，点击小说预览可直接阅读。
* 👥 查看关注列表，自己的关注支持公开/私密分类。
* 💬 查看和发表评论。

### ❤️ 收藏与互动

* ❤️ 收藏插图/小说，关注或取关作者。
* 📁 收藏管理（按标签筛选收藏作品）。

### ⚙️ 设置与系统

* ⚙️ 应用设置（语言、图片来源、网格列数、私密收藏等）。
* 🧭 可在悬浮液态玻璃与标准底部导航栏之间切换。
* 🌐 默认使用系统代理，可切换为直连或手动 HTTP / SOCKS 代理。保存后，新网络请求立即使用新设置。Android 和桌面端另提供 ECH 模式，网页登录支持的连接方式随平台而异。
* 🛠️ 在「网络设置」中可配置自定义 App API 代理域名，并通过「网络自检」检查 DNS、Pixiv API、官网、登录和图片下载。
* 🔒 隐私设置支持控制 R-18 内容显示、进入搜索页时读取剪贴板。Android Default、iOS 和桌面版还可关闭匿名使用统计。
* 🚫 屏蔽作品、用户和标签，可按长度与分段数量过滤小说长标签。
* 🔗 深度链接支持。
* 📦 在「应用数据」中清理缓存、导入和导出备份。升级后如有待恢复项，可在「数据恢复」中逐项重试，也可导入和导出旧数据包。
* 📋 在「设置 → 关于 → 应用日志」查看本次和上次运行的本地日志，可按级别筛选、复制或分享。
* 🆕 更新弹窗以 Markdown 展示发布说明，支持点击正文链接。可在「设置 → 关于」打开 Telegram 更新频道。

### 🖥️ 桌面平台

* 🎨 支持跟随系统浅色/深色主题，也可手动选择主题。
* 🔄 滚动列表支持 `R` 快捷键返回顶部或刷新，⬆️⬇️ 方向键滚动。
* ⌨️ 支持 ESC 键返回上一页。

### 📋 待办事项

* **欢迎提出Feature和PR**
* 更多...

</div>

## 🛠️ 开发环境

<div align="left">

* Kotlin **2.4.21**、Compose Multiplatform **1.12.2**、Android Gradle Plugin **9.5.1**。
* Gradle Wrapper **9.9.1**，JDK 工具链 **25**。
* Android 应用编译 SDK **37.2**、目标 SDK **37**、最低 SDK **26**。
* iOS 使用 Swift Export，部署目标为 **iOS 18.0**。
* 主要依赖包括 AndroidX Compose BOM Beta **2026.11.00**、Navigation 3（Android **1.2.1**、JetBrains **1.2.0**）、Ktor **3.6.1**、Koin **4.2.2** 和 Coil **3.6.3**。

依赖版本以 [版本目录](./gradle/libs.versions.toml) 为准，应用版本见 [gradle.properties](./gradle.properties) 与 [iOS 配置](./iosApp/Configuration/Config.xcconfig)。

</div>

## ⭐ 星标历史

<a href="https://www.star-history.com/?repos=darriousliu%2FPiPixiv&amp;type=date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=darriousliu/PiPixiv&amp;type=Date&amp;theme=dark" />
    <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=darriousliu/PiPixiv&amp;type=Date" />
    <img alt="PiPixiv 的 GitHub Star 增长曲线" src="https://api.star-history.com/svg?repos=darriousliu/PiPixiv&amp;type=Date" width="800" />
  </picture>
</a>

## 🙏 鸣谢

<div align="left">

本项目使用或参考了几个开源项目：

- **[Coil](https://github.com/coil-kt/coil)**: 基于 Kotlin Coroutines 的跨平台图像加载库
- **[Koin](https://github.com/InsertKoinIO/koin)**: 面向 Kotlin 开发者的实用轻量级依赖注入框架
- **[Multiplatform Markdown Renderer](https://github.com/mikepenz/multiplatform-markdown-renderer)**: Compose Multiplatform 的 Markdown 渲染组件
- **[Mihon](https://github.com/mihonapp/mihon)**: 发现并阅读漫画、网络漫画、漫画等。参考应用程序语言切换功能
- **[pixez-flutter](https://github.com/Notsfsssf/pixez-flutter)**: 参考登录实现方案
- **[Pixiv-MultiPlatform](https://github.com/magic-cucumber/Pixiv-MultiPlatform)**: 参考桌面滚动快捷键实现方式

</div>

</div>
