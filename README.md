# CombinedStatus

Android status-bar module project for a combined battery, cellular, and Wi-Fi indicator.

## Current milestone

The repository currently contains the first UI shell only. It is intended to validate the application structure and MIUIX appearance before SystemUI integration is introduced.

- Package: `com.chaners.combinedstatus`
- Display version: `0.0.1`
- Android: `minSdk 24`, `compileSdk 37`, `targetSdk 37`
- MIUIX: `0.9.4`
- Kotlin: `2.4.20`
- Android Gradle Plugin: `9.4.1`

The current build does not hook SystemUI, register background services, or request additional permissions.

## Build

Use Android Studio with Android API 37 installed, or run the repository build workflow.
