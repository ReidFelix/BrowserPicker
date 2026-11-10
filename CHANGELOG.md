# Browser Picker — 更新日志

> **注意**：v1.1DEV ~ v1.5DEV 均为内部开发版本。v1.0 是第一个对外发布的版本（对应内部版本 v1.5DEV）。
> v2.0 是内部测试版本（未对外分发），**v2.1 是第一个用正式密钥签名对外发布的版本**。

## v2.2 — 弹窗消失动画（重做）

### 修复

- **修复弹窗消失动画不自然**（重做，v2.1 的方案无效）。根因是 Compose 的 `Dialog` **从不** dismiss 自己的窗口：
  `DialogWrapper.cancel()` 是个空方法，返回键走 `DialogWrapper$2`（`OnBackPressedCallback`）、点击外部走
  `DialogWrapper.onTouchEvent`，两者都只是直接回调 `onDismissRequest()`；窗口只有在 Activity 销毁时
  才被拆掉。所以 v1/v2.1 用的 `windowExitAnimation` 根本没有机会播放，卡片是瞬间消失的。
  现在改为**由 Compose 自己驱动动画**：淡入淡出用 `Animatable` 控制，遮罩改为画在弹窗窗口内部，
  这样遮罩和卡片会一起淡出；`onDismiss` 在淡出结束后才回调，此时才关闭 Activity

### 变更

- 背景遮罩从 Activity 的 `backgroundDimEnabled` 改为画在弹窗窗口内部（否则会和系统遮罩叠加，
  而且系统遮罩无法跟着卡片一起淡出）；桌面模式因此不再有遮罩
- 弹窗窗口改为铺满屏幕并自行处理点击：卡片上下各有一个弹性空白区域，点它关闭。
  不用「可点击的遮罩盖在卡片下面」是因为 Compose 会把点击派发给所有命中的节点，
  已被消费的事件同样会到达兄弟节点，卡片吞不掉
- 浏览器列表最大高度 400dp → 320dp，保证卡片不会高到把上下空白区挤没

### 构建

- `versionCode` 4 → 5，`versionName` 2.1 → 2.2

---
## v2.1 — 弹窗动画修复

### 修复

- **修复弹窗消失动画不自然**：两个原因叠加——① 透明主题里的 `android:windowAnimationStyle="@null"`
  被 Compose 的 Dialog 窗口一并继承（它是在 Activity 主题上套一层 `DialogWindowTheme` 覆盖层），
  导致弹窗**根本没有进出场动画**，只是闪一下；② `onDismissRequest` 是在退出动画刚开始时就回调的，
  此时直接 `finish()` 会销毁 Activity、把动画从中间打断，看起来像抽搐。
  现在改为：给弹窗窗口配上真实的淡入淡出 + 轻微缩放动画，背景遮罩跟着淡出，
  并在关闭 Activity 前等动画播完
- 等待动画期间若有新链接到达，不再把刚弹出的新弹窗一起关掉

### 构建

- `versionCode` 3 → 4，`versionName` 2.0 → 2.1

---

## v2.0 — 修复与增强（内部测试版本，未对外分发）

### 新增

- **链接可复制**：点击弹窗中的链接即可复制到剪贴板，附带震动反馈与「已复制」提示（复制的是完整原始 URL）
- **一键设为默认浏览器**：主界面新增按钮，按系统版本跳转到最合适的默认浏览器设置页
- **默认浏览器状态**：主界面显示当前是否已经是默认浏览器
- **加载态**：浏览器列表查询期间显示加载指示器，不再先闪一下「未检测到已安装的浏览器」
- **主题图标**：补齐 Android 13+ 的 monochrome 图标层
- **单元测试**：为浏览器筛选、链接格式化、协议判断、链接状态机等纯逻辑补充 29 个单元测试
- **修复弹窗消失动画不自然**：详见上方 v2.1

### 修复

