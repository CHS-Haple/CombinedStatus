# CombinedStatus

**CombinedStatus for HyperOS** is a status-bar module built specifically for Xiaomi HyperOS. It combines battery, mobile network, and Wi-Fi status into a single indicator while preserving native HyperOS SystemUI layout and transition behavior.

## Target platform

- Xiaomi HyperOS
- HyperOS SystemUI
- LSPosed module architecture
- MIUIX application interface

## Terminology

Project-facing terminology uses **mobile network / 移动网络** consistently. Internal domain names should use `mobileNetwork` or `mobileSignal`; exact Android/HyperOS API and class identifiers keep their upstream names.

## Current milestone

The project now has a modern Xposed API 102 module baseline in addition to its application shell and navigation layer. The Android app uses MIUIX 0.9.4, a type-safe MIUIX navigation stack, adaptive launcher icons, localized resources, and reproducible CI signing. The module is statically scoped only to `com.android.systemui` and its compatibility baseline is derived only from the target SystemUI APK. After the one-shot compatibility probe, it installs one read-only lifecycle hook that captures the primary HyperOS status-bar host after inflation. The hook does not alter layout, measurement, translation, visibility, or drawing.

The top-level interface is organized as Home, Features, and Settings. Deeper settings pages use the MIUIX navigation runtime with standard transitions, system predictive back, and direction-aware swipe-back gestures. Appearance preferences are persisted with Jetpack DataStore and can control theme mode, optional MIUIX blur on the official floating navigation bar, and in-app swipe-back behavior. Android 13+ per-app language preferences are handled by the platform LocaleManager, and the launcher entry can be hidden without disabling the main activity or its non-launcher front door.

- Package: com.chaners.combinedstatus
- Display version: 0.0.1
- Android: minSdk 33, compileSdk 37.0, targetSdk 37
- JVM: 21
- MIUIX: 0.9.4
- Kotlin: 2.4.20
- Android Gradle Plugin: 9.4.1
- Languages: English, Simplified Chinese
- Minimum Android version: Android 13 / API 33

The current build follows the system language and uses a compile-only modern Xposed API 102 dependency. Its SystemUI integration observes the primary status-bar host after inflation. Debug builds can additionally perform a bounded, one-shot, read-only topology inventory for native status views and containers; release builds keep only low-frequency operational diagnostics. Neither diagnostics mode modifies SystemUI geometry or visual state. API 102 hot reload is enabled with a single Java entry and hook migration between module generations. The app also provides an explicit Root-confirmed action to restart the static SystemUI scope when a full process refresh is required. It does not register background services or request additional permissions.

## Diagnostics

Core module behavior is shared between debug and release builds. Release builds retain basic low-frequency diagnostics for version, compatibility, lifecycle, hot reload, and errors. Debug builds add detailed status-bar topology, geometry, and Hook information. The Diagnostics screen can build a feedback report from app/build information, basic device information, and recent CombinedStatus runtime log entries. Runtime-log collection is user-triggered and uses Root only while the report is generated; no resident logging service, polling loop, or continuous View-tree sampling is added.

## Development workflow

Engineering rules and the required change-control process are documented in [CONTRIBUTING.md](CONTRIBUTING.md). New directions are evaluated first, then a bounded implementation plan is defined and validated before project mutation. If runtime evidence invalidates that plan, implementation stops for re-evaluation before continuing. Functional changes also require pre/post-change checks, copy review for text changes, and MIUIX review for UI changes.

`main` is the stable integration baseline. SystemUI module work is developed on `dev`, where each small feature must pass CI and real-device validation before it is promoted to `main`. Short-lived `feat/*` branches are reserved for higher-risk experiments and are merged back into `dev` once validated.

Both `main` and `dev` run the Android build workflow. CI uses per-branch concurrency so a newer push cancels an obsolete in-progress build for the same branch.

## Build and signing

Pushes to main use a dedicated fixed CI debug certificate so successive test APKs can update in place. The test certificate is separate from the release certificate and its keystore is supplied only through the CI_DEBUG_KEYSTORE_BASE64 repository secret. Release builds use a separate manually triggered workflow and read signing material only from the protected release environment. Signing keys and credentials are not stored in the repository.

Test builds are published as GitHub Actions artifacts. Stable releases use the v<versionName> tag and require the display version to be advanced before another stable release can be published.

## Build

Use Android Studio with the Android 17 / API 37 SDK and JDK 21 installed, or run the repository build workflow.

## Changelog

Notable changes are tracked in CHANGELOG.md.
