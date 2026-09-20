# CombinedStatus

**CombinedStatus for HyperOS** is an Android status-bar module project designed for Xiaomi HyperOS. Its goal is to combine battery, cellular, and Wi-Fi information into a single status indicator while preserving HyperOS SystemUI layout and transition behavior.

## Target platform

- Xiaomi HyperOS
- HyperOS SystemUI
- LSPosed module architecture
- MIUIX application interface

## Current milestone

The repository currently contains the first MIUIX UI shell only. This stage validates the application structure, navigation, appearance, localization, and Android build baseline before HyperOS SystemUI integration is introduced.

- Package: `com.chaners.combinedstatus`
- Display version: `0.0.1`
- Android: `minSdk 33`, `compileSdk 37.0`, `targetSdk 37`
- MIUIX: `0.9.4`
- Kotlin: `2.4.20`
- Android Gradle Plugin: `9.4.1`
- Languages: English, Simplified Chinese
- Minimum Android version: Android 13 / API 33

The current build follows the system language and does not hook HyperOS SystemUI, register background services, or request additional permissions.

## Build and signing

Pushes to `main` run an unsigned debug build for continuous integration. Release builds use a separate manually triggered workflow and read signing material only from the protected `release` environment. Signing keys and credentials are not stored in the repository.

Test releases are published as GitHub pre-releases with a directly downloadable signed APK. Stable releases use the `v<versionName>` tag and require the display version to be advanced before another stable release can be published.

## Build

Use Android Studio with the Android 17 / API 37 SDK installed, or run the repository build workflow.
