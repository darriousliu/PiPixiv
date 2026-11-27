<div align="center">

[简体中文](./README.md) | **繁體中文** | [English](./README-en.md) | [日本語](./README-ja.md)

<a href="https://github.com/darriousliu/PiPixiv">
  <img src=".idea/icon.svg" width="88" height="88" alt="PiPixiv Logo" />
</a>

# PiPixiv

採用 Compose Multiplatform 開發的開放原始碼 Pixiv 第三方用戶端

**[官方網站（簡體中文）](https://pipixiv.com/zh)** &nbsp;·&nbsp; **[安裝說明](#下載)** &nbsp;·&nbsp; **[功能](#功能)** &nbsp;·&nbsp; **[架構](#架構)**

[![Android](https://img.shields.io/badge/Android-24292F?style=flat-square&logo=android&logoColor=3DDC84)](#下載)
[![iOS](https://img.shields.io/badge/iOS-24292F?style=flat-square&logo=apple&logoColor=FFFFFF)](#下載)
[![Windows][windows-badge]](#下載)
[![macOS](https://img.shields.io/badge/macOS-24292F?style=flat-square&logo=apple&logoColor=FFFFFF)](#下載)
[![Linux](https://img.shields.io/badge/Linux-24292F?style=flat-square&logo=linux&logoColor=FCC624)](#下載)

[![下載最新版](https://img.shields.io/github/v/release/darriousliu/PiPixiv?style=for-the-badge&logo=github&logoColor=white&label=%E4%B8%8B%E8%BC%89%E6%9C%80%E6%96%B0%E7%89%88&labelColor=1689DD&color=0068B8)](https://github.com/darriousliu/PiPixiv/releases/latest)
[![F-Droid](https://img.shields.io/f-droid/v/com.mrl.pixiv?style=for-the-badge&logo=f-droid&logoColor=white&label=F-Droid&labelColor=24292F&color=3B434E)](https://f-droid.org/packages/com.mrl.pixiv/)

[![下載次數](https://img.shields.io/github/downloads/darriousliu/PiPixiv/total?style=flat-square&logo=github&logoColor=FFFFFF&label=%E4%B8%8B%E8%BC%89%E6%AC%A1%E6%95%B8&labelColor=24292F&color=3B434E)](https://github.com/darriousliu/PiPixiv/releases)
[![建置狀態](https://img.shields.io/github/actions/workflow/status/darriousliu/PiPixiv/release.yml?style=flat-square&logo=githubactions&logoColor=58A6FF&label=%E5%BB%BA%E7%BD%AE&labelColor=24292F)](https://github.com/darriousliu/PiPixiv/actions/workflows/release.yml)
[![Telegram](https://img.shields.io/badge/Telegram-%40PiPixivChannel-3B434E?style=flat-square&logo=telegram&logoColor=26A5E4&labelColor=24292F)](https://t.me/PiPixivChannel)

[![Kotlin](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fdarriousliu%2FPiPixiv%2Fmaster%2Fgradle%2Flibs.versions.toml&query=%24.versions.kotlin&style=flat-square&label=Kotlin&logo=kotlin&logoColor=A97BFF&labelColor=24292F&color=3B434E)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fdarriousliu%2FPiPixiv%2Fmaster%2Fgradle%2Flibs.versions.toml&query=%24.versions.composeMultiplatform&style=flat-square&label=Compose%20Multiplatform&logo=jetpackcompose&logoColor=4285F4&labelColor=24292F&color=3B434E)](https://www.jetbrains.com/compose-multiplatform/)

<img width="100%" alt="PiPixiv 在平板、筆電和手機上的畫面" src="docs/preview.png" />

</div>

[windows-badge]: https://img.shields.io/badge/Windows-24292F?style=flat-square&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iIzAwQTRFRiIgZD0iTTAgMGgxMS4zNzd2MTEuMzcySDB6bTEyLjYyMyAwSDI0djExLjM3MkgxMi42MjN6TTAgMTIuNjIzaDExLjM3N1YyNEgwem0xMi42MjMgMEgyNFYyNEgxMi42MjN6Ii8%2BPC9zdmc%2B

## 功能

- 🎨 **插畫與漫畫**　瀏覽首頁推薦、排行榜、追蹤動態和 UGOIRA 動畫。可下載原圖、批次下載，並管理下載佇列。
- 📖 **小說閱讀**　沉浸式閱讀支援調整字級與行距，並會自動儲存閱讀進度。可瀏覽系列目錄、使用 Pixiv 書籤，或將小說匯出成 TXT。
- 🤖 **AI 翻譯**　可透過 OpenAI、Claude、Gemini 及相容的 API 翻譯小說。譯文以串流方式逐步顯示，並快取在裝置上。
- 👥 **多帳號**　支援 OAuth 和網頁 Cookie 登入，可即時切換帳號。各帳號分別儲存瀏覽紀錄和閱讀進度。
- 🌐 **網路設定**　可使用系統代理伺服器、直接連線，或指定 HTTP / SOCKS 代理伺服器。內建網路自我檢測，Android 和桌面版另支援 ECH。
- 🖥️ **跨裝置介面**　版面會依手機、平板和桌面螢幕調整。介面支援 13 種語言，桌面版另提供鍵盤快速鍵。

<details>
<summary><b>完整功能清單</b></summary>

#### 帳號

- 使用 Pixiv 帳號透過 OAuth 登入，或使用網頁 Cookie（PHPSESSID）登入，無須手動輸入權杖。
- 在「設定 → 切換帳戶」新增、切換或移除帳號。各帳號分別儲存瀏覽紀錄、搜尋紀錄、稍後閱讀清單和小說閱讀進度。移除帳號時，預設會保留裝置上的資料。
- 登出前會要求再次確認。

#### 瀏覽

- 首頁以瀑布流版面呈現推薦插畫。
- 最新動態分為「發現」、「收藏」和「追蹤」。
- 排行榜涵蓋每日、每週、每月、男性向、女性向和 AI 生成等類別。
- 首頁、動態、收藏、排行榜和搜尋頁面都可切換插畫與小說檢視，並會記住選擇。
- 可搜尋、清空或自動清理裝置上的插畫與小說瀏覽紀錄。Pixiv Premium 會員也可啟用雲端瀏覽紀錄。

#### 插畫

- 查看作品詳細資訊，包括多張圖片、UGOIRA 動畫和相關作品推薦。
- 預覽圖片時可選擇畫質、縮放圖片，並依原圖比例瀏覽相關作品。
- 可下載原圖或 GIF 動畫、自訂檔名及管理下載佇列。多圖作品可選取部分圖片或全部原圖，一次下載。

#### 小說

- 沉浸式閱讀可調整字級和行距，拖曳捲軸即可移至指定位置。變更排版後仍會保留閱讀位置。
- 查看完整標題、標籤、簡介、發表時間和統計資料。閱讀時可查看系列資訊，並前往作者個人頁面或系列目錄。
- 可在應用程式內放大檢視內文插圖。
- 系列目錄會顯示上次閱讀的章節與進度百分比，可直接接續閱讀。
- 自動儲存並還原閱讀進度，支援 Pixiv 小說書籤，也可將小說匯出成 TXT。

#### AI 翻譯

- 支援 OpenAI、Claude、Gemini 及相容的 API，也可連接區域網路內的服務，並自訂模型、逾時時間及請求參數。
- 可取得可用模型清單、設定整體並行請求數上限，並翻譯內文、標題和簡介。
- 譯文以串流方式逐步顯示，並快取在裝置上，閱讀時可隨時切換原文與譯文。
- 在「稍後閱讀」的翻譯佇列中，可查看工作狀態、重試失敗的工作，或重新產生譯文。

#### 搜尋

- 搜尋插畫、小說和使用者，可依人氣、發表時間等條件排序。
- 可設定預設比對方式、排序和 AI 內容篩選條件。插畫與小說會分別記住上次的排序與篩選設定，重新啟動後仍會保留。
- 搜尋結果可連續捲動或分頁顯示。分頁模式支援前後翻頁及跳至指定頁面。
- 插畫結果可選擇方形縮圖，或依原圖比例排列的瀑布流。小說結果使用較寬的清單與緊湊的標題版面。
- 長按標籤可將其加入收藏或複製到剪貼簿。

#### 使用者與收藏

- 查看作者簡介、個人資料、工作環境、投稿的插畫、漫畫與小說，以及公開收藏。點選頭像可放大檢視，點選小說預覽可直接閱讀。
- 查看追蹤清單，自己的清單可分別查看公開和不公開的追蹤。
- 閱讀及發表留言。
- 收藏插畫和小說、追蹤或取消追蹤作者，並依標籤篩選收藏。

#### 網路與隱私

- 預設使用系統代理伺服器，可改為直接連線或手動設定 HTTP / SOCKS 代理伺服器。儲存後，新連線請求會立即套用設定。Android 和桌面版另提供 ECH 模式，網頁登入可用的連線方式依平台而異。
- 在「網路設定」自訂 App API 的代理網域，並透過「網路自我檢測」檢查 DNS、Pixiv API、官網、登入及圖片下載。
- 可選擇是否顯示 R-18 內容，以及進入搜尋頁面時是否讀取剪貼簿。Android Default、iOS 和桌面版皆可關閉匿名使用統計。
- 可封鎖作品、使用者和標籤，也可依長度和分段數量過濾過長的小說標籤。

#### 設定與資料

- 一般設定包括語言、圖片來源、格狀版面欄數與不公開收藏等。底部導覽列可切換為懸浮液態玻璃或標準樣式。
- 支援深層連結。
- 在「應用程式數據」清除快取、匯入或匯出備份。升級後如有待復原項目，可在「資料復原」逐項重試，也可匯入或匯出舊版資料套件。
- 在「設定 → 關於 → 應用程式記錄」查看本次與上次執行的記錄，可依層級篩選、複製或分享。
- 更新視窗以 Markdown 顯示版本說明，也可在「設定 → 關於」開啟 Telegram 更新頻道。

#### 桌面版

- 可跟隨系統的淺色或深色主題，也可手動選擇。
- 在清單中按 `R` 回到頂端或重新整理，使用方向鍵捲動，按 `Esc` 返回上一頁。

</details>

## 下載

目前的原始碼版本為 **2.7.0**。各平台的安裝檔可在 [GitHub Releases](https://github.com/darriousliu/PiPixiv/releases/latest) 下載。

| 平台 | 系統需求 | 安裝檔 |
| --- | --- | --- |
| Android | Android 8.0 以上 | APK（Default / FOSS），也可透過 [F-Droid](https://f-droid.org/packages/com.mrl.pixiv/) 安裝 |
| iOS | iOS 18 以上 | 未簽署的 IPA，需自行簽署後安裝 |
| Windows | x86_64 | MSI |
| macOS | Apple Silicon（arm64） | DMG |
| Linux | x86_64 | tar.gz |

Android 提供兩種版本。

- **Default** 整合 Firebase 和 Sentry，用於當機回報與匿名使用統計。匿名統計可在設定中關閉。
- **FOSS** 不含統計或當機回報的相依套件。F-Droid 提供此版本。

版本更新也會發布至 Telegram 頻道 [@PiPixivChannel](https://t.me/PiPixivChannel)。

## 架構

PiPixiv 使用 Kotlin Multiplatform 開發，共用程式碼涵蓋使用者介面、業務邏輯、網路和資料儲存，各平台只需保留少量進入點程式碼。共用程式碼會編譯為 Android、iOS（arm64 實機與模擬器）和 JVM 桌面版三類目標。

### 模組劃分

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

圖中列出各層主要的相依關係，箭頭指向所依賴的模組。

| 模組 | 用途 |
| --- | --- |
| `app` | Android 進入點，負責 Default / FOSS 兩種版本的建置、簽署和 Baseline Profile |
| `desktopApp` | 桌面版進入點，使用 Nucleus 封裝 MSI、DMG 和 tar.gz |
| `iosApp`、`iosExport` | Xcode 專案與匯出模組。`iosExport` 透過 Swift Export 將共用程式碼匯出為 `PiPixivKit`，供 Swift 呼叫 |
| `composeApp` | 共用的應用程式框架，整合所有功能模組，組合導覽和依賴注入設定 |
| `feature/*` | 依功能劃分的頁面，包括 `picture`、`novel`、`manga`、`collection`、`setting` 等 |
| `common/ui` | 主題、共用元件、分頁狀態等介面基礎功能 |
| `common/repository` | 資料存取層，整合遠端 API、本機資料庫、網路和 AI 翻譯 |
| `common/datasource-remote` | 使用 Ktorfit 定義的 Pixiv API |
| `common/datasource-local` | 使用 Room 3 的本機資料庫 |
| `common/network` | Ktor 用戶端、代理伺服器和 ECH 支援 |
| `common/ai` | 串接 OpenAI、Claude、Gemini 等翻譯服務 |
| `common/core` | 鍵值儲存、記錄、路由、協程和 ViewModel 等基礎功能 |
| `common/data` | 資料模型與使用者設定 |
| `common/analytics-default`、`common/analytics-foss` | 統計與當機回報有兩套實作，建置時依版本擇一使用 |
| `common/platform-api` | 需由各平台原生實作的介面，例如 iOS 使用 Swift 實作儲存至相簿及處理 ZIP 檔案 |
| `common/icons` | 圖示 |
| `lib_strings` | 13 種語言的字串資源 |

### 使用的技術

| 類別 | 技術 |
| --- | --- |
| 使用者介面 | Compose Multiplatform、Material 3 自適應版面 |
| 導覽 | Navigation 3 |
| 依賴注入 | Koin、Koin Annotations（KSP） |
| 網路 | Ktor、Ktorfit。Android 和桌面版使用 OkHttp 引擎，iOS 使用 Darwin 引擎 |
| ECH | 透過 DoH 取得 ECH 設定，使用 Conscrypt 建立連線（Android、桌面版） |
| 儲存 | Room 3（內建 SQLite）、MMKV（[mmkv-kotlin](https://github.com/darriousliu/mmkv-kotlin)） |
| 圖片 | Coil 3、ZoomImage |
| 基礎函式庫 | Kotlin Coroutines / Flow、kotlinx.serialization、Kermit、FileKit |
| 監控 | Firebase Crashlytics / Analytics、Sentry（僅用於 Default 版本） |
| 桌面版 | Nucleus 提供視窗、WebView 和安裝檔封裝，也可選用 JVM AOT 快取 |
| iOS | Swift Export 匯出 Kotlin 模組，平台專屬功能由 Swift 實作 |

### 開發與建置工具

- **建置慣例外掛程式**　`build-logic` 中的 `pixiv.multiplatform`、`pixiv.multiplatform.compose`、`pixiv.android.application` 和 `pixiv.desktop.compose` 統一各模組的建置設定。
- **相依套件管理**　套件版本集中在 [`gradle/libs.versions.toml`](./gradle/libs.versions.toml)，由 Renovate 自動提出更新。建置 FOSS 版本時會檢查相依關係，確認其中沒有統計套件。
- **持續整合**　每次推送至 `develop` 都會建置所有平台。推送版本標籤後，會建置 Android Default、Android FOSS、iOS、Windows、macOS 和 Linux 共 6 個安裝檔，並附在同一個 Release 中。此外也會檢查多語系資源的一致性與 Fastlane 中繼資料。
- **效能與除錯**　Android 使用 Baseline Profile 改善啟動速度。開發時，桌面版支援 Compose Hot Reload，Android 則支援 HotSwan 熱重載。

## 在本機建置

環境需求如下。

- JDK 25（若本機尚未安裝，Gradle 會透過 Foojay 自動下載工具鏈）
- 支援 Android Gradle Plugin 9.5 的 Android Studio
- 建置 iOS 版需要 macOS 和 Xcode

在本機建置時，建議加上 `-PapplyFirebasePlugins=false` 來建置 FOSS 版本，便無須準備 Firebase 和 Sentry 的設定檔。

```bash
# Android
./gradlew :app:assembleDebug -PapplyFirebasePlugins=false

# 桌面版
./gradlew :desktopApp:run -PapplyFirebasePlugins=false
```

若要執行 iOS 版，請使用 Xcode 開啟 `iosApp/PiPixiv.xcodeproj`。

應用程式版本號記錄在 [`gradle.properties`](./gradle.properties) 和 [iOS 設定檔](./iosApp/Configuration/Config.xcconfig)。

## 參與貢獻

歡迎提出 Issue 或提交 Pull Request。程式碼風格、提交訊息、多語系資源同步和介面驗證等約定，請參閱 [AGENTS.md](./AGENTS.md)。

## GitHub Star 歷程

<a href="https://www.star-history.com/?repos=darriousliu%2FPiPixiv&amp;type=date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=darriousliu/PiPixiv&amp;type=Date&amp;theme=dark" />
    <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=darriousliu/PiPixiv&amp;type=Date" />
    <img alt="PiPixiv 的 GitHub Star 數量變化" src="https://api.star-history.com/svg?repos=darriousliu/PiPixiv&amp;type=Date" width="800" />
  </picture>
</a>

## 致謝

PiPixiv 使用或參考了以下開放原始碼專案。

- [Coil](https://github.com/coil-kt/coil) 提供以 Kotlin Coroutines 為基礎的跨平台圖片載入函式庫。
- [Koin](https://github.com/InsertKoinIO/koin) 是適用於 Kotlin 的輕量級依賴注入框架。
- [Multiplatform Markdown Renderer](https://github.com/mikepenz/multiplatform-markdown-renderer) 提供 Compose Multiplatform 的 Markdown 算繪元件。
- 應用程式內切換語言的方式參考了 [Mihon](https://github.com/mihonapp/mihon)。
- 登入流程參考了 [pixez-flutter](https://github.com/Notsfsssf/pixez-flutter)。
- 桌面版的捲動快速鍵參考了 [Pixiv-MultiPlatform](https://github.com/magic-cucumber/Pixiv-MultiPlatform)。

## 授權條款

本專案採用 [Apache-2.0](./LICENSE) 授權條款。
