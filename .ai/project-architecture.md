---
description: PiPixiv 项目架构与技术选型规范 - 供 AI Agent 深入理解项目结构使用
alwaysApply: true
enabled: true
updatedAt: 2026-03-05T00:00:00.000Z
provider: CodeBuddy
---

# PiPixiv 项目架构与技术选型规范

> 本规范目标：帮助 AI Agent 深入理解 PiPixiv 的架构设计、技术选型及开发约定，确保代码生成与修改与现有代码风格高度一致。
> 工作分支：**develop**

---

## 1. 项目概述

PiPixiv 是一个使用 **Kotlin + Jetpack Compose Multiplatform** 开发的第三方 Pixiv 客户端，支持国内直连（SNI 绕过）。

### 1.1 支持平台

| 平台 | 最低版本 |
|------|---------|
| Android | 8.0 (API 26) |
| iOS | 17.0 |
| Windows | x86_64 |
| macOS | arm64 (Apple Silicon) |
| Linux | x86_64 |

### 1.2 包名

```
com.mrl.pixiv
```

---

## 2. 技术栈选型

### 2.1 核心框架

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.x | 主要开发语言（100% Kotlin） |
| Compose Multiplatform | 1.10.x | 全平台 UI 框架 |
| Koin | 最新版 | 依赖注入框架（KSP 注解驱动） |
| Ktor Client (OkHttp engine) | 最新版 | 网络请求客户端 |
| Ktorfit | 最新版 | 类 Retrofit 的 Ktor 接口声明框架 |
| Paging 3 | 最新版 | 分页加载（Jetpack） |

### 2.2 数据层

| 技术 | 用途 |
|------|------|
| Room | 本地数据库（插图历史、下载记录等） |
| MMKV | 高性能 KV 存储（用户设置、Token 等） |
| Kotlinx Serialization | JSON 序列化/反序列化 |

### 2.3 图片加载

```
Coil 3 (coil3)
  └── KtorNetworkFetcherFactory  // 使用自定义 ImageHttpClient 加载图片
  └── AnimatedImageDecoder / GifDecoder  // GIF 支持
```

**规则**：所有图片加载必须通过 `AsyncImage`（Coil3）完成，使用 `imageHttpClient` 作为网络 fetcher，确保图片请求经过 SNI 绕过处理。

### 2.4 导航框架

- **Navigation 3**（`androidx.navigation3`）：基于 `NavDisplay` + `BackStack` 的类型安全导航
- 所有路由目标定义在 `common/core` 的 `Destination` 密封类中
- `NavigationManager` 作为单例（`@Single`）集中管理导航状态

### 2.5 其他工具库

| 库 | 用途 |
|------|------|
| Kermit | 跨平台日志 |
| kotlinx-datetime | 日期时间处理 |
| kotlinx-collections-immutable | 不可变集合（`ImmutableList`） |
| WorkManager | 后台下载任务 |
| Sonner (Dokar) | Toast/通知组件 |
| Fastlane | 自动化发布 |
| Renovate | 依赖自动更新 |

---

## 3. 项目模块架构

### 3.1 模块结构图

```
PiPixiv/
├── app/                          # Android 入口模块（MainActivity、Navigation、Application）
├── composeApp/                   # 多平台入口模块（iOS/Desktop）
├── iosApp/                       # iOS 原生入口
├── lib_strings/                  # 字符串资源模块（所有平台共享）
├── baselineprofile/              # Baseline Profile 生成
├── build-logic/                  # 自定义 Gradle 构建插件（Convention Plugins）
│
├── common/                       # 公共模块（按职责拆分）
│   ├── core/                     # Activity基类、MVI ViewModel、工具类、路由定义
│   ├── data/                     # 数据模型（DTOs、Constants、Enums）
│   ├── network/                  # Ktor 客户端配置（ApiClient/AuthClient/ImageClient）
│   ├── datasource-remote/        # Ktorfit API 接口声明（PixivApi、AuthApi）
│   ├── repository/               # Repository 层（PixivRepository、AuthManager等）
│   ├── ui/                       # 公共 Compose 组件、主题、UI 扩展
│   └── ...                       # mmkv、database 等其他公共模块
│
└── feature/                      # 功能模块（按功能垂直拆分）
    ├── main/                     # 主界面（Home、Ranking、Latest、Search、Profile）
    ├── picture/                  # 图片详情页
    ├── search/                   # 搜索（含 SearchResult）
    ├── login/                    # 登录（WebView OAuth）
    ├── setting/                  # 设置（网络、文件名格式、App数据等）
    ├── follow/                   # 关注列表
    ├── history/                  # 浏览历史
    ├── artwork/                  # 用户作品列表
    ├── collection/               # 收藏
    ├── comment/                  # 评论
    ├── report/                   # 举报
    └── ...
```

