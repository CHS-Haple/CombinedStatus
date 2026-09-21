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

The project now has a modern Xposed API 102 module baseline in addition to its application shell and navigation layer. The Android app uses MIUIX 0.9.4, a type-safe MIUIX navigation stack, adaptive launcher icons, localized resources, and reproducible CI signing. The module is statically scoped only to `com.android.systemui`; after the one-shot compatibility probe, it installs one read-only lifecycle hook that captures the primary HyperOS status-bar host after inflation. The hook does not alter layout, measurement, translation, visibility, or drawing.

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

The current build follows the system language and uses a compile-only modern Xposed API 102 dependency. Its only SystemUI hook observes the primary status-bar host after inflation and stores a weak reference for later feature integration; it does not modify SystemUI geometry or visual state. The app does not register background services or request additional permissions.

## Development workflow

`main` is the stable integration baseline. SystemUI module work is developed on `dev`, where each small feature must pass CI and real-device validation before it is promoted to `main`. Short-lived `feat/*` branches are reserved for higher-risk experiments and are merged back into `dev` once validated.

Both `main` and `dev` run the Android build workflow. CI uses per-branch concurrency so a newer push cancels an obsolete in-progress build for the same branch.

## Build and signing

Pushes to main use a dedicated fixed CI debug certificate so successive test APKs can update in place. The test certificate is separate from the release certificate and its keystore is supplied only through the CI_DEBUG_KEYSTORE_BASE64 repository secret. Release builds use a separate manually triggered workflow and read signing material only from the protected release environment. Signing keys and credentials are not stored in the repository.

Test builds are published as GitHub Actions artifacts. Stable releases use the v<versionName> tag and require the display version to be advanced before another stable release can be published.

## Build

Use Android Studio with the Android 17 / API 37 SDK and JDK 21 installed, or run the repository build workflow.

## Changelog

Notable changes are tracked in CHANGELOG.md.
