# 项目协作约定

## 代码风格与提交

- Kotlin 使用 Kotlin 官方代码风格，优先按 Android Studio 的项目 Code Style 格式化本次修改的文件，避免顺带格式化无关文件。
- 保留工作区中已有的用户修改；提交前检查暂存差异和 `git diff --check`。
- 提交信息使用中文，遵循 Conventional Commits，例如 `feat(ui): 统一漫画与发现页面样式`。

## Git Flow 发布流程

后续版本发布统一使用 Git Flow：生产分支为 `master`，开发分支为 `develop`，发布分支为 `release/vX.Y.Z`，正式版本使用注释标签 `vX.Y.Z`。

1. 发布前检查工作区、远端分支及目标标签和 Release 的状态，保留用户已有修改。从最新的 `develop` 创建 `release/vX.Y.Z`；使用 Git Flow 命令时执行 `git flow release start vX.Y.Z`，当前配置的版本标签前缀为空，版本参数本身须包含 `v`。
2. 在发布分支更新并提交版本：同步 `gradle.properties` 的 `versionName` / `versionCode`、`iosApp/Configuration/Config.xcconfig` 的 `MARKETING_VERSION` / `CURRENT_PROJECT_VERSION`，以及 `README.md`、`.github/README-en.md` 的当前源码版本。Gradle 与 iOS 的版本名、构建号须一致，构建号沿用项目现有编码，例如 `2.6.0 → 20600`。提交信息使用 `chore(release): 升级版本至 X.Y.Z`。
3. 完成版本配置检查及受影响的验证，检查暂存差异并运行 `git diff --check`，再将发布分支以 `--no-ff` 合并到 `master`，提交信息使用 `chore(release): 发布 vX.Y.Z`。
4. 在 `master` 的发布合并提交上创建注释标签 `vX.Y.Z`，标签必须指向该发布提交，不直接指向 `develop` 或发布分支的 tip。随后将该标签以 `--no-ff` 合并回 `develop`，提交信息使用 `chore(release): 将 vX.Y.Z 发布结果合并回开发分支`。
5. 正常推送 `master`、`develop` 与版本标签，并核对远端合并关系及标签指向。正常发布不重写已推送的分支历史，已发布的版本标签不得移动或覆盖；已有同名标签或 Release 时先核对其状态和提交，避免重复发布。
6. 按历史 GitHub Release 格式整理中英双语的新功能、问题修复、Issue 处理情况、下载表及完整变更链接，内容以自上一正式版本以来的实际变更为准。`.github/workflows/release.yml` 同时响应 `master` 和 `v*` 标签推送，须根据版本标签和发布提交 SHA 识别正式标签构建；只有标签构建会汇总 Release 附件。
7. 正式发布前等待标签流水线及最终 Release 汇总任务全部成功，核对 Android Default / FOSS APK、iOS IPA、Windows x86_64 MSI、macOS arm64 DMG、Linux x86_64 tar.gz 六个附件均已上传且版本正确。流水线会将 Release 写为草稿并使用 `Release vX.Y.Z` 占位正文，因此须在汇总结束后写回最终双语正文，再正式发布为最新稳定版，避免正文或发布状态被流水线覆盖。
8. 发布后核对公开 Release 的正文、附件和最新稳定版状态，再清理本次临时发布分支，保留正式标签及 `master` / `develop` 的合并记录。构建或发布失败时保留草稿和发布分支，处理问题后继续，不将未完成的发布报告为已完成。

## 测试范围

- 新增或调整测试时，不添加过多防御性测试，只保留与本次改动直接相关的关键边界和实际回归测试，例如配置兼容、重要状态切换与恢复、导航层级等。
- 优先复用现有测试，同一风险保留最小必要覆盖；避免重复断言、无业务差异的逐项穷举、逐个按钮的简单回调验证，以及仅验证框架或序列化库默认行为的测试，不为追求覆盖率扩充用例。
- 简单样式和低风险布局调整通过下方 Compose Preview 流程验证；交互或业务行为变化只补充必要的边界测试，并运行受影响的现有测试。

