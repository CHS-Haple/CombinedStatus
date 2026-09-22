# CombinedStatus

**CombinedStatus for HyperOS** is an LSPosed module for Xiaomi HyperOS that brings battery, mobile signal, and Wi-Fi status into a single status-bar indicator.

The project focuses on clean SystemUI integration, native-looking behavior, and a lightweight MIUIX settings app.

## Highlights

- Combined battery, mobile signal, and Wi-Fi indicator
- Xiaomi HyperOS SystemUI integration
- MIUIX-based settings interface
- Light, dark, and dynamic color support
- Built-in compatibility and diagnostic tools
- Modern Xposed API 102

## Platform

- Xiaomi HyperOS
- Android 13 or later
- LSPosed
- MIUIX 0.9.4
- Android API 37
- JDK 21

## Development

The project is under active development.

- `main` — stable integration baseline
- `dev` — active development and device validation

Changes are validated through CI and real-device testing before promotion to `main`.

## Build

Use Android Studio with Android API 37 and JDK 21, or run the repository's GitHub Actions workflow.

CI test builds are available as workflow artifacts.

## Documentation

- [Changelog](CHANGELOG.md)
- [Contributing](CONTRIBUTING.md)
