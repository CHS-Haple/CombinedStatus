# Changelog

All notable changes to CombinedStatus are documented in this file.

The project follows a Keep a Changelog-style structure. Development changes remain under Unreleased until a display version is formally published.

## [Unreleased]

### Added
- English and Simplified Chinese MIUIX application shell for HyperOS.
- Adaptive launcher icon with separate foreground/background resources and Android themed-icon support.
- Fixed CI debug signing so successive test APKs can update in place.
- MIUIX 0.9.4 navigation runtime with serializable routes, standard page transitions, predictive back, and direction-aware swipe-back gestures.
- Home, Features, and Settings top-level navigation with a MIUIX floating navigation bar.
- Optional blur material on the official MIUIX floating navigation bar with automatic fallback when runtime shaders are unavailable.
- Persistent appearance preferences for theme mode, floating-navigation blur, and in-app swipe-back.

### Changed
- Minimum Android version is Android 13 / API 33 to match the current MIUIX blur baseline.
- Android compile and target SDK baseline is API 37.
- Application JVM target is 21 to align with the MIUIX 0.9.4 navigation runtime.
- Launcher icon resources now use the Android adaptive-icon resource model.
- The original single-screen settings index is split into top-level Home, Features, and Settings areas while deeper settings remain on the navigation stack.
- Appearance settings now drive the root theme and navigation behavior instead of temporary screen-local preview state.

### Fixed
- CI certificate verification now accepts the current Android Build Tools signer output while still pinning the expected certificate SHA-256 digest.