- 修复连续点击两个链接时第二个链接丢失的问题：待处理链接改用 `StateFlow` 驱动，不再 `recreate()` Activity
- 修复弹窗被新链接替换时旧弹窗的 dismiss 回调会误关 Activity 的问题（dismiss 回调增加「是否仍是当前链接」守卫）
- 修复浏览器列表加载期间刷新会把已有列表替换成空状态的问题
- **修复「未检测到已安装的浏览器」**：v2.0 预览版把浏览器发现路径收紧了——`<queries>` 里加了
  `<category>`、查询 intent 里加了 `CATEGORY_DEFAULT`、并把 `MATCH_ALL` 换成了
  `MATCH_DEFAULT_ONLY`。收紧后设备上查不到任何其他浏览器。已把整条发现路径恢复成 v1.0 的
  宽松写法（`<queries>` 只声明 action + scheme，查询 intent 只加 `CATEGORY_BROWSABLE`，
  flags 仍是 `MATCH_ALL`），并加注释说明这些约束，避免以后再被"顺手收紧"
- 空状态新增诊断信息：显示系统实际返回的候选应用数量，便于区分「系统没返回任何应用」和
  「只返回了本应用」
- 修复同一 App 注册多个 Activity 时图标被重复加载的问题
- 移除冗余的 `EXTRA_FORWARDED` 标记，不再把内部标记透传给目标浏览器
- 移除 `onPause` 中的空实现
- 移除未被使用的 DataStore 依赖（同时去掉 4 个 ABI 的 native 库）

### 构建

- **发布包不再使用 debug 构建**：新增 release 签名配置（`keystore.properties`），无配置时产出未签名包而不会回退到 debug 密钥
- 开启资源压缩（`shrinkResources`）
- 正式包体积从 **9.37 MB 降到 809 KB**
- lint 配置化：`abortOnError` + 文本报告
- `versionCode` 1 → 3，`versionName` 1.0 → 2.0

### 文档

- README / Manifest 注释与实际实现对齐（此前 README 提到的 `excludeFromRecents` 实际并未使用，下载链接指向的 APK 也被 `.gitignore` 排除）
- 补充签名生成脚本与密钥备份说明

> ⚠️ **升级提示**：v1.0 是用 debug 密钥签名的，v2.1 起用正式密钥签名。签名不同，
> 已安装 v1.0 的设备需要先卸载旧版再安装 v2.1。

## v1.0 — 正式发布

内部版本 v1.5DEV，首次公开发布。

### 功能
- 注册为 Android 可识别的浏览器应用（http/https intent filter）
- 点击任何链接时弹出 Material 3 风格浏览器选择弹窗
- 使用 `setPackage()` explicit intent 转发 URL，不会再次拦截自身
- 从桌面图标打开时显示已安装浏览器列表及使用说明
- 透明背景弹窗 UI，不干扰原有应用
- 支持 Android 8.0+

### Bug 修复
- 彻底解决 Android 跨应用跳转「允许本次」权限弹窗后跳转失败的问题（Compose Dialog 自动 dismiss 导致发起 Activity 被过早销毁）
- 修复 `setClassName` 参数错误导致目标浏览器无法解析

## 内部开发版本

### v1.5DEV (2026-07-13)

**Bug 修复**
- 终于彻底解决「允许本次」跳转失败。根因：Compose `Dialog` 组件在 `startActivity()` 触发、Activity 切换后台时，系统自动触发 `Dialog.onDismissRequest`，回调中调用了 `finish()`，导致 Activity 被销毁、权限弹窗丢失发起方。修复：`onDismiss` 回调添加守卫 `if (!hasLaunchedBrowser) finish()`，已发起跳转时忽略 dismiss；`onResume` 中检测标记位并清理。

### v1.4DEV

- 未被实际使用，直接迭代到 v1.5

### v1.3DEV

**Bug 修复（不彻底）**
- 改用 `moveTaskToBack(true)` 代替 `finish()`，但导致 Android 12+ 判定为后台应用跳转而拦截权限弹窗。

### v1.2DEV

**Bug 修复**
- 修复「无法打开浏览器」：`setClassName(packageName, packageName)` 第二个参数误传包名，应为完整 Activity 类名，导致 ActivityNotFoundException。改为 `setPackage()` 标准转发方式。

### v1.1DEV

**Bug 修复（不彻底）**
- 将 `try { ... } finally { finish() }` 改为 `window.decorView.post { finish() }`，但延迟仅一帧（~16ms），系统权限弹窗是异步的，修复无效。

### v1.0DEV

**初始版本**
- 存在「允许本次」跳转失败问题。