### 3.2 模块依赖规则

```
feature/* → common/repository → common/datasource-remote → common/network
feature/* → common/data
feature/* → common/ui
feature/* → common/core
app → feature/*
app → common/core
```

**禁止**：
- ❌ `common/*` 模块依赖 `feature/*` 模块
- ❌ `feature/*` 之间直接互相依赖（通过 `NavigationManager` 或 `common/core` 间接交互）
- ❌ `app` 模块包含业务逻辑（仅做装配、导航图、Application 初始化）

### 3.3 Gradle 版本目录

项目使用 **多版本目录**（Multi-Version Catalogs）管理依赖：

```
gradle/
├── libs.versions.toml      # 主要三方库
├── androidx.versions.toml  # AndroidX 系列
├── kotlinx.versions.toml   # Kotlin 官方扩展库
└── compose.versions.toml   # Compose 相关
```

**使用规则**：新增依赖必须先加入对应版本目录，再在 `build.gradle.kts` 中引用，禁止直接写 hardcoded 版本号。

---

## 4. MVI 架构规范

### 4.1 核心类

```kotlin
// 所有 ViewModel 必须继承此基类
abstract class BaseMviViewModel<State, Intent : ViewIntent>(
    initialState: State
) : ViewModel()
```

### 4.2 标准模式

每个功能模块的 ViewModel 遵循以下标准结构：

```kotlin
// ✅ 标准 ViewModel 结构
@Stable  // 可选，当 State 是 data object 时加此注解
data class XxxState(
    val loading: Boolean = false,
    // 使用不可变集合：ImmutableList 而非 List
    val items: ImmutableList<Item> = persistentListOf(),
)

// 可选：用于 UI 副作用（如导航事件）
sealed class XxxEvent : SideEffect {
    data object NavigateToDetail : XxxEvent()
}

@KoinViewModel
class XxxViewModel : BaseMviViewModel<XxxState, ViewIntent>(
    initialState = XxxState()
) {
    private fun loadData() {
        launchIO {
            updateState { copy(loading = true) }
            // ...业务逻辑
            updateState { copy(loading = false, items = result.toImmutableList()) }
        }
    }
}
```

### 4.3 状态收集规范

```kotlin
// ✅ 在 Composable 中使用 asState() 扩展函数收集状态
@Composable
fun XxxScreen(viewModel: XxxViewModel = koinViewModel()) {
    val state = viewModel.asState()
    // state.loading, state.items ...
}

// ✅ 收集副作用
LaunchedEffect(Unit) {
    viewModel.sideEffect.collect { effect ->
        when (effect) {
            is XxxEvent.NavigateToDetail -> navigationManager.navigate(...)
            is SideEffect.Error -> ToastUtil.safeShortToast(effect.throwable.message)
        }
    }
}
```

### 4.4 协程规范

| 方法 | 适用场景 |
|------|---------|
| `launchIO { }` | 网络请求、数据库操作 |
| `launchUI { }` | UI 线程操作 |
| `launchCatch { }` | 自定义线程 + 错误处理 |

---

## 5. 网络层规范

### 5.1 三种 HttpClient

| 客户端标识 | 注解 | 用途 |
|-----------|------|------|
| `@ApiClient` | API 请求 | 自动注入 Authorization、Host 头，处理 Token 刷新 |
| `@AuthClient` | 认证请求 | OAuth Token 刷新 |
| `@ImageClient` | 图片加载 | Coil 图片下载，自动替换 imageHost |

### 5.2 IP 直连（国内绕过 SNI 嗅探）

```kotlin
// Constants.kt 中定义 hostMap
val hostMap: Map<String, String> = mapOf(
    API_HOST to "210.140.139.155",
    AUTH_HOST to "210.140.139.155",
    IMAGE_HOST to "210.140.92.144",
    STATIC_IMAGE_HOST to "210.140.92.143",
    "doh" to "doh.dns.sb",
)
```

- 用户可在设置中开启/关闭"绕过 SNI 嗅探"
- 开启时使用 IP 直连，关闭时使用域名（`enableBypassSniffing` 配置）
- `NetworkUtil.hostnameVerifier` 自定义 SSL 证书验证以支持 IP 直连

### 5.3 Ktorfit API 接口

