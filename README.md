<div align="center">

**简体中文** &nbsp;|&nbsp; [繁體中文](./README-zh-TW.md) &nbsp;|&nbsp; [English](./README-en.md) &nbsp;|&nbsp; [日本語](./README-ja.md)

<a href="https://github.com/darriousliu/PiPixiv">
  <img src=".idea/icon.svg" width="88" height="88" alt="PiPixiv Logo" />
</a>

# PiPixiv

一款用 Compose Multiplatform 开发的开源 Pixiv 第三方客户端

**[官网](https://pipixiv.com/zh)** &nbsp;·&nbsp; **[安装说明](#下载)** &nbsp;·&nbsp; **[功能](#功能)** &nbsp;·&nbsp; **[架构](#架构)**

[![下载最新版](https://img.shields.io/github/v/release/darriousliu/PiPixiv?style=for-the-badge&logo=github&logoColor=white&label=%E4%B8%8B%E8%BD%BD%E6%9C%80%E6%96%B0%E7%89%88&labelColor=1689DD&color=0068B8)](https://github.com/darriousliu/PiPixiv/releases/latest) &nbsp;[![F-Droid](https://img.shields.io/f-droid/v/com.mrl.pixiv?style=for-the-badge&logo=f-droid&logoColor=white&label=F-Droid&labelColor=24292F&color=3B434E)](https://f-droid.org/packages/com.mrl.pixiv/)

[![Android](https://img.shields.io/badge/Android-24292F?style=flat-square&logo=android&logoColor=3DDC84)](#下载)
[![iOS](https://img.shields.io/badge/iOS-24292F?style=flat-square&logo=apple&logoColor=FFFFFF)](#下载)
[![Windows][windows-badge]](#下载)
[![macOS](https://img.shields.io/badge/macOS-24292F?style=flat-square&logo=apple&logoColor=FFFFFF)](#下载)
[![Linux](https://img.shields.io/badge/Linux-24292F?style=flat-square&logo=linux&logoColor=FCC624)](#下载)
<br/>
[![下载量](https://img.shields.io/github/downloads/darriousliu/PiPixiv/total?style=flat-square&logo=github&logoColor=FFFFFF&label=%E4%B8%8B%E8%BD%BD%E9%87%8F&labelColor=24292F&color=3B434E)](https://github.com/darriousliu/PiPixiv/releases)
[![构建状态](https://img.shields.io/github/actions/workflow/status/darriousliu/PiPixiv/release.yml?style=flat-square&logo=githubactions&logoColor=58A6FF&label=%E6%9E%84%E5%BB%BA&labelColor=24292F)](https://github.com/darriousliu/PiPixiv/actions/workflows/release.yml)
[![Telegram](https://img.shields.io/badge/Telegram-%40PiPixivChannel-3B434E?style=flat-square&logo=telegram&logoColor=26A5E4&labelColor=24292F)](https://t.me/PiPixivChannel)
<br/>
[![Kotlin](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fdarriousliu%2FPiPixiv%2Fmaster%2Fgradle%2Flibs.versions.toml&query=%24.versions.kotlin&style=flat-square&label=Kotlin&logo=kotlin&logoColor=A97BFF&labelColor=24292F&color=3B434E)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fdarriousliu%2FPiPixiv%2Fmaster%2Fgradle%2Flibs.versions.toml&query=%24.versions.composeMultiplatform&style=flat-square&label=Compose%20Multiplatform&logo=jetpackcompose&logoColor=4285F4&labelColor=24292F&color=3B434E)](https://www.jetbrains.com/compose-multiplatform/)
[![License: Apache-2.0](https://img.shields.io/github/license/darriousliu/PiPixiv?style=flat-square&label=License&labelColor=24292F&color=3B434E)](./LICENSE)

<img width="100%" alt="PiPixiv 在平板、笔记本和手机上的界面预览" src="docs/preview.png" />

</div>

[windows-badge]: https://img.shields.io/badge/Windows-24292F?style=flat-square&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iIzAwQTRFRiIgZD0iTTAgMGgxMS4zNzd2MTEuMzcySDB6bTEyLjYyMyAwSDI0djExLjM3MkgxMi42MjN6TTAgMTIuNjIzaDExLjM3N1YyNEgwem0xMi42MjMgMEgyNFYyNEgxMi42MjN6Ii8%2BPC9zdmc%2B

## 功能

- 🎨 **插画与漫画**：浏览首页推荐、排行榜、关注动态和 UGOIRA 动图，支持原图下载、批量下载和下载队列管理。
- 📖 **小说阅读**：沉浸式阅读，可调字号与行距，自动保存进度，支持系列目录、Pixiv 书签和 TXT 导出。
- 🤖 **AI 翻译**：接入 OpenAI、Claude、Gemini 及兼容接口翻译小说，译文流式显示并缓存在本地。
- 👥 **多账号**：支持 OAuth 和网页 Cookie 登录，账号之间即时切换，浏览历史和阅读进度按账号分开保存。
- 🌐 **网络设置**：可使用系统代理、直连或 HTTP / SOCKS 代理，Android 和桌面端还支持 ECH，内置网络自检。
- 🖥️ **多端适配**：手机、平板和桌面使用自适应布局，界面支持 13 种语言，桌面端支持快捷键。

<details>
<summary><b>完整功能列表</b></summary>

#### 账号

- 使用 Pixiv 账号登录（OAuth），或通过网页端 Cookie（PHPSESSID）登录，无需手动填写 Token。
- 在「设置 → 切换账户」中添加、切换或移除账号。浏览历史、搜索历史、稍后阅读和小说阅读进度按账号保存，移除账号时默认保留其本地数据。
- 退出登录前二次确认。

#### 浏览

- 首页推荐插画以瀑布流展示。
- 查看最新动态，分为发现、收藏和关注。
- 查看排行榜，包括日榜、周榜、月榜、男性 / 女性向和 AI 生成等。
- 首页、动态、收藏、排行榜和搜索页均可在插画与小说视图间切换，偏好会被保存。
- 插画和小说的本地浏览历史，支持搜索、清空和自动清理；Pixiv 高级会员可启用云端历史。

#### 插画

- 查看作品详情，包括多图、UGOIRA 动图和相关推荐。
- 图片预览可选择画质、缩放，并按原图比例浏览相关作品。
- 下载原图或 GIF 动图，可自定义文件名、管理下载队列；多图作品可批量下载所选或全部原图。

#### 小说

- 沉浸式阅读：调节字号与行距，拖动滚动条定位，调整排版时保持阅读位置。
- 查看完整标题、标签、简介、发布时间和统计数据；阅读时可查看系列信息，并跳转到作者主页或系列目录。
- 在应用内放大查看正文中的插图。
- 系列目录提供继续阅读入口，显示上次阅读的章节和进度百分比。
- 自动保存和恢复阅读进度，支持 Pixiv 小说书签和 TXT 导出。

#### AI 翻译

- 支持 OpenAI、Claude、Gemini 及兼容接口，可使用局域网服务，并自定义模型、超时和请求参数。
- 可获取可用模型列表、限制全局并发，翻译正文、标题和简介。
- 译文流式显示并缓存在本地，可随时切换原文和译文。
- 稍后阅读翻译队列：查看任务状态、重试失败任务、重新生成译文。

#### 搜索

- 搜索插画、小说和用户，按人气、最新等方式排序。
- 设置默认匹配方式、排序和 AI 内容筛选；插画与小说分别记住上次使用的排序和筛选，重启后仍然保留。
- 搜索结果可连续滚动或分页显示，分页结果支持前后翻页和跳页。
- 插画结果可选择方图或原图比例瀑布流；小说结果使用更宽的列表和紧凑标题。
- 长按标签可收藏或复制到剪贴板。

#### 用户与收藏

- 查看作者简介、个人资料、工作环境、插画 / 漫画 / 小说投稿和公开收藏；点击头像可放大，点击小说预览可直接阅读。
- 查看关注列表，自己的关注区分公开和私密。
- 查看和发表评论。
- 收藏插画和小说，关注或取关作者，按标签筛选收藏。

#### 网络与隐私

- 默认使用系统代理，可切换为直连或手动 HTTP / SOCKS 代理，保存后新请求立即生效。Android 和桌面端另提供 ECH 模式；网页登录可用的连接方式因平台而异。
- 在「网络设置」中自定义 App API 代理域名，并通过「网络自检」检查 DNS、Pixiv API、官网、登录和图片下载。
- 控制 R-18 内容是否显示，以及进入搜索页时是否读取剪贴板。Android Default、iOS 和桌面版可关闭匿名使用统计。
- 屏蔽作品、用户和标签，可按长度和分段数量过滤小说长标签。

#### 设置与数据

- 常规设置：语言、图片来源、网格列数、私密收藏等；底部导航可在悬浮液态玻璃与标准样式之间切换。
- 支持深度链接。
- 在「应用数据」中清理缓存、导入和导出备份。升级后如有待恢复项，可在「数据恢复」中逐项重试，也可导入和导出旧数据包。
- 在「设置 → 关于 → 应用日志」中查看本次和上次运行的日志，可按级别筛选、复制或分享。
- 更新弹窗以 Markdown 展示发布说明；可在「设置 → 关于」中打开 Telegram 更新频道。

#### 桌面端

- 跟随系统浅色 / 深色主题，也可手动选择。
- 在列表中按 `R` 返回顶部或刷新，用方向键滚动，按 `Esc` 返回上一页。

</details>

## 下载

当前源码版本：**2.7.0**。各平台安装包见 [GitHub Release](https://github.com/darriousliu/PiPixiv/releases/latest)。

| 平台 | 系统要求 | 安装包 |
| --- | --- | --- |
| Android | Android 8.0 及以上 | APK（Default / FOSS），也可从 [F-Droid](https://f-droid.org/packages/com.mrl.pixiv/) 安装 |
| iOS | iOS 18 及以上 | 未签名 IPA，需自行签名后安装 |
| Windows | x86_64 | MSI |
| macOS | Apple Silicon（arm64） | DMG |
| Linux | x86_64 | tar.gz |

Android 提供两个版本：

- **Default**：集成 Firebase 和 Sentry，用于崩溃上报和匿名使用统计，匿名统计可在设置中关闭。
- **FOSS**：不包含任何统计和崩溃上报依赖。F-Droid 上架的是这个版本。

版本更新会同步发布到 Telegram 频道 [@PiPixivChannel](https://t.me/PiPixivChannel)。

## 架构

PiPixiv 基于 Kotlin Multiplatform 构建，界面、业务逻辑、网络和存储都在共享代码中实现，各平台只保留很薄的入口层。共享代码编译到 Android、iOS（arm64 真机与模拟器）和 JVM 桌面三类目标。

### 模块划分

```text
         app                 desktopApp            iosApp + iosExport
      (Android)       (Windows / macOS / Linux)    (iOS, Swift Export)
          |                       |                         |
          +-----------------------+-------------------------+
                                  |
                                  v
                             composeApp
                                  |
                                  v
                              feature/*
                                  |
              +-------------------+
              |                   |
              v                   v
          common/ui ----> common/repository
                                  |
                                  v
      +-------------------------------------------------------+
      |  datasource-remote   datasource-local   network   ai  |
      +-------------------------------------------------------+
                                  |
                                  v
                      common/core   common/data
```

图中按层展示主要的依赖方向，箭头指向被依赖的模块。

| 模块 | 职责 |
| --- | --- |
| `app` | Android 应用入口，负责 Default / FOSS 两种构建、签名和 Baseline Profile |
| `desktopApp` | 桌面应用入口，基于 Nucleus 打包 MSI、DMG 和 tar.gz |
| `iosApp`、`iosExport` | Xcode 工程；`iosExport` 通过 Swift Export 把共享代码导出为 `PiPixivKit` 供 Swift 调用 |
| `composeApp` | 共享应用壳，汇总所有功能模块，组装导航和依赖注入 |
| `feature/*` | 按功能拆分的页面，如 `picture`、`novel`、`manga`、`collection`、`setting` 等 |
| `common/ui` | 主题、通用组件和分页状态等界面基础设施 |
| `common/repository` | 仓库层，整合远程接口、本地数据库、网络和 AI 翻译 |
| `common/datasource-remote` | 基于 Ktorfit 定义的 Pixiv API |
| `common/datasource-local` | 基于 Room 3 的本地数据库 |
| `common/network` | Ktor 客户端、代理和 ECH 支持 |
| `common/ai` | 接入 OpenAI、Claude、Gemini 等翻译服务 |
| `common/core` | 键值存储、日志、路由、协程和 ViewModel 等基础能力 |
| `common/data` | 数据模型与用户设置 |
| `common/analytics-default`、`common/analytics-foss` | 统计与崩溃上报的两套实现，按构建版本二选一 |
| `common/platform-api` | 需要原生实现的平台接口，如 iOS 的相册保存和 ZIP 处理由 Swift 实现 |
| `common/icons` | 图标 |
| `lib_strings` | 13 种语言的字符串资源 |

### 技术栈

| 方向 | 方案 |
| --- | --- |
| UI | Compose Multiplatform、Material 3 自适应布局 |
| 导航 | Navigation 3 |
| 依赖注入 | Koin、Koin Annotations（KSP） |
| 网络 | Ktor、Ktorfit；Android 和桌面端使用 OkHttp 引擎，iOS 使用 Darwin 引擎 |
| ECH | 通过 DoH 获取 ECH 配置，基于 Conscrypt 建立连接（Android、桌面端） |
| 存储 | Room 3（内置 SQLite）、MMKV（[mmkv-kotlin](https://github.com/darriousliu/mmkv-kotlin)） |
| 图片 | Coil 3、ZoomImage |
| 基础库 | Kotlin Coroutines / Flow、kotlinx.serialization、Kermit、FileKit |
| 监控 | Firebase Crashlytics / Analytics、Sentry（仅 Default 构建） |
| 桌面端 | Nucleus 提供窗口、WebView 和安装包打包，可选 JVM AOT 缓存 |
| iOS | Swift Export 导出 Kotlin 模块，平台能力由 Swift 实现 |

### 工程化

- **约定插件**：`build-logic` 中的 `pixiv.multiplatform`、`pixiv.multiplatform.compose`、`pixiv.android.application` 和 `pixiv.desktop.compose` 统一各模块的构建配置。
- **依赖管理**：版本集中在 [`gradle/libs.versions.toml`](./gradle/libs.versions.toml)，由 Renovate 自动发起升级。FOSS 构建会检查依赖图，确保不含统计类依赖。
- **持续集成**：`develop` 分支每次推送都会构建全部平台；推送版本标签后构建 6 个安装包（Android Default / FOSS、iOS、Windows、macOS、Linux）并汇总到 Release。另有多语言一致性检查和 Fastlane 元数据校验。
- **性能与调试**：Android 使用 Baseline Profile 优化启动；开发时桌面端支持 Compose Hot Reload，Android 支持 HotSwan 热重载。

## 本地构建

环境要求：

- JDK 25（本机没有时，Gradle 会通过 Foojay 自动下载工具链）
- 支持 Android Gradle Plugin 9.5 的 Android Studio
- 构建 iOS 需要 macOS 和 Xcode

本地构建建议加上 `-PapplyFirebasePlugins=false`，即构建 FOSS 版本，这样不需要 Firebase 和 Sentry 的配置文件：

```bash
# Android
./gradlew :app:assembleDebug -PapplyFirebasePlugins=false

# 桌面端
./gradlew :desktopApp:run -PapplyFirebasePlugins=false
```

iOS 请用 Xcode 打开 `iosApp/PiPixiv.xcodeproj` 运行。

应用版本号在 [`gradle.properties`](./gradle.properties) 和 [iOS 配置](./iosApp/Configuration/Config.xcconfig) 中维护。

## 参与贡献

欢迎提交 Issue 和 Pull Request。代码风格、提交信息、多语言同步和 UI 验证等约定见 [AGENTS.md](./AGENTS.md)。

## 星标历史

<a href="https://www.star-history.com/?repos=darriousliu%2FPiPixiv&amp;type=date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=darriousliu/PiPixiv&amp;type=Date&amp;theme=dark" />
    <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=darriousliu/PiPixiv&amp;type=Date" />
    <img alt="PiPixiv 的 GitHub Star 增长曲线" src="https://api.star-history.com/svg?repos=darriousliu/PiPixiv&amp;type=Date" width="800" />
  </picture>
</a>

## 鸣谢

本项目使用或参考了以下开源项目：

- [Coil](https://github.com/coil-kt/coil)：基于 Kotlin Coroutines 的跨平台图片加载库
- [Koin](https://github.com/InsertKoinIO/koin)：面向 Kotlin 的轻量级依赖注入框架
- [Multiplatform Markdown Renderer](https://github.com/mikepenz/multiplatform-markdown-renderer)：Compose Multiplatform 的 Markdown 渲染组件
- [Mihon](https://github.com/mihonapp/mihon)：参考了应用内语言切换的实现
- [pixez-flutter](https://github.com/Notsfsssf/pixez-flutter)：参考了登录实现方案
- [Pixiv-MultiPlatform](https://github.com/magic-cucumber/Pixiv-MultiPlatform)：参考了桌面端滚动快捷键的实现

## 许可证

本项目基于 [Apache-2.0](./LICENSE) 许可证开源。
