# Combined Status

[![Build](https://github.com/CHS-Haple/CombinedStatus/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/CHS-Haple/CombinedStatus/actions/workflows/build.yml)
![Companion app: Android 13+](https://img.shields.io/badge/Companion%20app-Android%2013%2B-3DDC84?logo=android&logoColor=white)
![Modern Xposed API 102](https://img.shields.io/badge/Modern%20Xposed%20API-102-3F51B5)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)
![Status: pre-release](https://img.shields.io/badge/status-pre--release-orange)

**Combined Status** is an LSPosed module for Xiaomi HyperOS that combines battery, mobile-network, and Wi-Fi information into one status-bar indicator while preserving native SystemUI behavior whenever a safe replacement contract is unavailable.

[English](#english) | [简体中文](#简体中文)

---

## English

### Project status

> **Pre-release development.** The active development line is **0.0.2** and the first planned formal release is **1.0.0**. Current 0.0.x builds are development checkpoints, not formal releases.

| Item | Current scope |
| --- | --- |
| Runtime platform | Xiaomi HyperOS |
| Verified SystemUI baseline | `17.03.260226.r` |
| Xposed interface | Modern Xposed API 102 |
| Companion app | Android 13 / API 33+ |
| Verified Combined Status scene | Home status bar |
| Other scenes | Native-only until separately validated |

Compatibility is established against the exact target SystemUI rather than assumed from version names alone. Other HyperOS builds or device variants may differ internally and remain unsupported until validated.

### Highlights

**Status-bar integration**
- Combines battery, mobile-network, and Wi-Fi information into one compact Home status indicator.
- Reacts to authoritative battery, connectivity, SIM/data, airplane-mode, tint, and relevant SystemUI state.
- Uses fail-native behavior: unsupported or incomplete replacement states keep or restore native SystemUI presentation.

**Companion app**
- MIUIX-based Home, Features, and Settings navigation.
- Light/dark appearance, dynamic color, and standard or floating navigation options.
- English and Simplified Chinese with Android 13+ per-app language selection.
- Optional launcher-icon hiding while keeping a non-launcher app entry point.

**Diagnostics**
- General and Detailed diagnostic levels.
- Local diagnostic-report export and Android sharing.
- Explicit, user-confirmed SystemUI restart when Root access is available.
- No resident Root service or project-operated telemetry. See [PRIVACY.md](PRIVACY.md).

### Current development boundary

The Home status bar is the current runtime-verified Combined Status surface. Notification-shade / Control Center transitions, keyguard, and AOD remain native until their own host, lifecycle, motion, cleanup, and device-validation contracts are completed.

The project intentionally prefers native HyperOS/SystemUI state, resources, layout, and animation ownership over duplicate local machinery. Development decisions and rejected routes are documented in the repository rather than hidden behind one-device visual patches.

### Documentation

**Project / user-facing**
- [CHANGELOG.md](CHANGELOG.md) — current unreleased net changes and future release history.
- [PRIVACY.md](PRIVACY.md) — local data, diagnostics, Root, export, and sharing.
- [SECURITY.md](SECURITY.md) — private security-reporting policy.
- [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) — direct dependency and license notices.
- [LICENSE](LICENSE) — Apache License 2.0.

**Development / contribution**
- [CONTRIBUTING.md](CONTRIBUTING.md) — engineering, validation, ownership, CI, and contribution rules.
- [docs/README.md](docs/README.md) — documentation map and authority guide.
- [docs/development/README.md](docs/development/README.md) — current state, roadmap, versioning, and engineering history.
- [docs/architecture/README.md](docs/architecture/README.md) — architecture status and superseded-route boundaries.
- [docs/reference/README.md](docs/reference/README.md) — reusable implementation evidence and usage rules.

---

## 简体中文

### 项目状态

> **预发布开发阶段。** 当前开发版本线为 **0.0.2**，计划首个正式发布版本为 **1.0.0**。现阶段 0.0.x 均为开发检查点，不属于正式发布版本。

| 项目 | 当前范围 |
| --- | --- |
| 运行平台 | Xiaomi HyperOS |
| 已验证 SystemUI 基线 | `17.03.260226.r` |
| Xposed 接口 | Modern Xposed API 102 |
| 配套应用 | Android 13 / API 33+ |
| 已验证三合一场景 | 主状态栏 Home |
| 其他场景 | 在分别完成验证前保持原生 |

兼容性以目标 SystemUI 的实际结构和运行表现为准，而不是仅根据版本号推定。其他 HyperOS 版本或不同机型的内部实现可能不同，在完成验证前不会默认视为兼容。

### 功能概览

**状态栏集成**
- 将电池、移动网络和 Wi-Fi 信息整合为一个紧凑的 Home 状态图标。
- 跟随电池、连接、SIM/数据、飞行模式、Tint 以及相关 SystemUI 原生状态。
- 坚持 Fail native：无法安全替换或状态不完整时保留或恢复系统原生显示。

**配套应用**
- 基于 MIUIX 的 Home、Features、Settings 导航。
- 支持亮色/深色、动态取色，以及标准或悬浮导航。
- 支持英文、简体中文和 Android 13+ 应用级语言选择。
- 可隐藏桌面图标，同时保留非桌面入口。

**诊断**
- General / Detailed 两档诊断等级。
- 本地生成诊断报告，并通过 Android 系统导出或分享。
- 具备 Root 权限时，可由用户明确确认后重启 SystemUI。
- 不使用常驻 Root 服务，也不包含项目运营的遥测服务。详见 [PRIVACY.md](PRIVACY.md)。

### 当前开发边界

主状态栏 Home 是当前已完成运行时验证的 Combined Status 场景。通知栏 / 控制中心过渡、锁屏和 AOD 在各自的 Host、生命周期、动画、清理和实机验证合同完成前保持系统原生行为。

项目优先复用 HyperOS/SystemUI 原生状态、资源、布局和动画所有权，而不是重复创建本地机制。架构决策、被否定路线和验证边界会保留在仓库工程文档中，而不是通过单机型补丁掩盖。

### 文档

**项目 / 用户**
- [CHANGELOG.md](CHANGELOG.md) — 当前未发布净变化与未来发布历史。
- [PRIVACY.md](PRIVACY.md) — 本地数据、诊断、Root、导出与分享说明。
- [SECURITY.md](SECURITY.md) — 安全问题私密报告规则。
- [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) — 直接依赖与许可证说明。
- [LICENSE](LICENSE) — Apache License 2.0。

**开发 / 贡献**
- [CONTRIBUTING.md](CONTRIBUTING.md) — 工程、验证、所有权、CI 与贡献规范。
- [docs/README.md](docs/README.md) — 文档导航与权威关系说明。
- [docs/development/README.md](docs/development/README.md) — 当前状态、路线图、版本规则与工程历史。
- [docs/architecture/README.md](docs/architecture/README.md) — 架构状态与已否定路线边界。
- [docs/reference/README.md](docs/reference/README.md) — 可复用实现证据与使用规则。

---

## Disclaimer / 免责声明

Combined Status is an independent community project and is not affiliated with, endorsed by, or maintained by Xiaomi, HyperOS, LSPosed, or the MIUIX project.

Combined Status 是独立的社区项目，与 Xiaomi、HyperOS、LSPosed 或 MIUIX 项目不存在官方隶属、背书或维护关系。
