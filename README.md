# CombinedStatus

[![Build](https://github.com/CHS-Haple/CombinedStatus/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/CHS-Haple/CombinedStatus/actions/workflows/build.yml)
![Android 13+](https://img.shields.io/badge/Android-13%2B-3DDC84?logo=android&logoColor=white)
![Modern Xposed API 102](https://img.shields.io/badge/Modern%20Xposed%20API-102-3F51B5)
![Status: pre-release](https://img.shields.io/badge/status-pre--release-orange)

[English](#english) | [简体中文](#简体中文)

---

## English

**CombinedStatus** is an LSPosed module for Xiaomi HyperOS that combines battery, mobile network, and Wi-Fi status into a single status-bar indicator.

> **Status:** pre-release development. The planned initial display version is **0.0.1** and has not yet been formally released.

### Compatibility

Current verified baseline:

- Xiaomi HyperOS
- HyperOS SystemUI `17.03.260226.r`
- Android 13 / API 33 or later for the companion app
- Modern Xposed API 102

Compatibility is verified against the actual target SystemUI. Other HyperOS versions or device variants may differ internally and are not assumed compatible until they are validated.

### Features

#### Status-bar module

- Combines battery, mobile network, and Wi-Fi information into one indicator in supported status-bar scenes.
- Responds to relevant system state changes such as battery, connectivity, airplane mode, default-data subscription, and SystemUI tint.
- Preserves native SystemUI behavior wherever a safe replacement is not available.

#### Companion app

- MIUIX-based interface with Home, Features, and Settings pages.
- Light and dark appearance options with dynamic color support.
- Standard or floating bottom navigation with optional Blur/Glass material.
- English and Simplified Chinese.
- Android 13+ per-app language selection.
- Optional launcher-icon hiding while retaining access to the app.

#### Diagnostics

- General and Detailed diagnostic levels.
- Built-in feedback report export and sharing.
- Explicit, user-confirmed SystemUI restart when Root access is available.
- Diagnostic behavior and local data handling are documented in [PRIVACY.md](PRIVACY.md).

### Current limitations

CombinedStatus is still pre-release software.

- The Home status-bar stable scene is the current runtime-verified CombinedStatus rendering baseline.
- Notification-shade transitions, Control Center, keyguard, and AOD remain native-only unless separately validated.
- Compatibility is currently verified only against the SystemUI baseline listed above.
- Wider device, system-version, and scene support requires separate real-device validation.

### Documentation

- [CONTRIBUTING.md](CONTRIBUTING.md) — development, contribution, validation, lifecycle, and ownership rules.
- [CHANGELOG.md](CHANGELOG.md) — unreleased and released changes.
- [PRIVACY.md](PRIVACY.md) — local data, diagnostics, Root, export, and sharing behavior.
- [SECURITY.md](SECURITY.md) — private security-reporting policy.
- [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) — direct third-party dependencies and license notices.
- [docs/architecture](docs/architecture) — architecture notes for developers and contributors.

---

## 简体中文

**CombinedStatus** 是一个面向 Xiaomi HyperOS 的 LSPosed 模块，用于将电池、移动网络和 Wi-Fi 状态整合为一个状态栏图标。

> **当前状态：** 尚处于预发布开发阶段。计划中的首个外显版本为 **0.0.1**，目前尚未正式发布。

### 兼容性

当前已验证基线：

- Xiaomi HyperOS
- HyperOS SystemUI `17.03.260226.r`
- 配套应用需要 Android 13 / API 33 或更高版本
- Modern Xposed API 102

兼容性以目标 SystemUI 的实际结构和运行时表现为准。其他 HyperOS 版本或不同机型的内部实现可能存在差异，在完成验证前不会默认视为兼容。

### 功能

#### 状态栏模块

- 在已支持的状态栏场景中，将电池、移动网络和 Wi-Fi 信息整合为一个图标。
- 响应电池、连接状态、飞行模式、默认数据卡和 SystemUI Tint 等相关系统状态变化。
- 在无法安全替换时，优先保留 SystemUI 原生行为。

#### 配套应用

- 基于 MIUIX 的 Home、Features 和 Settings 页面。
- 支持亮色、深色外观与动态取色。
- 支持标准或悬浮底部导航，以及可选的 Blur / Glass 材质。
- 支持英文和简体中文。
- Android 13+ 支持应用级语言选择。
- 可选择隐藏桌面图标，同时保留应用入口。

#### 诊断

- 提供 General / Detailed 两档诊断等级。
- 内置反馈报告导出与分享。
- 在具备 Root 权限时，可由用户明确确认后重启 SystemUI。
- 诊断行为与本地数据处理说明见 [PRIVACY.md](PRIVACY.md)。

### 当前限制

CombinedStatus 目前仍处于预发布阶段。

- 主状态栏稳态是当前已完成运行时验证的 CombinedStatus 渲染基线。
- 通知栏过渡、控制中心、锁屏和 AOD 在未单独完成验证前保持原生行为。
- 当前兼容性仅针对上方列出的 SystemUI 基线完成验证。
- 更多机型、系统版本与场景支持需要分别进行实机验证。

### 相关文档

- [CONTRIBUTING.md](CONTRIBUTING.md) — 开发、贡献、验证、生命周期与所有权规范。
- [CHANGELOG.md](CHANGELOG.md) — 未发布与已发布版本的变化。
- [PRIVACY.md](PRIVACY.md) — 本地数据、诊断、Root、导出与分享说明。
- [SECURITY.md](SECURITY.md) — 安全问题私密报告规则。
- [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) — 直接第三方依赖与许可证说明。
- [docs/architecture](docs/architecture) — 面向开发者与贡献者的架构说明。

---

## Disclaimer / 免责声明

CombinedStatus is an independent community project and is not affiliated with, endorsed by, or maintained by Xiaomi, HyperOS, LSPosed, or the MIUIX project.

CombinedStatus 是独立的社区项目，与 Xiaomi、HyperOS、LSPosed 或 MIUIX 项目不存在官方隶属、背书或维护关系。
