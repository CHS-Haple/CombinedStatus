# Changelog

All notable changes to CombinedStatus are documented in this file.

The project follows a Keep a Changelog-style structure. During normal development before the first formal release, `[Unreleased]` describes the **current net state intended for the initial 0.0.1 release**. The final release-preparation commit freezes those changes into a dated version section immediately before publication. Intermediate experiments, superseded implementations, CI-by-CI adjustments, and diagnostic investigation history belong in Git history or dedicated development documentation.

## [Unreleased]

### Added

- Modern Xposed API 102 module baseline scoped to `com.android.systemui`, with a single Java entry point, verified SystemUI compatibility profile, host lifecycle capture, and hot-reload support.
- Event-driven CombinedStatus state pipeline for battery, Wi-Fi, mobile network, airplane mode, default-data subscription, connectivity, and native SystemUI tint.
- Home status-bar CombinedStatus rendering based on verified SystemUI hosts and native state sources while preserving conservative SystemUI geometry ownership.
- Shared scene-capability and layout-policy models for Home, notification-shade transitions, Control Center, keyguard, and AOD, with charging represented as render state rather than scene identity.
- Deterministic HyperOS Wi-Fi and mobile signal parsing into semantic levels, with unit coverage for the core mappings.
- Bounded structured runtime diagnostics for compatibility, lifecycle, state propagation, rendering, host topology, geometry ownership, and hot reload.
- Built-in diagnostic report export/share using LSPosed module logs with logcat fallback, without a resident collection service.
- Debug, Canary, and Release build channels with diagnostics depth separated from core feature behavior; Canary is non-debuggable and release-optimized while retaining bounded runtime diagnostics.
- Manual, explicitly confirmed SystemUI restart using bounded Root execution without a resident Root service.
- MIUIX 0.9.4 application shell with Home, Features, and Settings navigation, predictive back, direction-aware swipe-back, and adaptive launcher icons.
- Appearance settings for light/dark mode, dynamic color, standard/floating navigation, Blur/Glass floating-navigation material, and in-app swipe-back behavior.
- Android 13+ per-app language selection for system default, English, and Simplified Chinese.
- Optional launcher-icon hiding while retaining a non-launcher app entry point.
- Runtime diagnostics UI for app/build, device/system, module compatibility, diagnostics level, and report actions.

### Changed

- Top-level page navigation now uses MIUIX Cross-Axis pager gesture ownership so horizontal page switching remains available while vertical child content is settling, without adding a second app-owned gesture recognizer.
- Companion-app MIUIX dependencies now track the published main-canary snapshot `0.9.4-2afdbb39-SNAPSHOT` from upstream revision `2afdbb39f1aac5747165cc354cafd4b918fa55a5`, with one shared dependency identity exposed to BuildConfig for later version reporting.
- Diagnostic reports now limit log collection to CombinedStatus-related runtime/share diagnostics and no longer collect broad third-party application/system share logs.
- Runtime state acquisition now favors authoritative event-driven platform/SystemUI sources and cached process-scoped state instead of repeated querying or polling.
- Wi-Fi and mobile semantic updates are committed before their verified SystemUI emitters proceed so CombinedStatus can enter the same UI frame as native icon changes.
- Render-state commits are atomic: incomplete candidates retain the last stable frame, while explicit Hidden/Unavailable states update immediately.
- Home rendering is independent of `BuildConfig.DEBUG`; build channels control diagnostics capability rather than core rendering behavior.
- SystemUI integration preserves native layout, translation, visibility, and animation ownership wherever practical; CombinedStatus-specific appearance remains in its own presentation/rendering layer.
- Diagnostics use bounded event-driven snapshots rather than continuous collection, and General mode avoids detailed per-transition diagnostic allocation.
- Hot reload rotates runtime-session identity, replaces the previous hook generation, revalidates compatibility, and restores current host health instead of stacking duplicate generations.
- The app uses native MIUIX 0.9.4 components and shared production material definitions for navigation and appearance previews instead of separate visual approximations.
- Diagnostics, app descriptions, and user-facing copy consistently identify Xiaomi HyperOS as the target and use **mobile network / 移动网络** terminology.
- Build and release tooling separates Debug, Canary, and formal Release signing/CI responsibilities; distributable APK filenames use application version/build identity rather than GitHub Actions run numbers, while test-release tags may retain the run number as CI execution metadata.