## 多语言资源同步

- 多语言变更必须覆盖项目支持的全部语言，在同一次改动中完成，不得依赖默认语言回退、空文本或复制其他语言文案来掩盖漏译。当前共 13 种：简体中文（默认 `values`）、繁体中文（`values-zh-rTW`）、英语、日语、韩语、德语、法语、西班牙语、葡萄牙语、俄语、阿拉伯语、印地语、土耳其语。
- 以 `lib_strings/src/commonMain/composeResources/values/` 为资源基准，检查所有 `values-*` 目录下的全部 XML，包括按功能拆分的资源文件，不能只检查 `strings.xml`。仅基准明确标记 `translatable="false"` 的固定名称、协议等资源可免翻译，不得为跳过翻译而添加该标记。
- **新增**资源时，为所有语言补齐同名、同类型且符合对应语言的译文；**修改**原文含义、术语或参数时，逐一审核并同步所有语言，不能只改简中、繁中和英文；**删除或重命名**资源时，同步清理或重命名所有语言中的旧 key，并更新代码引用。若某种语言的现有译文仍然准确，无需强行改写，但应在验证说明中注明已审核及保留原因。
- 各语言应保持格式占位符的编号、类型和数量一致（例如 `%1$s`、`%2$d`），可按语序调整位置；正确保留换行、转义、XML 实体和必要的标记。数组须保持条目对应，复数资源须符合目标语言的复数规则并包含 `other`。
- 涉及 iOS 权限说明等原生文案时，同步检查 `iosApp/iosApp/*.lproj/InfoPlist.strings`。新增或移除语言时，还须同步 Compose 资源、iOS 本地化目录与 Xcode 语言配置，以及 `LanguageHelper.kt` 中的应用语言列表。
- 完成多语言变更后，在项目根目录执行 `python3 scripts/check_localizations.py` 和 `git diff --check`，处理缺失、多余、重复、空译文、占位符和语言列表不一致问题。自动检查只能验证结构，不能判断译文语义是否同步；仍须逐语言审核本次文案变更，并运行相关资源编译检查。涉及 UI 布局调整时，继续遵循下方 Compose Preview 视觉验证要求。

## Compose UI 开发与视觉验证

Composable 函数声明 `modifier: Modifier = Modifier` 时，必须将其作为第一个可选参数（带默认值的参数），放在必填业务参数之后、其他可选参数之前；内容 lambda 可保留在末尾以支持尾随 lambda。调整参数顺序时，同步检查调用点，避免位置参数错位。

新增或调整页面、列表条目、按钮、筛选面板等 UI 时，沿用项目的 Material Expressive 主题、排版、间距、圆角和导航习惯，并通过带 mock 数据的 Compose Preview 检查最终布局。预览代码仅在验证期间临时创建，完成后删除，不纳入提交。

分页页面复用 `ContentPullToRefreshBox`、`PagingContentState` 和 `PagingAppendState`：首载或零数据重试使用页面状态，有内容时刷新使用下拉指示器，追加分页使用底部状态。传入真实 `isRefreshing`，仅控制指示器可见性，避免破坏加载中禁止重复刷新的行为。分页状态项只在需要时加入列表，错误提示应保留已有内容并避开底部按钮。普通空态、异常态复用 `ContentEmptyState` / `ContentErrorState`；空态的操作按钮通过 `action` 插槽放在文案下方。

### 复用真实组件

1. 将依赖 ViewModel、导航、分页或仓库的页面与可预览的内容组件分开。内容组件接收状态和回调，由实际页面与 Preview 共同调用，不单独仿制一套预览 UI。
2. 临时 Preview 使用与实际页面相同的 Material Expressive 浅色/深色主题。可用 Coil 的 `LocalAsyncImagePreviewHandler` 为 mock 地址提供离线封面和头像，不依赖网络、登录、真实账号或 Koin 初始化；验证结束后一起删除这些预览辅助代码。
3. 提供能暴露布局问题的 mock 数据：长标题、长作者名、多行说明、不同封面和数量；根据组件行为补齐空列表、加载、失败、禁用、提交中或屏蔽等状态。
4. 至少检查常规手机布局；对会受尺寸影响的 UI 增加 320dp 窄屏、大字体和深色预览，对大屏页面增加 840dp 预览。使用 `@Preview` 的 `widthDp`、`fontScale`、`uiMode`、`locale` 明确配置；深色预览同时设置 `uiMode = 0x20`，确保截图元数据准确。

