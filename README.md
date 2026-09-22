# CombinedStatus

**CombinedStatus for HyperOS** is an LSPosed module for Xiaomi HyperOS that combines battery, mobile network, and Wi-Fi status into a single status-bar indicator.

The project is designed around native SystemUI behavior, lightweight runtime integration, and a MIUIX-based configuration app.

## Project scope

CombinedStatus focuses on three areas:

- status-bar integration for battery, mobile network, and Wi-Fi state;
- native-looking behavior across HyperOS SystemUI transitions;
- a lightweight settings and diagnostics interface built with MIUIX.

The project is under active development. Features in `dev` may still require real-device validation before they are promoted to `main`.

## Compatibility

- Xiaomi HyperOS
- Android 13+ / API 33+
- SystemUI scope: `com.android.systemui`
- Modern Xposed API 102
- MIUIX 0.9.4
- Compile / target SDK: API 37
- JDK 21

Compatibility work is based on a pinned HyperOS SystemUI reference artifact instead of relying on the displayed SystemUI version alone.

### Validated environment

- Device: Xiaomi 15 Pro
- Model: `2410DPN6CC` (`haotian`)
- Android: 17 / API 37
- OS version: `4.0.0.14.XOBCNXM.D01`
- SystemUI: `17.03.260226.r` (`202602260`)
- SystemUI SHA-256: `a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d`

The device and HyperOS values describe the environment used for validation. Exact SystemUI compatibility identity is determined by the artifact fingerprint, not by device or version strings alone.

## Architecture

The module keeps SystemUI responsible for host layout, lifecycle, and transition ownership wherever possible. CombinedStatus handles state normalization and presentation without replacing the surrounding SystemUI structure.

Runtime integration is event-driven. Debug builds may expose additional bounded diagnostics for compatibility and layout validation, while release behavior remains lightweight.

The companion app uses MIUIX for its interface and Jetpack DataStore for persistent appearance and navigation preferences.

## Repository structure

- `main` — stable integration baseline
- `dev` — active development and device validation
- `compat/` — compatibility profiles and reference metadata
- `app/` — Android application and module implementation

Changes are validated through CI and, where SystemUI behavior is involved, real-device testing before promotion to `main`.

## Build

Use Android Studio with Android API 37 and JDK 21, or run the repository's GitHub Actions workflow.

CI test builds are published as workflow artifacts.

## Documentation

- [Changelog](CHANGELOG.md)
- [Contributing](CONTRIBUTING.md)
