# Changelog

All notable changes to CombinedStatus are documented in this file.

The project follows a Keep a Changelog-style structure. Until the first formal release is published, `[Unreleased]` describes the **current net state intended for the initial 0.0.1 release**. Intermediate experiments, superseded implementations, CI-by-CI adjustments, and diagnostic investigation history belong in Git history or dedicated development documentation rather than being accumulated here.

## [Unreleased]

### Added

- Modern Xposed API 102 module baseline scoped to `com.android.systemui`, with a single Java entry point, verified SystemUI compatibility profile, host lifecycle capture, and hot-reload support.
- Event-driven CombinedStatus state pipeline for battery, Wi-Fi, mobile network, airplane mode, default-data subscription, connectivity, and native SystemUI tint.
- Home status-bar CombinedStatus rendering built on verified SystemUI hosts and native state sources while keeping SystemUI geometry ownership conservative.
- Shared scene-capability and layout-policy models for Home, notification-shade transitions, Control Center, keyguard, and AOD, keeping charging state separate from scene identity.
- Deterministic parsing of HyperOS Wi-Fi and mobile signal resources into semantic levels, with local unit coverage for core mapping behavior.
- Structured bounded runtime diagnostics with schema/session identity, lifecycle and compatibility health, state-to-render tracing, host/topology inspection, geometry ownership probes, and current-generation report scoping.
- Debug, Canary, and Release build channels with diagnostics depth separated from core feature behavior; Canary remains non-debuggable and release-optimized while supporting bounded runtime diagnostics.
- Built-in diagnostic report export/share, reading LSPosed module logs first with logcat fallback, without a resident collection service.
- Manual, explicitly confirmed SystemUI restart using bounded Root execution without a resident Root service.
- MIUIX 0.9.4 application shell with Home, Features, and Settings navigation, predictive back, direction-aware swipe-back, and adaptive launcher icons.
- Appearance settings for light/dark mode, dynamic color, standard/floating navigation, Blur/Glass floating-navigation material, and in-app swipe-back behavior.
- Android 13+ per-app language selection for system default, English, and Simplified Chinese.
- Optional launcher-icon hiding while retaining a non-launcher app entry point.
- Runtime diagnostics UI for app/build, device/system, module compatibility, diagnostics level, and report actions.

### Changed

- Network, airplane-mode, tint, subscription, and connectivity state acquisition now favors authoritative event-driven platform/SystemUI sources and cached process-scoped state instead of repeated querying or polling.
- Wi-Fi and mobile semantic updates are committed before the corresponding verified SystemUI emitter proceeds so CombinedStatus can enter the same UI frame as native icon changes.
- Render-state commits are atomic: incomplete candidates retain the last stable frame, while explicit Hidden/Unavailable states update immediately.
- Home rendering is no longer tied to `BuildConfig.DEBUG`; build channels now affect diagnostics capability rather than core rendering behavior.
- SystemUI integration preserves native layout, translation, visibility, and animation ownership wherever possible; CombinedStatus-specific appearance is kept in its own presentation/rendering layer.
- Diagnostics use bounded event-driven snapshots rather than continuous collection, and General mode avoids detailed per-transition diagnostic allocation.
- Hot reload now rotates runtime-session identity, replaces the previous hook generation, revalidates compatibility, and restores current host health instead of stacking duplicate generations.
- The app now uses MIUIX 0.9.4 components and shared production material definitions for navigation and appearance previews instead of maintaining separate visual approximations.
- Diagnostics, app descriptions, and user-facing terminology consistently identify Xiaomi HyperOS as the target and use **mobile network / 移动网络** terminology.
- Build and release tooling now separates Debug, Canary, and formal Release signing/CI responsibilities; application build identity is independent from GitHub Actions run numbers.
- Developer and contributor rules now use explicit MUST/SHOULD/MAY boundaries and formalize lifecycle ownership, host-scoped sessions, disposal, single-writer discipline, geometry separation, fail-native fallback, reference-project limits, and staged runtime ownership migration.

### Fixed

- Network hook installation is fail-soft per source so failure in Wi-Fi or mobile resolution no longer tears down the other source.
- Verified network emitter resolution no longer depends on resolving Kotlin `Continuation` by name through the SystemUI ClassLoader, preventing optimized/Canary builds from losing network hooks.
- Wi-Fi state tracking now follows the verified HyperOS Wi-Fi collector and registers the relevant root before the native binder proceeds.
- CombinedStatus no longer disappears when HyperOS temporarily hides the native battery container during Wi-Fi/mobile status transitions.
- Airplane-mode presentation now updates from the authoritative global setting path used on the target device, with mobile-signal sampling retained only as fallback evidence.
- Transparent or uninitialized tint samples no longer blank the CombinedStatus renderer.
- Per-app language selection preserves an explicitly selected language even when it currently matches the system locale.
- CI signing verification remains pinned to the expected certificate while accepting current Android Build Tools signer output.

### Removed

- Removed the experimental battery-anchor charging-island follower after runtime evidence showed the battery anchor was not the transition owner.
- Removed the experimental Home owned-slot padding mutation after validation showed that a nominal 105 px request expanded native battery geometry and leaked layout effects into other scenes.
- Removed superseded continuous/multi-frame diagnostic probes once their ownership questions were resolved; production diagnostics remain bounded and event-driven.
- Removed development-only UI approximations that were replaced by native MIUIX components or shared production configuration.

### Engineering

- Runtime architecture is moving toward explicit `Host -> HostSession -> owned resources` boundaries rather than global host-specific mutable state.
- Long-lived runtime resources require explicit ownership and cleanup; SystemUI recreation, host replacement, and hot reload must not leave stale sessions active.
- Live SystemUI properties follow a single-writer rule: diagnostics may observe native geometry without implicitly granting CombinedStatus ownership of it.
- Native SystemUI layout geometry, CombinedStatus visual geometry, transition geometry, and optical adjustment are treated as separate responsibilities.
- Compatibility-sensitive hooks are tied to verified members from the pinned HyperOS SystemUI `17.03.260226.r` target profile and validated against the live runtime when ownership matters.
- The staged ownership-migration gate prevents new long-lived lifecycle responsibilities from accumulating indefinitely in `CombinedStatusModule`.
