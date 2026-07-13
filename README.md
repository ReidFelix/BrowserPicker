# Browser Picker

[![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen)](https://developer.android.com) [![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue)](https://kotlinlang.org) [![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-purple)](https://developer.android.com/compose) [![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

> Android 系统的默认浏览器只能选一个，每次点击链接都自动用同一个浏览器打开，想换就得去设置里改。**Browser Picker** 解决了这个问题——将它设为默认浏览器后，每次点击链接都会弹出一个选择窗口，让你自由选择用哪个浏览器打开。

---

## ✨ 功能

- **链接拦截**：注册为 Android 系统浏览器，拦截所有 http/https 链接
- **每次选择**：点击链接时弹出浏览器列表，选择后跳转——不提供"始终"，真正每次都让你选
- **智能转发**：使用 `setPackage()` 发送 explicit intent，URL 直达目标浏览器，不会再次触发本应用
- **桌面入口**：保留启动器图标，打开后显示已安装浏览器列表和使用说明
- **轻量无感**：透明背景弹窗 + `excludeFromRecents`，不打扰、不常驻后台

---

## 🚀 使用方法

1. 下载 [BrowserPicker-v1.0.apk](https://github.com/ReidFelix/BrowserPicker/releases/download/v1.0/BrowserPicker-v1.0.apk) 安装到手机
2. 进入 `设置 → 应用 → 默认应用 → 浏览器`，选择 **Browser Picker**
3. 从任何应用点击链接，弹出浏览器选择界面

---

## 📁 项目结构

```
BrowserPicker/
├── app/
│   ├── build.gradle.kts                         # 应用模块与依赖配置
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml                  # 浏览器 intent filter + 启动器注册
│       ├── java/com/example/browserpicker/
│       │   ├── BrowserInterceptorActivity.kt    # 核心 Activity：拦截 URL、展示 UI、转发跳转
│       │   ├── BrowserListViewModel.kt          # 查询已安装浏览器（排除自身）
│       │   ├── model/
│       │   │   └── BrowserInfo.kt               # 数据类：包名、名称、图标
│       │   └── ui/
│       │       ├── BrowserPickerDialog.kt       # 链接模式弹窗
│       │       ├── MainScreen.kt                # 桌面模式主界面
│       │       └── theme/
│       │           └── Theme.kt                  # Material 3 配色
│       └── res/
│           ├── drawable/ic_launcher_foreground.xml
│           ├── mipmap-anydpi-v26/ic_launcher.xml
│           └── values/
│               ├── colors.xml
│               ├── strings.xml
│               └── themes.xml                   # 透明主题
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

**核心依赖**：Compose BOM 2024.06、Lifecycle ViewModel Compose、Core KTX

---

## ⚙️ 原理

### 1. 注册为浏览器

在 `AndroidManifest.xml` 声明处理 `ACTION_VIEW` + `http/https` + `BROWSABLE`，Android 系统将其识别为浏览器候选：

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="http" />
    <data android:scheme="https" />
</intent-filter>
```

### 2. 查询浏览器

`PackageManager.queryIntentActivities()` 查询所有能处理 `ACTION_VIEW` 的应用，过滤排除自身包名。

### 3. 转发 URL（避免循环）

```kotlin
val intent = Intent(Intent.ACTION_VIEW, uri).apply {
    setPackage(browser.packageName)          // explicit intent，不会触发本应用的 filter
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    putExtra(EXTRA_FORWARDED, true)          // 标记位：防止 onNewIntent 误拦截
}
startActivity(intent)
```

### 4. 生命周期

- `singleTask` 启动模式：同一实例，新链接走 `onNewIntent()`
- 跳转后 Activity 保持存活，直到用户按返回时 `onResume` 中 `finish()`
- `onDismiss` 加守卫：已发起跳转后忽略 Compose Dialog 的自动 dismiss，避免误销毁

---

## 🛠 构建

```bash
# 前提：JDK 17 + Android SDK (API 34)

echo "sdk.dir=C\:\\Android" > local.properties
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 许可证

MIT License

---

## 🤝 贡献

欢迎提 Issue 和 PR。本项目目标保持轻量专注——只做"选择浏览器跳转"这一件事。
