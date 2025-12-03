<div align="center">

<a href="https://github.com/master-lzh/PiPixiv">
<img src="../.idea/icon.svg" width="80" alt="PiPixiv Logo">
</a>

# PiPixiv [App](#)

### Third-party Pixiv Client

A third-party Pixiv App entirely written
by [Jetpack Compose](https://developer.android.com/develop/ui/compose)

[![Kotlin](https://img.shields.io/badge/kotlin-2.2.21-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/master-lzh/PiPixiv/release.yml)](https://github.com/master-lzh/PiPixiv/actions/workflows/release.yml)
[![License: Apache-2.0](https://img.shields.io/github/license/master-lzh/PiPixiv?labelColor=27303D&color=0877d2)](/LICENSE)

## Download

[![GitHub Release](https://img.shields.io/github/v/release/master-lzh/PiPixiv?label=Stable)](https://github.com/master-lzh/PiPixiv/releases)
[![GitHub downloads](https://img.shields.io/github/downloads/master-lzh/PiPixiv/total?label=downloads&labelColor=27303D&color=0D1117&logo=github&logoColor=FFFFFF&style=flat)](https://github.com/master-lzh/PiPixiv/releases)
[![F-Droid Version](https://img.shields.io/f-droid/v/com.mrl.pixiv)](https://f-droid.org/packages/com.mrl.pixiv/)

### Supports **[Android 8.0]()** or higher

## Features

<div align="left">

* Log in using a Pixiv account.
* Homepage recommended illustrations.
* Search illustrations and sort by popularity, latest, etc.
* Bookmark illustrations/Follow artists.
* View illustration details and recommended illustrations.
* Long press on an image to download the original illustration.

### To-do List

* **Features and PRs are welcome**
* More...

### v1.1.0

- [x] Refactor the network layer, replacing OkHttp with Ktor
- [x] Refactor datastore to MMKV
- [x] Refactor some global states into a singleton pattern for code simplification and easier
  dependency injection
- [x] Adapt new navigation
- [x] Settings page
- [x] Favourites page
- [x] Followed page
- [x] History page
- [x] Translation
- [x] Deep link support (open from https://www.pixiv.net/illust/xxxxxx)

</div>

## App Preview

| ![Home](https://github.com/master-lzh/PiPixiv/assets/60057825/0c9431bf-bff1-4752-9d62-f2721b3ade5e)           | ![SearchPreview](https://github.com/master-lzh/PiPixiv/assets/60057825/240c5011-cbdb-4423-8d41-b787b5495d4d) |
|---------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| ![Search](https://github.com/master-lzh/PiPixiv/assets/60057825/8d44b554-7cdd-4eeb-a520-a93e6fc7507d)         | ![Search Result](https://github.com/master-lzh/PiPixiv/assets/60057825/7b7f6ea4-5df7-46b9-ba65-4cb1b2f52373) |
| ![Picture Detail](https://github.com/master-lzh/PiPixiv/assets/60057825/dfe36948-525c-486d-a339-6c2c78b5aebf) |                                                                                                              |

## Star History

[![Star History](https://starchart.cc/master-lzh/PiPixiv.svg?variant=adaptive)](https://starchart.cc/master-lzh/PiPixiv)

## Acknowledgments

<div align="left">

This project uses or references several open-source projects:

- **[Coil](https://github.com/coil-kt/coil)**: An Android image loading library supported by Kotlin
  Coroutines
- **[Koin](https://github.com/InsertKoinIO/koin)**: A pragmatic lightweight dependency injection
  framework for Kotlin developers
- **[Mihon](https://github.com/mihonapp/mihon)**: Discover and read manga, webtoons, comics, etc.
  Reference application language switching feature
- **[pixez-flutter](https://github.com/Notsfsssf/pixez-flutter)**: Reference login implementation

</div>

</div>