```kotlin
// ✅ 在 common/datasource-remote 中声明接口
interface PixivApi {
    @GET("v1/illust/recommended")
    suspend fun getIllustRecommended(
        @Query("filter") filter: String,
        @Query("include_ranking_illusts") includeRankingIllusts: Boolean,
        @Query("include_privacy_policy") includePrivacyPolicy: Boolean,
    ): IllustRecommendedResp
}

// ✅ 在 PixivRepository 中调用
object PixivRepository : KoinComponent {
    suspend fun getIllustRecommended(...) = apiApi.getIllustRecommended(...)
}
```

**规则**：
- ❌ 禁止在 ViewModel 或 Screen 中直接调用网络 API
- ✅ 所有网络请求必须通过 `PixivRepository` 或对应的 `XxxRepository` 访问
- ❌ 禁止在 Repository 之外硬编码 API URL

### 5.4 认证 Header 格式

```
User-Agent: PixivAndroidApp/6.158.0 (Android {OS_VERSION}; {DEVICE_MODEL})
X-Client-Time: {ISO8601_DATETIME}
X-Client-Hash: MD5(X-Client-Time + HashSalt)
Authorization: Bearer {accessToken}
```

---

## 6. 导航规范

### 6.1 Destination 定义

```kotlin
// ✅ 所有路由目标必须在 Navigation.kt 中的 Destination 密封类中���明
@Serializable
sealed class Destination : NavKey {
    @Serializable
    data class ProfileDetail(val userId: Long) : Destination()

    @Serializable
    data object Setting : Destination()
}
```

**规则**：
- 路由参数必须可序列化（`@Serializable`）
- 禁止在路由参数中传递大型对象（如 `Illust` 列表）
- 大型对象通过 `IllustCacheRepo`（内存缓存）+ 索引 key 传递

### 6.2 NavigationManager 使用

```kotlin
// ✅ 在 Screen 或 ViewModel 中通过 Koin 注入 NavigationManager
val navigationManager = koinInject<NavigationManager>()

// ✅ 使用具名方法导航（不要直接调用 backStack.navigate）
navigationManager.navigateToProfileDetailScreen(userId)
navigationManager.navigateToSearchResultScreen(keyword)
navigationManager.popBackStack()
```

**禁止**：
- ❌ 直接操作 `navigationManager.backStack`（除在 `NavigationManager` 内部实现时）
- ❌ 在 `common/*` 模块中定义具体的 Screen 导航逻辑

---

## 7. Compose UI 规范

### 7.1 组件设计原则

```kotlin
// ✅ Screen 级 Composable：顶层，通过 koinViewModel/koinInject 获取依赖
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
    navigationManager: NavigationManager = koinInject(),
) { ... }

// ✅ 子组件：无业务逻辑，通过参数传入数据和回调
@Composable
fun IllustItem(
    illust: Illust,
    onIllustClick: (Illust) -> Unit,
    modifier: Modifier = Modifier,
) { ... }
```

### 7.2 状态列表规范

```kotlin
// ✅ 使用 ImmutableList 避免不必要的 Recompose
val items: ImmutableList<Illust> = persistentListOf()

// ✅ 转换
result.toImmutableList()

// ❌ 禁止在 UI State 中使用普通 List
val items: List<Illust> = emptyList()  // 不推荐
```

### 7.3 主题规范

```kotlin
// ✅ 颜色使用 MaterialTheme
MaterialTheme.colorScheme.primary
MaterialTheme.colorScheme.surface

// ✅ 深色模式支持
isSystemInDarkTheme()

// ❌ 禁止硬编码颜色
Color(0xFF123456)  // 禁止
```

### 7.4 公共 UI 工具

```kotlin
// 防抖点击
Modifier.throttleClick { ... }
// 节流点击（Composable 版本）
val onClick = rememberThrottleClick { ... }

// Spacer 扩展
8.HSpacer   // 水平间距 8.dp
16.VSpacer  // 垂直间距 16.dp
```

---

## 8. 依赖注入规范（Koin）

### 8.1 注解驱动

```kotlin
// ✅ ViewModel 使用 @KoinViewModel
@KoinViewModel
class HomeViewModel : BaseMviViewModel<...>(...) { ... }

// ✅ 单例 Service/Repository 使用 @Single
@Single(binds = [NetworkFeature::class])
class NetworkFeatureImpl : NetworkFeature { ... }

// ✅ 工厂（每次创建新实例）使用 @Factory
@Factory
class SomePagingSource(...) : PagingSource<...>() { ... }
```

### 8.2 命名限定符

```kotlin
// 注入带 @Named 限定符的 bean
val apiClient by inject<HttpClient>(named<ApiClient>())
val imageClient by inject<HttpClient>(named<ImageClient>())
```

