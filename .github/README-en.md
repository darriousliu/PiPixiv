<div align="center">

<a href="https://github.com/darriousliu/PiPixiv">
<img src="../.idea/icon.svg" width="80" alt="PiPixiv Logo">
</a>

# PiPixiv [App](#)

### Third-party Pixiv Client

A cross-platform third-party Pixiv App entirely written
by [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/)

[![Kotlin](https://img.shields.io/badge/kotlin-2.4.20--RC3-blue.svg?logo=kotlin)](https://kotlinlang.org)
![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.12.0-blue)
[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/darriousliu/PiPixiv/release.yml)](https://github.com/darriousliu/PiPixiv/actions/workflows/release.yml)
[![License: Apache-2.0](https://img.shields.io/github/license/darriousliu/PiPixiv?labelColor=27303D&color=0877d2)](/LICENSE)

## 📥 Download

Current source version: **2.4.0**.

[![GitHub Release](https://img.shields.io/github/v/release/darriousliu/PiPixiv?label=Stable)](https://github.com/darriousliu/PiPixiv/releases)
[![GitHub downloads](https://img.shields.io/github/downloads/darriousliu/PiPixiv/total?label=downloads&labelColor=27303D&color=0D1117&logo=github&logoColor=FFFFFF&style=flat)](https://github.com/darriousliu/PiPixiv/releases)
[![F-Droid Version](https://img.shields.io/f-droid/v/com.mrl.pixiv)](https://f-droid.org/packages/com.mrl.pixiv/)

## 📸 App Preview

<img width="100%" alt="b48d643b-8bd4-479c-bc7b-9c586a9fafd8" src="https://github.com/user-attachments/assets/396ff31e-fecc-4447-bb6f-f9cb3e79b812" />

### 📱 Supported Platforms

<div align="left">

- **Android**: Android 8.0 or higher
- **iOS**: iOS 18 or higher
- **Windows**: x86_64
- **macOS**: arm64 (Apple Silicon)
- **Linux**: x86_64

</div>

## ✨ Features

<div align="left">

### 🔐 Authentication

* 🔐 Log in with Pixiv account (OAuth authentication).
* 🍪 Log in via web Cookie (PHPSESSID) without manually entering a token.

### 🏠 Content Browsing

* 🏠 Homepage recommended illustrations in waterfall layout.
* 🆕 Browse latest content (Discover, Bookmarks, Following).
* 🏆 View rankings (Daily, Weekly, Monthly, Male/Female-oriented, AI-generated, etc.).
* 📜 Local illustration and novel browsing history with search, clearing, and automatic cleanup; Pixiv Premium users can enable cloud history.

### 🖼️ Illustration

* 🖼️ View illustration details (multi-page, UGOIRA animation, related artworks).
* 🔎 Choose preview image quality, zoom into images, and view related artworks at their original aspect ratios.
* ⬇️ Download original illustrations or GIFs, customize file naming, manage download queue.

### 📖 Novel

* 📖 Switch between illustration/novel view across Home, Feed, Collection, Ranking, and Search pages; preference is persisted.
* 📝 Immersive novel reading with adjustable font size, line spacing, and chapter navigation (previous/next chapter), preserving your reading position when changing the layout.
* ℹ️ View the synopsis, tags, and series information while reading, and open the author's profile or series contents.
* 🤖 AI novel translation with OpenAI / Claude / Gemini and compatible endpoints, including local network services; configure the model, timeout, and request parameters. Supports fetching available models, global concurrency limits, and translating novel text, titles, and synopses.
* 🌐 Stream translated text, cache translations locally, and switch between the original and translation.
* 📚 Read Later translation queue with task status, retries for failed tasks, and translation regeneration.
* 💾 Automatically save and restore reading progress, use Pixiv novel reading markers, and export novels as TXT.

### 🔍 Search & Discovery

* 🔍 Search illustrations/novels and users, sort by popularity, latest, etc.
* 🛠️ Configure default matching, sorting, and AI content filters; temporary changes in search leave global defaults intact.
* 📄 Choose continuous scrolling or paged search results; supported results offer previous/next page and page-jump controls.
* 🖼️ Choose square thumbnails or an original-aspect-ratio waterfall layout for illustration search; novel search uses wider lists and compact titles.
* 🏷️ Long-press a tag to collect it or copy it to clipboard.

### 👤 User & Social

* 👤 View author bios, profile and workspace details, illustration/manga/novel submissions, and public bookmarks; open novels directly from their previews.
* 👥 View following lists, with public/private tabs for your own follows.
* 💬 View and post comments.

### ❤️ Bookmarks & Interactions

* ❤️ Bookmark illustrations/novels, follow or unfollow artists.
* 📁 Collection management (filter bookmarks by tag).

### ⚙️ Settings & System

* ⚙️ App settings (Language, Network proxy / SNI bypass, Image source, Grid columns, Private bookmarks, etc.).
* 🔒 Privacy settings control R-18 content visibility and clipboard reading when opening search.
* 🚫 Block artworks, users, and tags, and filter long novel tags by length and segment count.
* 🔗 Deep link support.
* 📦 App data management (cache clearing, data export/import).
* 🆕 Update dialogs display release notes as Markdown with clickable links.

### 🖥️ Desktop Platform

* 🔄 Scrolling list supports `R` shortcut key to return to top or refresh, and ⬆️⬇️ arrow keys to scroll.
* ⌨️ Press ESC to go back.

### 📋 To-do List

* **Features and PRs are welcome**
* More...

</div>

## 🛠️ Development Environment

<div align="left">

* Kotlin **2.4.20-RC3**, Compose Multiplatform **1.12.0**, and Android Gradle Plugin **9.4.0**.
* Gradle Wrapper **9.6.1** and JDK toolchain **25**; Android compile/target SDK **37**, minimum SDK **26**.
* iOS uses Swift Export with an **iOS 18.0** deployment target.

Dependency versions are defined in the [version catalog](../gradle/libs.versions.toml). App versions are set in [gradle.properties](../gradle.properties) and the [iOS configuration](../iosApp/Configuration/Config.xcconfig).

</div>

## ⭐ Star History

[![Star History](https://starchart.cc/darriousliu/PiPixiv.svg?variant=adaptive)](https://starchart.cc/darriousliu/PiPixiv)

## 🙏 Acknowledgments

<div align="left">

This project uses or references several open-source projects:

- **[Coil](https://github.com/coil-kt/coil)**: A cross-platform image loading library powered by Kotlin
  Coroutines
- **[Koin](https://github.com/InsertKoinIO/koin)**: A pragmatic lightweight dependency injection
  framework for Kotlin developers
- **[Multiplatform Markdown Renderer](https://github.com/mikepenz/multiplatform-markdown-renderer)**: Markdown rendering components for Compose Multiplatform
- **[Mihon](https://github.com/mihonapp/mihon)**: Discover and read manga, webtoons, comics, etc.
  Reference application language switching feature
- **[pixez-flutter](https://github.com/Notsfsssf/pixez-flutter)**: Reference login implementation
- **[Pixiv-MultiPlatform](https://github.com/magic-cucumber/Pixiv-MultiPlatform)**: Reference desktop scrolling shortcut implementation

</div>

</div>
