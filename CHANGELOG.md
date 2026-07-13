# Browser Picker — 更新日志

> **注意**：v1.1DEV ~ v1.5DEV 均为内部开发版本。对外发布的第一个正式版本为 **v1.0**（对应内部版本 v1.5DEV）。

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