### 8.3 Composable 中注入

```kotlin
// ViewModel
val viewModel: HomeViewModel = koinViewModel()

// 普通单例
val navigationManager = koinInject<NavigationManager>()

// Activity 作用域 ViewModel
val splashViewModel: SplashViewModel = activityKoinViewModel()
```

---

## 9. 数据层规范

### 9.1 MMKV 存储

```kotlin
// ✅ 继承 MMKVUser 接口使用代理属性
object SettingRepository : MMKVUser {
    private val _userPreference by mmkvDelegate(UserPreference.getDefaultInstance())
    val userPreferenceFlow = _userPreference.asMutableStateFlow()
}
```

### 9.2 Room 数据库

- 数据库名：`pixiv.db`
- 所有 DAO 操作通过 `PixivDatabase` 注入
- 历史记录、下载记录、屏蔽记录等持久化数据使用 Room

### 9.3 Paging Source 规范

```kotlin
// ✅ PagingSource 通过 Koin @Factory 或构造函数注入，不直接 new
class IllustRecommendedPagingSource : PagingSource<Map<String, String>, Illust>() {
    override suspend fun load(params: LoadParams<Map<String, String>>): LoadResult<...> {
        return try {
            val resp = if (params.key == null) {
                PixivRepository.getIllustRecommended(...)
            } else {
                PixivRepository.loadMoreIllustRecommended(params.key!!)
            }
            LoadResult.Page(data = resp.illusts, prevKey = null, nextKey = resp.nextUrl?.parseQueryParams())
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
```

---

## 10. 字符串与国际化

所有字符串资源统一放在 `lib_strings` 模块中，通过类型别名访问：

```kotlin
typealias RString = com.mrl.pixiv.strings.R.string
typealias RDrawable = com.mrl.pixiv.strings.R.drawable
typealias RXml = com.mrl.pixiv.strings.R.xml
```

**规则**：
- ❌ 禁止在代码中 hardcode UI 展示字符串
- ✅ 所有用户可见文本必须定义在 `lib_strings` 资源文件中
- ✅ 使用 `stringResource(RString.xxx)` 在 Composable 中引用

---

## 11. 构建变体（Build Variants）

项目包含 **FOSS** 变体（无 Firebase/Analytics）：

```
src/
├── main/          # 通用代码
├── foss/          # FOSS 变体（空实现 Firebase、Kotzilla 等）
└── google/        # Google Play 变体（含 Firebase Analytics）
```

**规则**：
- 所有分析/崩溃上报类操作必须有 `foss` 空实现
- `logEvent()`、`logException()` 等在 foss 变体中为 no-op

---

## 12. 代码质量规范

### 12.1 文件组织

- 每个 Screen 对应一个 ViewModel，文件放在同一 feature 模块下
- Screen 文件命名：`XxxScreen.kt`
- ViewModel 文件命名：`XxxViewModel.kt`
- PagingSource 放在 `common/repository/src/main/kotlin/.../paging/` 目录

### 12.2 命名约定

| 类型 | 命名模式 | 示例 |
|------|---------|------|
| State | `XxxState` | `HomeState`, `LoginState` |
| Action/Intent | `XxxAction` | `HomeAction : ViewIntent` |
| SideEffect/Event | `XxxEvent` | `LoginEvent : SideEffect` |
| ViewModel | `XxxViewModel` | `HomeViewModel` |
| Screen | `XxxScreen` | `HomeScreen` |
| Repository | `XxxRepository` | `PixivRepository` |
| PagingSource | `XxxPagingSource` | `IllustRecommendedPagingSource` |

### 12.3 TAG 扩展

```kotlin
// ✅ 使用 TAG 扩展属性获取类名
val Any.TAG: String
    get() = this::class.simpleName ?: "TAG"
```

---

## 附录：常用模块路径速查

| 功能 | 路径 |
|------|------|
| MVI 基类 | `common/core/.../viewmodel/BaseMviViewModel.kt` |
| 路由定义 | `common/core/.../router/Navigation.kt` |
| 导航管理 | `common/core/.../router/NavigationManager.kt` |
| API 接口 | `common/datasource-remote/.../PixivApi.kt` |
| 网络常量 | `common/data/.../data/Constants.kt` |
| 全局 Repository | `common/repository/.../PixivRepository.kt` |
| 网络客户端 | `common/network/.../KtorClient.kt` |
| 主导航图 | `app/.../navigation/Navigation3MainGraph.kt` |
| 主界面 | `feature/main/.../MainScreen.kt` |