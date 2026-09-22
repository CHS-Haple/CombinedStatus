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
- Pinned compatibility profile generated from the exact HyperOS SystemUI 17.03.260226.r APK.
- Read-only capture of the primary HyperOS status-bar host after `MiuiNotificationStatusContainer.onFinishInflate()`, stored as a weak reference for later rendering integration.
- Manual SystemUI scope restart from the module app with an explicit Root confirmation and no resident Root service.
- Modern Xposed API 102 hot reload lifecycle with automatic app-update reload metadata and hook migration for the status-host observer.
- One-shot native SystemUI status-view inventory after host layout, covering mobile network, Wi-Fi, and battery views without modifying geometry or drawing.
- Root-view topology inventory for native status icons and their verified SystemUI containers, with bounded one-shot traversal and ancestor/path diagnostics.
- Built-in export/share diagnostic report for feedback, containing app/build, basic device, and recent CombinedStatus runtime information without a resident collection service.
- Diagnostic report now reads LSPosed's own module log files first and falls back to logcat, matching the framework's actual log storage.
- Debug topology diagnostics now flag mobile-network and Wi-Fi candidate views by class/resource identity so third-party status-bar container changes remain observable.
- Verified HyperOS Home Wi-Fi/mobile collectors now provide the production event-driven network state source; detailed collector diagnostics remain Debug-only, with no polling or SystemUI geometry mutation.
- Event-driven normalized state storage now combines battery state, semantic Home Wi-Fi visibility/resource state, and per-subscription mobile signal/VoLTE/VoWiFi resources without drawing or changing native SystemUI geometry.
- Deterministic signal parsing maps SystemUI mobile `signal_0..4` / `signal_null` and Wi-Fi `wifi_signal_0..3` resources to semantic levels, covered by local unit tests.
- Debug builds now include a Home-only, non-layout visual probe that reproduces the P11BJ 120-unit ring/Wi-Fi/mobile-dot geometry inside the verified native 105×108 battery slot using ViewGroupOverlay; native icons remain visible and native geometry is untouched.
- A pure shared layout-policy layer now models visual size, adaptive neighbor gap, requested/applied slot width, end anchoring, render mode, and motion ownership for future multi-scene reuse; it is not wired to runtime layout yet.
- A pure scene-capability policy now classifies Home, notification-shade transition, Control Center, keyguard, and AOD without duplicating geometry rules; charging variants remain render state rather than a separate scene.
- Debug stable-status diagnostics now capture one-shot Home slot readiness metrics (padding, layout params, margins, adjacent status-icon boundary, clipping, RTL, and native translation) without mutating geometry.

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
- Standardized module terminology on mobile network / 移动网络 and clarified Xiaomi HyperOS as the target platform in module descriptions.
- Diagnostics now report the active Modern Xposed API 102 runtime and hot-reload capability instead of the earlier pre-hook placeholder state.
- Diagnostics UI now uses MIUIX read-only information rows for module framework, compatibility baseline, and the native status probe; preview copy no longer claims SystemUI is disconnected.
- Native status probe wording now describes the status-bar topology check without exposing host-lifecycle implementation details.
- Diagnostics now follow build type: release keeps low-frequency operational diagnostics, while debug adds detailed topology, geometry, and Hook reporting without changing core module behavior.
- SystemUI restart and on-demand diagnostic collection now share one bounded Root shell executor instead of duplicating process lifecycle code.
- Push CI remains debug-only for `main`/`dev`; signed release builds remain explicit manual workflow runs.
- LibXposed artifacts are resolved explicitly from Maven Central at `repo.maven.apache.org`, restricted to the `io.github.libxposed` group.
- Restart confirmation now follows the MIUIX two-action dialog layout with equal-width actions and user-facing SystemUI wording.
- Xposed lifecycle diagnostics now include the internal build ID to make hot-reload generation changes directly visible in LSPosed logs.
- Cold-start initialization now configures edge-to-edge before Compose content and prepares one-time platform state before the first composition; later theme changes update only system-bar icon appearance.
- Debug diagnostics now record a one-shot, bounded three-level inventory of the verified MIUI status-icon container subtree to identify live icon ownership without adding hooks or reading user-facing text.
- Diagnostic reports now default to the latest SystemUI process session instead of accumulating historical sessions across builds.
- Diagnostic report export continues to use Android's system document picker with an editable default text-file name.
- Diagnostic report sharing now uses a bounded MediaStore Downloads transport with a system-managed content URI, standard text/plain semantics, EXTRA_STREAM, ClipData, and temporary read permission.
- Temporary managed share reports are kept under Download/CombinedStatus and pruned to at most three recent files with a 24-hour age bound.
- Stable status geometry capture now waits for the first valid battery-view layout before recording the anchor, while keeping native SystemUI geometry untouched.
- Compatibility metadata, diagnostics copy, and CI verification now target SystemUI only, matching the module's actual `com.android.systemui` scope.
- Verified network collectors now feed the typed CombinedStatus state snapshot in all build types, while detailed change-only diagnostics remain Debug-only.

### Fixed
- Home Wi-Fi roots are registered before the native binder proceeds; Wi-Fi icon tracing now follows the verified `getWifiIcon()` collector (`classId=1`) instead of the unrelated `setImageViewResId()` helper.
- CI certificate verification now accepts the current Android Build Tools signer output while still pinning the expected certificate SHA-256 digest.
- Per-app language selection now keeps the explicit System/English/Simplified Chinese choice visible even when the chosen language matches the current system locale.
- Normalized state diagnostics now include the battery plugged source consistently for both charging and discharging states.
- The Home visual probe now matches P11BJ unavailable/mobile-airplane rendering: all four mobile dots remain as low-alpha filled dots and the X marker is drawn on top.
