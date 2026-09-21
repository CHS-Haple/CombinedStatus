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
- Official MIUIX 0.9.4 floating-navigation blur recipe with automatic runtime-shader fallback, 25 px texture blur, 60% surface blend, and theme-aware glass-stroke highlight.
- Persistent appearance preferences for theme mode, floating-navigation blur, and in-app swipe-back.
- Android 13+ per-app language selection for system default, English, and Simplified Chinese.
- Optional launcher icon hiding through a dedicated activity alias while retaining a non-launcher CATEGORY_INFO front door.
- Modern Xposed API 102 module baseline with a single Java entry point and a static `com.android.systemui` scope.
- One-shot SystemUI structural compatibility probe for the status bar, Control Center, keyguard header, and battery-view hosts.
- Pinned compatibility profile generated from the exact HyperOS SystemUI 17.03.260226.r and SystemUI component 18.3.2.22.0 APKs.

### Changed
- Minimum Android version is Android 13 / API 33 to match the current MIUIX blur baseline.
- Android compile and target SDK baseline is API 37.
- Application JVM target is 21 to align with the MIUIX 0.9.4 navigation runtime.
- Launcher icon resources now use the Android adaptive-icon resource model.
- The original single-screen settings index is split into top-level Home, Features, and Settings areas while deeper settings remain on the navigation stack.
- Appearance settings now drive the root theme and navigation behavior instead of temporary screen-local preview state.
- Aligned system bar icon appearance with the selected theme mode.
- Aligned top-level pager fling and back-to-home behavior with the MIUIX 0.9.4 example patterns.
- Renamed the optional floating-bar effect to floating navigation blur and migrated its saved preference key.
- Simplified in-app wording and removed development-oriented placeholder phrasing.
- Aligned page spacing and typography with MIUIX 0.9.4 defaults, including standard cards, section spacing, top-level scroll behavior, and bottom padding handling.
- Standardized user-facing settings copy and hardened platform language and launcher-entry state handling.
- Polished settings summaries to use shorter, more natural system-style phrasing without unnecessary semicolons.
- Added CI validation for modern Xposed metadata while keeping the initial module entry point hook-free.

### Fixed
- CI certificate verification now accepts the current Android Build Tools signer output while still pinning the expected certificate SHA-256 digest.
- Per-app language selection now keeps the explicit System/English/Simplified Chinese choice visible even when the chosen language matches the current system locale.
