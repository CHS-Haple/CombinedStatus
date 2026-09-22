# CombinedStatus

**CombinedStatus** is an LSPosed module for Xiaomi HyperOS that combines battery, mobile-network, and Wi-Fi status into a single status-bar indicator.

The project is being rebuilt around explicit SystemUI lifecycle ownership, event-driven state, conservative native-geometry integration, and bounded diagnostics so that new features remain maintainable instead of accumulating scene-specific patches.

> **Status:** pre-release development. The planned initial display version is **0.0.1** and has not yet been formally released.

## Current scope

CombinedStatus currently targets:

- Xiaomi HyperOS;
- `com.android.systemui`;
- Modern Xposed API 102;
- Android 13 / API 33 and later;
- MIUIX 0.9.4 for the companion application.

The current verified compatibility baseline is:

- HyperOS SystemUI `17.03.260226.r`.

Compatibility is validated against the exact target SystemUI rather than inferred from version names alone. Other HyperOS builds or device variants may differ internally and are not assumed compatible without evidence.

## Current capabilities

### SystemUI runtime

- Combined battery, mobile-network, and Wi-Fi presentation for the Home status bar.
- Event-driven state acquisition for battery, Wi-Fi, mobile network, airplane mode, default-data subscription, connectivity, and native tint.
- Verified SystemUI host capture and runtime compatibility checks.
- Modern Xposed hot reload with generation replacement rather than duplicate hook stacking.
- Shared scene and layout-policy models for future Home, notification-shade, Control Center, keyguard, and AOD integration.
- Conservative SystemUI integration: native layout, translation, visibility, and animation ownership are preserved wherever practical.

### Diagnostics

- General and Detailed diagnostics levels independent from build type.
- Bounded lifecycle, compatibility, state, rendering, topology, and geometry diagnostics.
- Built-in feedback report export/share using LSPosed module logs with logcat fallback.
- Explicit, user-confirmed SystemUI restart through bounded Root execution.
- No resident logging service, polling loop, or continuous View-tree sampling.

### Companion app

- MIUIX 0.9.4 interface with Home, Features, and Settings.
- Predictive back and direction-aware swipe-back navigation.
- Light/dark mode and dynamic-color preferences.
- Standard or floating bottom navigation with optional Blur/Glass material.
- English and Simplified Chinese.
- Android 13+ per-app language selection.
- Optional launcher-icon hiding while retaining a non-launcher app entry point.

## Design principles

CombinedStatus follows three project-wide principles:

- **Standardized** — respect Android, HyperOS, MIUIX, and Modern Xposed lifecycle and ownership conventions.
- **Lightweight** — avoid unnecessary polling, duplicate state, hooks, listeners, background work, Root processes, and high-frequency diagnostics.
- **Modern** — prefer maintained platform/library APIs when they fit the lifecycle and compatibility requirements.

SystemUI integration also follows several architectural constraints:

- host-scoped runtime state should remain host-scoped;
- long-lived resources require an explicit owner and cleanup path;
- one live SystemUI property should have one runtime writer;
- native layout geometry, CombinedStatus visual geometry, transition geometry, and optical adjustment are separate responsibilities;
- observation of SystemUI behavior does not automatically grant CombinedStatus ownership of that behavior;
- when safe replacement cannot be established, the module should degrade toward native HyperOS behavior rather than leave a broken partial replacement.

The complete engineering rules for developers and contributors are in [CONTRIBUTING.md](CONTRIBUTING.md).

## Build channels

CombinedStatus uses three build channels:

| Channel | Purpose |
| --- | --- |
| **Debug** | Development probes, assertions, detailed topology/ownership diagnostics, and experimental validation. |
| **Canary** | Daily real-device testing close to Release behavior; non-debuggable and release-optimized while retaining bounded runtime diagnostics. |
| **Release** | Formal distributable build with production diagnostics only. |

Core feature behavior is shared across build channels. Build type controls diagnostic capability, not whether the core CombinedStatus renderer exists.

## Development workflow

- `main` is the stable, installable, validated integration baseline.
- `dev` is the active integration branch.
- `feat/*` is reserved for larger isolated experiments that return to `dev` after validation.

Runtime-sensitive changes require both CI and focused real-device validation. CI success alone is not treated as proof that SystemUI behavior is correct.

Detailed contribution, lifecycle, ownership, migration, changelog, and validation rules are defined in [CONTRIBUTING.md](CONTRIBUTING.md).

## Build requirements

Current project baseline:

| Item | Value |
| --- | --- |
| Package | `com.chaners.combinedstatus` |
| Planned initial display version | `0.0.1` (unreleased) |
| minSdk | 33 |
| compileSdk / targetSdk | 37 |
| JVM | 21 |
| Modern Xposed API | 102 |
| MIUIX | 0.9.4 |
| Kotlin | 2.4.20 |
| Android Gradle Plugin | 9.4.1 |

Build with Android Studio using the Android 17 / API 37 SDK and JDK 21, or use the repository's GitHub Actions workflows.

Test artifacts use a dedicated CI test certificate so compatible builds can update in place. Formal Release signing is isolated from CI test signing, and signing credentials are not stored in the repository.

## Versioning and changelog

The external display version changes only when a formal version is intentionally advanced. Ordinary development iterations use internal build identifiers.

Until the first formal release, [CHANGELOG.md](CHANGELOG.md) keeps a single `[Unreleased]` section describing the **net state intended for 0.0.1**, not the full sequence of experiments used to reach it.

## Terminology

Project-facing text consistently uses:

- English: **mobile network**
- Chinese: **移动网络**
- Internal domain names: `mobileNetwork` / `mobileSignal`

Exact upstream Android/HyperOS API, class, field, method, and resource identifiers keep their original names.
