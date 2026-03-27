<div align="center">

<a href="https://github.com/darriousliu/PiPixiv">
<img src=".idea/icon.svg" width="80" alt="PiPixiv Logo">
</a>

### [English README](./.github/README-en.md)

# PiPixiv [应用](#)

### 第三方 Pixiv 客户端

一个完全由 [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) 编写的跨平台第三方
Pixiv 应用

[![Kotlin](https://img.shields.io/badge/kotlin-2.3.10-blue.svg?logo=kotlin)](https://kotlinlang.org)
![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.10.0-blue)
[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/darriousliu/PiPixiv/release.yml)](https://github.com/darriousliu/PiPixiv/actions/workflows/release.yml)
[![License: Apache-2.0](https://img.shields.io/github/license/darriousliu/PiPixiv?labelColor=27303D&color=0877d2)](/LICENSE)

## 📥 下载

[![GitHub Release](https://img.shields.io/github/v/release/darriousliu/PiPixiv?label=稳定版)](https://github.com/darriousliu/PiPixiv/releases)
[![GitHub downloads](https://img.shields.io/github/downloads/darriousliu/PiPixiv/total?label=下载量&labelColor=27303D&color=0D1117&logo=github&logoColor=FFFFFF&style=flat)](https://github.com/darriousliu/PiPixiv/releases)
[![F-Droid Version](https://img.shields.io/f-droid/v/com.mrl.pixiv)](https://f-droid.org/packages/com.mrl.pixiv/)

## 📸 应用预览

<img width="100%" alt="b48d643b-8bd4-479c-bc7b-9c586a9fafd8" src="https://github.com/user-attachments/assets/396ff31e-fecc-4447-bb6f-f9cb3e79b812" />

### 📱 支持平台

<div align="left">

- **Android**: Android 8.0 或更高版本
- **iOS**: iOS 17 或更高版本
- **Windows**: x86_64
- **macOS**: arm64 (Apple Silicon)
- **Linux**: x86_64

</div>

## ✨ 功能

<div align="left">

* 🔐 使用 Pixiv 账号登录（OAuth 认证）。
* 🏠 首页推荐插图瀑布流展示。
* 🆕 查看最新动态（发现、收藏、关注分类）。
* 🔍 搜索插图/小说和用户，按人气、最新等排序。
* 🏆 查看排行榜（日榜、周榜、月榜、男性/女性向、AI 生成、R-18 等多种类型）。
* ❤️ 收藏插图/小说 / 关注或取关作者。
* 📜 查看浏览历史（支持搜索和清空）。
* 🖼️ 查看插图详情（多图、UGOIRA 动图、推荐插图）。
* 💬 查看和发表评论。
* 👤 查看用户主页和作品列表。
* 👥 查看关注列表和粉丝列表。
* 📁 收藏管理（按标签筛选收藏作品）。
* ⬇️ 下载原始插图或 GIF，自定义文件命名，管理下载队列。
* ⚙️ 应用设置（语言、网络代理/SNI 绕过、图片来源、网格列数、R-18 内容、私密收藏等）。
* 🚫 屏蔽作品、用户和标签。
* 🔗 深度链接支持。
* 📦 应用数据管理（缓存清理、数据导出/导入）。
* 🔄 桌面应用滚动列表支持 `R` 快捷键返回顶部或刷新和⬆️⬇️按键滚动
* 📖 **小说模式**：首页、动态、收藏、排行榜、搜索页均支持插画/小说视图切换，偏好设置持久化保存。
* 📝 **小说阅读**：沉浸式阅读界面，支持调节字体大小与行距、章节导航（上一章/下一章）。
* 🤖 **小说 AI 翻译**：支持接入 OpenAI / Claude / Gemini 翻译小说正文，本地缓存，可切换原文/译文。
* 💾 **阅读进度记忆**：小说阅读进度自动保存，下次打开精确恢复到段落级别。
* 🍪 **Cookie 登录**：支持通过网页端 Cookie（PHPSESSID）登录，无需手动填写 Token。
* ⌨️ **ESC 返回**：桌面端支持 ESC 键返回上一页。
* 🏷️ **标签操作增强**：长按 Tag 可收藏标签或复制到剪贴板。

### 📋 待办事项

* **欢迎提出Feature和PR**
* 更多...

</div>

## ⭐ 星标历史

[![Star History](https://starchart.cc/darriousliu/PiPixiv.svg?variant=adaptive)](https://starchart.cc/darriousliu/PiPixiv)

## 🙏 鸣谢

<div align="left">

本项目使用或参考了几个开源项目：

- **[Coil](https://github.com/coil-kt/coil)**: 一个由 Kotlin Coroutines 支持的 Android 图像加载库
- **[Koin](https://github.com/InsertKoinIO/koin)**: 面向 Kotlin 开发者的实用轻量级依赖注入框架
- **[Mihon](https://github.com/mihonapp/mihon)**: 发现并阅读漫画、网络漫画、漫画等。参考应用程序语言切换功能
- **[pixez-flutter](https://github.com/Notsfsssf/pixez-flutter)**: 参考登录实现方案
- **[Pixiv-MultiPlatform](https://github.com/magic-cucumber/Pixiv-MultiPlatform)**: 参考桌面滚动快捷键实现方式

</div>

</div>