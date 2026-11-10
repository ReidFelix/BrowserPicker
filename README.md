# Browser Picker

[![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen)](https://developer.android.com) [![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue)](https://kotlinlang.org) [![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-purple)](https://developer.android.com/compose) [![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

> Android 系统的默认浏览器只能选一个，每次点击链接都自动用同一个浏览器打开，想换就得去设置里改。**Browser Picker** 解决了这个问题——将它设为默认浏览器后，每次点击链接都会弹出一个选择窗口，让你自由选择用哪个浏览器打开。

---

## ✨ 功能

- **链接拦截**：注册为 Android 系统浏览器，拦截所有 http/https 链接
- **每次选择**：点击链接时弹出浏览器列表，选择后跳转——不提供"始终"，真正每次都让你选
- **一键复制**：弹窗里的链接可以点击复制，方便发到别处
- **智能转发**：使用 `setPackage()` 发送 explicit intent，URL 直达目标浏览器，不会再次触发本应用
- **一键设为默认**：应用内直接跳转系统设置，不用自己去设置里翻
- **桌面入口**：保留启动器图标，打开后显示已安装浏览器列表和使用说明
- **轻量无感**：透明背景弹窗 + 跳转后立即退出任务，不打扰、不常驻后台

---

## 🚀 使用方法

1. 从 [Releases](https://github.com/ReidFelix/BrowserPicker/releases) 下载最新的 `BrowserPicker-v*.apk` 安装到手机
   （当前版本：[BrowserPicker-v2.2.apk](https://github.com/ReidFelix/BrowserPicker/releases/download/v2.2/BrowserPicker-v2.2.apk)）
2. 打开应用，点 **设为默认浏览器**（或手动进入 `设置 → 应用 → 默认应用 → 浏览器` 选择 **Browser Picker**）
3. 从任何应用点击链接，弹出浏览器选择界面

> **升级提示**：v1.0 及更早版本是用 debug 密钥签名的，v2.1 起改用正式密钥签名。两者签名不同，
> 已安装 v1.0 的设备**需要先卸载旧版**再安装 v2.1。

---

## 📁 项目结构

```
BrowserPicker/
├── app/
│   ├── build.gradle.kts                         # 应用模块、依赖与 release 签名配置
│   ├── lint.xml                                 # lint 规则例外
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml              # 浏览器 intent filter + 启动器注册
│       │   ├── java/com/example/browserpicker/
│       │   │   ├── BrowserInterceptorActivity.kt # 核心 Activity：拦截 URL、展示 UI、转发跳转
│       │   │   ├── BrowserListViewModel.kt      # 查询已安装浏览器（排除自身）
│       │   │   ├── core/
│       │   │   │   ├── BrowserSelection.kt      # 浏览器筛选/去重/排序（纯逻辑，可单测）
│       │   │   │   ├── DefaultBrowser.kt        # 默认浏览器状态与跳转系统设置
│       │   │   │   ├── LinkRequestState.kt      # 待处理链接的状态机（可单测）
│       │   │   │   ├── UrlFormatter.kt          # 链接展示格式（纯逻辑，可单测）
│       │   │   │   └── WebUrl.kt                # 支持的协议判断（纯逻辑，可单测）
│       │   │   ├── model/
│       │   │   │   ├── BrowserInfo.kt           # 数据类：包名、名称、图标
│       │   │   │   └── BrowserListUiState.kt    # 浏览器列表状态（加载中 / 已加载）
│       │   │   └── ui/
│       │   │       ├── BrowserIcon.kt           # 统一的图标渲染
│       │   │       ├── BrowserPickerDialog.kt   # 链接模式弹窗（含链接复制）
│       │   │       ├── MainScreen.kt            # 桌面模式主界面
│       │   │       └── theme/
│       │   │           └── Theme.kt              # Material 3 配色
│       │   └── res/
│       │       ├── drawable/
│       │       │   ├── ic_launcher_foreground.xml
│       │       │   └── ic_launcher_monochrome.xml   # Android 13+ 主题图标
│       │       ├── mipmap-anydpi-v26/ic_launcher.xml
│       │       └── values/
│       │           ├── colors.xml
│       │           ├── strings.xml
│       │           └── themes.xml               # 透明主题
│       └── test/java/com/example/browserpicker/core/   # 单元测试
├── scripts/create-release-keystore.ps1           # 一键生成 release 签名密钥
├── keystore.properties.example                  # 签名配置模板
├── build.gradle.kts                              # 根构建配置
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/
├── CHANGELOG.md                                  # 版本更新日志
└── README.md                                     # 本文件
```

---

## 🔧 技术栈

| 层面 | 技术 | 说明 |
|------|------|------|
| 语言 | Kotlin 2.0 | JVM target 17 |
| UI | Jetpack Compose + Material 3 | Dialog 弹窗 + Scaffold 主界面 |
| 生命周期 | ViewModel + StateFlow | 响应式浏览器列表查询 |
| 构建 | Gradle 8.7 (Kotlin DSL) | AGP 8.5.1 |
| 兼容性 | Android 8.0 + | minSdk 26, targetSdk 34 |

**核心依赖**：Compose BOM 2024.06、Lifecycle ViewModel Compose、Core KTX、Material Icons Core

---

## ⚙️ 原理

### 1. 注册为浏览器

在 `AndroidManifest.xml` 声明处理 `ACTION_VIEW` + `http/https` + `BROWSABLE`，Android 系统将其识别为浏览器候选：

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="http" />
    <data android:scheme="https" />
</intent-filter>
```

> 声明了 http/https 的 `BROWSABLE` intent filter，应用就自动成为系统「浏览器」角色的候选者，
> 会出现在 `设置 → 默认应用 → 浏览器` 的列表里，并支持 `RoleManager.createRequestRoleIntent()`。

### 2. 查询浏览器

`PackageManager.queryIntentActivities()` 查询所有能处理 `ACTION_VIEW` 的应用，过滤排除自身包名。
先按包名去重再加载图标，避免一个 App 注册多个 Activity 时重复解码图标。

> ⚠️ **这条查询必须保持宽松**：`<queries>` 里不要加 `<category>`，查询 intent 里只加
> `CATEGORY_BROWSABLE`，flags 用 `MATCH_ALL`。收紧任何一处，系统就只会返回「当前默认浏览器」
> 这一个候选——而那正是本应用自己，过滤自身后列表会变成空的（表现为「未检测到已安装的浏览器」）。

### 3. 转发 URL（避免循环）

```kotlin
val intent = Intent(Intent.ACTION_VIEW, uri).apply {
    setPackage(browser.packageName)          // explicit intent，不会触发本应用的 filter
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
startActivity(intent)
```

### 4. 生命周期

- `singleTask` 启动模式：同一实例，新链接走 `onNewIntent()`
- 待处理的链接保存在 `MutableStateFlow` 中，**不重建 Activity**，新链接直接换掉弹窗内容
- 跳转后 Activity 保持存活，直到用户按返回时 `onResume` 中 `finish()`
- `onDismiss` 加双重守卫：已发起跳转、或该弹窗已被新链接替换时，都忽略 Compose Dialog 的自动 dismiss

### 5. 链接复制

弹窗里的链接是一块可点击区域，点击后写入系统剪贴板，并震动反馈 + 显示「已复制」1.8 秒。
复制的是**原始完整 URL**，界面上显示的是去掉协议头和结尾斜杠的紧凑版本。

---

## 🛠 构建

```bash
# 前提：JDK 17 + Android SDK (API 34)
echo "sdk.dir=C\:\\Android" > local.properties

# 调试包
./gradlew assembleDebug          # APK → app/build/outputs/apk/debug/app-debug.apk

# 单元测试
./gradlew testDebugUnitTest

# 静态检查
./gradlew lint

# 正式包（需要先配置签名，见下）
./gradlew assembleRelease        # APK → app/build/outputs/apk/release/app-release.apk
```

### 发布签名

正式包必须用正式密钥签名，**不能**用 debug 密钥。仓库里不包含任何密钥：

```bash
# 1. 生成密钥 + keystore.properties（两者都已在 .gitignore 中）
./scripts/create-release-keystore.ps1

# 2. 打正式包
./gradlew assembleRelease
```

> ⚠️ **务必备份** `browserpicker-release.jks` 和 `keystore.properties`。
> Android 的升级安装要求新旧包签名一致，密钥丢了就无法再给已安装的用户推送更新了。

---

## 📄 许可证

MIT License

---

## 🤝 贡献

欢迎提 Issue 和 PR。本项目目标保持轻量专注——只做"选择浏览器跳转"这一件事。