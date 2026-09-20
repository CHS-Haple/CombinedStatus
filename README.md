# CombinedStatus

**CombinedStatus for HyperOS** is an Android status-bar module project designed for Xiaomi HyperOS. Its goal is to combine battery, cellular, and Wi-Fi information into a single status indicator while preserving HyperOS SystemUI layout and transition behavior.

## Target platform

- Xiaomi HyperOS
- HyperOS SystemUI
- LSPosed module architecture
- MIUIX application interface

## Current milestone

The project is currently in its application-shell and navigation stage. The Android app uses MIUIX 0.9.4, a type-safe MIUIX navigation stack, adaptive launcher icons, localized resources, and reproducible CI signing. HyperOS SystemUI hooks are intentionally not enabled yet.

The top-level interface is organized as Home, Features, and Settings. Deeper settings pages use the MIUIX navigation runtime with standard transitions, system predictive back, and direction-aware swipe-back gestures. Appearance preferences are persisted with Jetpack DataStore and can control theme mode, optional MIUIX blur on the official floating navigation bar, and in-app swipe-back behavior. Android 13+ per-app language preferences are handled by the platform LocaleManager, and the launcher entry can be hidden without disabling the main activity.

- Package: com.chaners.combinedstatus
- Display version: 0.0.1
- Android: minSdk 33, compileSdk 37.0, targetSdk 37
- JVM: 21
- MIUIX: 0.9.4
- Kotlin: 2.4.20
- Android Gradle Plugin: 9.4.1
- Languages: English, Simplified Chinese
- Minimum Android version: Android 13 / API 33

The current build follows the system language and does not hook HyperOS SystemUI, register background services, or request additional permissions.

## Build and signing

Pushes to main use a dedicated fixed CI debug certificate so successive test APKs can update in place. The test certificate is separate from the release certificate and its keystore is supplied only through the CI_DEBUG_KEYSTORE_BASE64 repository secret. Release builds use a separate manually triggered workflow and read signing material only from the protected release environment. Signing keys and credentials are not stored in the repository.

Test builds are published as GitHub Actions artifacts. Stable releases use the v<versionName> tag and require the display version to be advanced before another stable release can be published.

## Build

Use Android Studio with the Android 17 / API 37 SDK and JDK 21 installed, or run the repository build workflow.

## Changelog

Notable changes are tracked in CHANGELOG.md.