临时预览放在能访问实际内容组件的源码包中，并使用独立文件，便于验证结束后清理。不要覆盖或删除任务开始前已有的预览。

### 生成截图

在项目根目录执行。环境需具备带 `android preview` 的 Android CLI、匹配的 Preview renderer pack，以及项目所需的 Android SDK/JDK。首次使用可通过 `android preview --help` 确认可用命令。

```sh
# 列出当前临时预览的完整函数名
android preview list 'gradle://.?app' --all -- \
  -PcomposePreview=true --no-parallel --max-workers=1

# 将以下示例 FQN 替换为本次实际创建的预览；多个预览重复传入 --fqn
python3 scripts/render_ui_review.py \
  --fqn com.mrl.pixiv.example.UiReviewPreviewKt.ExamplePreview

# 使用已有 PNG 和组件树重新生成总览
python3 scripts/render_ui_review.py --gallery-only
```

- 默认输出为 `build/reports/ui-review/`，`index.html` 是截图总览，`images/` 包含 PNG 和组件树 JSON，日志和预览函数清单也保存在该目录。可通过 `--output` 指定独立的报告目录。
- 脚本不维护固定预览清单。每次通过 `--fqn` 选择本次临时预览，避免依赖已清理的预览类。
- 渲染使用 Android app 目标 `gradle://.?app`。KMP feature 模块中的预览也通过 app 汇总；不要直接将 KMP library 作为 Android CLI 的渲染目标。
- 脚本传入 `-PcomposePreview=true --no-parallel --max-workers=1`。`composePreview` 只关闭离线 Layoutlib 无法初始化的 HotSwan 插桩，正常调试构建仍启用热重载。app 的 debug 依赖必须保留 `ui-tooling`。
- 不启动真实应用的 JVM 状态/UI 测试也使用 `-PcomposePreview=true`，避免 HotSwan 的未初始化解释器干扰测试。正常调试编译另行不带该参数验证，不将两种配置混在同一次 Gradle 调用中。
- 保持项目默认 Firebase/analytics 构建配置，不为渲染临时切换构建变体。串行 Gradle 参数用于避免跨平台 metadata 构建依赖缺失。
- 不使用持久 Preview renderer daemon：改动 `@Preview` 配置后可能复用旧参数。脚本每次启动新的渲染进程，并在渲染前清理自身的旧图片输出。

### 审核与收尾

1. 打开总览并逐张查看最终 PNG，检查文字是否截断、重叠，封面比例与留白是否协调，按钮主次是否清楚，点击区域是否合理，以及深色和大字体布局。渲染成功不能代替视觉检查。
2. 预览必须实际展示待检查内容。若自动聚焦或滚动导致错误提示等内容不可见，为真实状态组件补独立 Preview；不要为截图改变生产逻辑，也不要把被遮住的状态算作已审核。
3. 调整 UI 后重新渲染受影响的预览。布局截图不能替代交互、网络和分页验证；根据改动运行相关现有测试，涉及这些行为时补充必要的运行验证，并如实记录未验证范围。
4. 向用户提供截图总览与简洁的验证结果供检查。检查完成后，删除本次新增的 `@Preview` 函数、mock 数据、预览专用主题及辅助代码，同时清理图片、组件树、构建日志、下载包和调试脚本；这些内容不纳入提交。保留生产 UI、通用验证脚本和必要的构建配置。
5. 清理后检查是否存在对预览代码的残留引用，并运行相关编译检查。只清理本次任务生成的文件，不删除其他任务的文件或通用 SDK/Gradle 缓存。下次修改 UI 时重新创建对应的临时预览并重复上述流程。
