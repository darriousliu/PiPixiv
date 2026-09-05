<div align="center">

<a href="https://github.com/darriousliu/PiPixiv">
<img src=".idea/icon.svg" width="80" alt="PiPixiv Logo">
</a>

### [English README](./.github/README-en.md)

# PiPixiv [应用](#)

### 第三方 Pixiv 客户端

一个完全由 [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) 编写的跨平台第三方
Pixiv 应用

[![Kotlin](https://img.shields.io/badge/kotlin-2.4.20--RC3-blue.svg?logo=kotlin)](https://kotlinlang.org)
![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.12.0-blue)
[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/darriousliu/PiPixiv/release.yml)](https://github.com/darriousliu/PiPixiv/actions/workflows/release.yml)
[![License: Apache-2.0](https://img.shields.io/github/license/darriousliu/PiPixiv?labelColor=27303D&color=0877d2)](/LICENSE)

## 📥 下载

当前源码版本：**2.4.0**。

[![GitHub Release](https://img.shields.io/github/v/release/darriousliu/PiPixiv?label=稳定版)](https://github.com/darriousliu/PiPixiv/releases)
[![GitHub downloads](https://img.shields.io/github/downloads/darriousliu/PiPixiv/total?label=下载量&labelColor=27303D&color=0D1117&logo=github&logoColor=FFFFFF&style=flat)](https://github.com/darriousliu/PiPixiv/releases)
[![F-Droid Version](https://img.shields.io/f-droid/v/com.mrl.pixiv)](https://f-droid.org/packages/com.mrl.pixiv/)

## 📸 应用预览

<img width="100%" alt="b48d643b-8bd4-479c-bc7b-9c586a9fafd8" src="https://github.com/user-attachments/assets/396ff31e-fecc-4447-bb6f-f9cb3e79b812" />

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

### 🔐 认证与登录

* 🔐 使用 Pixiv 账号登录（OAuth 认证）。
* 🍪 通过网页端 Cookie（PHPSESSID）登录，无需手动填写 Token。

### 🏠 内容浏览

* 🏠 首页推荐插图瀑布流展示。
* 🆕 查看最新动态（发现、收藏、关注分类）。
* 🏆 查看排行榜（日榜、周榜、月榜、男性/女性向、AI 生成等多种类型）。
* 📜 插画和小说的本地浏览历史，支持搜索、清空和自动清理；Pixiv 高级会员可启用云端历史。

### 🖼️ 插图

* 🖼️ 查看插图详情（多图、UGOIRA 动图、推荐插图）。
* 🔎 图片预览支持选择画质、缩放，以及按原图比例查看相关作品。
* ⬇️ 下载原始插图或 GIF，自定义文件命名，管理下载队列。

### 📖 小说

* 📖 插画/小说视图一键切换，首页、动态、收藏、排行榜、搜索页均支持，偏好设置持久化保存。
* 📝 沉浸式小说阅读，支持调节字体大小与行距、章节导航（上一章/下一章），调整排版时保持阅读位置。
* ℹ️ 阅读时查看作品简介、标签和系列信息，跳转作者主页或系列目录。
* 🤖 小说 AI 翻译：支持 OpenAI / Claude / Gemini 和兼容接口，可配置局域网服务、模型、超时及请求参数；支持获取可用模型列表、限制全局并发，以及正文、标题和简介翻译。
* 🌐 译文支持流式显示、本地缓存和原文/译文切换。
* 📚 稍后阅读翻译队列，支持查看任务状态、重试失败任务和重新生成译文。
* 💾 自动保存和恢复阅读进度，支持 Pixiv 小说书签及 TXT 导出。

### 🔍 搜索与发现

* 🔍 搜索插图/小说和用户，按人气、最新等排序。
* 🛠️ 配置默认匹配方式、排序和 AI 内容筛选；搜索页可临时调整，不覆盖全局默认值。
* 📄 搜索结果支持连续滚动或分页显示；支持分页的结果可前后翻页和跳页。
* 🖼️ 插画搜索可选择方图或原图比例瀑布流；小说搜索使用更宽的列表和紧凑标题。
* 🏷️ 长按 Tag 可收藏标签或复制到剪贴板。

### 👤 用户与社交

* 👤 查看作者简介、个人资料和工作环境，以及插画、漫画、小说投稿和公开收藏；小说预览可直接打开阅读。
* 👥 查看关注列表，自己的关注支持公开/私密分类。
* 💬 查看和发表评论。

### ❤️ 收藏与互动

* ❤️ 收藏插图/小说，关注或取关作者。
* 📁 收藏管理（按标签筛选收藏作品）。

### ⚙️ 设置与系统

* ⚙️ 应用设置（语言、网络代理/SNI 绕过、图片来源、网格列数、私密收藏等）。
* 🔒 隐私设置支持控制 R-18 内容显示、进入搜索页时读取剪贴板。
* 🚫 屏蔽作品、用户和标签，可按长度与分段数量过滤小说长标签。
* 🔗 深度链接支持。
* 📦 应用数据管理（缓存清理、数据导出/导入）。
* 🆕 更新弹窗以 Markdown 展示发布说明，支持点击正文链接。

### 🖥️ 桌面平台

* 🔄 滚动列表支持 `R` 快捷键返回顶部或刷新，⬆️⬇️ 方向键滚动。
* ⌨️ 支持 ESC 键返回上一页。

### 📋 待办事项

* **欢迎提出Feature和PR**
* 更多...

</div>

## 🛠️ 开发环境

<div align="left">

* Kotlin **2.4.20-RC3**、Compose Multiplatform **1.12.0**、Android Gradle Plugin **9.4.0**。
* Gradle Wrapper **9.6.1**、JDK 工具链 **25**；Android 编译/目标 SDK **37**，最低 SDK **26**。
* iOS 使用 Swift Export，部署目标为 **iOS 18.0**。

依赖版本以 [版本目录](./gradle/libs.versions.toml) 为准，应用版本见 [gradle.properties](./gradle.properties) 与 [iOS 配置](./iosApp/Configuration/Config.xcconfig)。

</div>

## ⭐ 星标历史

[![Star History](https://starchart.cc/darriousliu/PiPixiv.svg?variant=adaptive)](https://starchart.cc/darriousliu/PiPixiv)

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
