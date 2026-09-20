# CombinedStatus

**CombinedStatus for HyperOS** is an Android status-bar module project designed for Xiaomi HyperOS. Its goal is to combine battery, cellular, and Wi-Fi information into a single status indicator while preserving HyperOS SystemUI layout and transition behavior.

## Target platform

- Xiaomi HyperOS
- HyperOS SystemUI
- LSPosed module architecture
- MIUIX application interface

## Current milestone

The repository currently contains the first MIUIX UI shell only. This stage validates the application structure, navigation, appearance, and Android build baseline before HyperOS SystemUI integration is introduced.

- Package: `com.chaners.combinedstatus`
- Display version: `0.0.1`
- Android: `minSdk 24`, `compileSdk 37.0`, `targetSdk 37`
- MIUIX: `0.9.4`
- Kotlin: `2.4.20`
- Android Gradle Plugin: `9.4.1`

The current build does not hook HyperOS SystemUI, register background services, or request additional permissions.

## Build

Use Android Studio with the Android 17 / API 37 SDK installed, or run the repository build workflow.