### Fixed

- Network hook installation is fail-soft per source so failure in Wi-Fi or mobile resolution no longer tears down the other source.
- Verified network emitter resolution no longer depends on resolving Kotlin `Continuation` by name through the SystemUI ClassLoader, preventing optimized/Canary builds from losing network hooks.
- Wi-Fi state tracking follows the verified HyperOS Wi-Fi collector and registers the relevant root before the native binder proceeds.
- CombinedStatus remains visible when HyperOS temporarily hides the native battery container during Wi-Fi/mobile status transitions.
- Airplane-mode presentation follows the authoritative global setting used by the target device, with mobile-signal sampling retained only as fallback evidence.
- Center presentation now represents the active data connection only: Wi-Fi or mobile type when active, otherwise an explicit empty center, while cellular service state remains in the signal-dot area; transport-first handoff logic avoids transient Wi-Fi/mobile mismatch frames.
- Transparent or uninitialized tint samples no longer blank the CombinedStatus renderer.
- Per-app language selection preserves an explicit language choice even when it currently matches the system locale.
- CI signing verification remains pinned to the expected certificate while accepting current Android Build Tools signer output.

### Removed

- Removed the experimental battery-anchor charging-island follower after runtime evidence showed that the battery anchor was not the transition owner.
- Removed the experimental Home owned-slot padding mutation after validation showed that it altered native battery geometry and leaked layout effects into other scenes.

### Engineering

- Release automation now restricts signed test releases to `dev` or `main` and stable releases to `main`, keeping experiment/work branches in CI artifacts rather than GitHub Releases.
- Promotion readiness now runs as a lightweight post-Build workflow, so readiness infrastructure failures cannot turn an otherwise successful APK Build red; readiness still gates `dev -> main` through the same CI/device/changelog conditions.
- CI validation now separates Light, Fast, Integration, and Full scopes: ordinary work branches prove changes with Debug, trusted `dev` runtime integration builds signed Canary only, and full Debug+Canary validation is reserved for build-system or stable-boundary risk.
- Dependabot version updates now target `dev`; minor/patch updates are grouped per ecosystem to reduce PR noise, major updates remain individually reviewable, and generated dependency PRs are not auto-merged by default.
- Contribution governance uses risk-based routing: repository text/governance and repository automation may move independently of runtime promotion when their own validation passes, shared `main` changes are history-preserving back-synced into `dev`, normal work reuses bounded active `feat/*`/`fix/*` branches instead of creating one branch per sub-task, device validation is checkpoint-based, hotfixes return to `dev`, and merged short-lived branches are cleaned up automatically.
- Upstream dependency adoption uses relevance classes, explicit maturity levels, exact-revision CI/artifact gates, isolated Canary validation, and a bounded work branch only when the branch-admission rules require one.
- Project source and contributions are licensed under Apache License 2.0, with third-party components retaining their upstream license obligations.
- Public/reproducible development now uses the checked-in official Gradle 9.7.1 Wrapper with distribution/integrity validation, commit-pinned GitHub Actions, secret-free pull-request validation, hardened ignore rules for local signing/environment artifacts, least-privilege workflow credentials, dependency-update automation, and explicit third-party dependency notices.
- Stable release automation is fail-closed: formal releases must come from a prepared `main` commit with a matching dated changelog section, pass target-profile/tests/Xposed-metadata/non-debuggable/signature checks, and use application release/build identity rather than CI run numbers for distributable APKs.
- Runtime architecture is moving toward explicit `Host -> HostSession -> owned resources` boundaries with required cleanup across host replacement, SystemUI recreation, and hot reload.
- Live SystemUI properties follow a single-writer rule; native layout geometry, CombinedStatus visual geometry, transition geometry, and optical adjustment remain separate responsibilities, and observation does not itself grant write ownership.
- Compatibility-sensitive hooks are tied to verified members from the pinned HyperOS SystemUI `17.03.260226.r` target profile and are validated against the live runtime when ownership matters.
- Contributor rules define MUST/SHOULD/MAY boundaries, fail-native fallback, staged ownership migration, changelog discipline, the normal `dev` contribution target, and private security-reporting expectations.
