# Changelog

All notable changes to Combined Status are documented in this file.

The project follows a Keep a Changelog-style structure. During normal development before the first formal release, `[Unreleased]` describes the **current net state intended for the initial 0.0.1 release**. The final release-preparation commit freezes those changes into a dated version section immediately before publication. Intermediate experiments, superseded implementations, CI-by-CI adjustments, and diagnostic investigation history belong in Git history or dedicated development documentation.

## [Unreleased]

### Added

- Modern Xposed API 102 module baseline scoped to `com.android.systemui`, with a single Java entry point, verified SystemUI compatibility profile, host lifecycle capture, and hot-reload support.
- Event-driven Combined Status state pipeline for battery, Wi-Fi, mobile network, airplane mode, default-data subscription, connectivity, and native SystemUI tint.
- Home status-bar Combined Status rendering based on verified SystemUI hosts and native state sources while preserving conservative SystemUI geometry ownership.
- Shared scene-capability and layout-policy models for Home, notification-shade transitions, Control Center, keyguard, and AOD, with charging represented as render state rather than scene identity.
- Deterministic HyperOS Wi-Fi and mobile signal parsing into semantic levels, with unit coverage for the core mappings.
- Bounded structured runtime diagnostics for compatibility, lifecycle, state propagation, rendering, host topology, geometry ownership, and hot reload.
- Built-in diagnostic report export/share using LSPosed module logs with logcat fallback, without a resident collection service.
- Debug, Canary, and Release build channels with diagnostics depth separated from core feature behavior; Canary is non-debuggable and release-optimized while retaining bounded runtime diagnostics.
- Manual, explicitly confirmed SystemUI restart using bounded Root execution without a resident Root service.
- MIUIX application shell with Home, Features, and Settings navigation, predictive back, direction-aware swipe-back, and adaptive launcher icons.
- Appearance settings for light/dark mode, dynamic color, standard/floating navigation, Blur/Glass floating-navigation material, and in-app swipe-back behavior.
- Android 13+ per-app language selection for system default, English, and Simplified Chinese.
- Optional launcher-icon hiding while retaining a non-launcher app entry point.
- Runtime diagnostics UI for app/build, device/system, module compatibility, diagnostics level, and report actions.

### Changed

- Build 303 keeps the Build 301 zero-width `combined_status` slot bridge but removes the inherited centered-child gravity that shifted the 105px render surface about half a slot left. The renderer now starts at local x=0 and overflows right into the preserved native battery slot; handoff additionally verifies render bounds `0..batteryWidth` before commit, with no peer battery/status-icon geometry writes.
- Center mobile-network type labels keep their accepted physical size while using heavier typography and glyph-ink centering for a more balanced `5G` / enhanced-type presentation inside the Combined Status composition.
- Island motion diagnostics now start bounded frame sampling only when development/Detailed diagnostics are active, stop on the UI thread when Detailed is disabled, and cancel their timeout callback during cleanup; General Canary diagnostics no longer pay the per-frame probe cost.
- Native Wi-Fi replacement now uses one semantic-readiness policy across rendering and suppression: SystemUI Wi-Fi semantics lead, Connectivity only fills an unknown Internet state when Wi-Fi is the current default network, obsolete freshness timestamps are removed, and native Wi-Fi remains visible whenever Combined Status cannot safely reproduce the current Wi-Fi presentation.
- Connectivity state now consumes authoritative default-network capability callbacks directly on the registered main-thread Handler and ignores stale loss events, avoiding redundant callback reposting and synchronous capability re-query during network transitions.
- English user-facing product naming now consistently uses **Combined Status** in the companion app and diagnostic reports while established technical identifiers remain unchanged.
- Home native Combined Status suppresses Home Wi-Fi and single-subscription mobile participants through the modern SystemUI binding visibility contract after handoff; multi-subscription mobile presentation remains SystemUI-owned until Combined Status can represent every active SIM, preserving native or externally extended dual-SIM layouts without geometry writes.
- Bottom navigation now differentiates selected and unselected items with MIUIX icon weights while preserving the existing navigation colors, layout, and interaction behavior.
- Top app bars now use MIUIX progressive backdrop blur while scrolling content beneath them on supported devices, with the standard solid surface retained as fallback.
- Companion-app transient feedback now uses MIUIX Snackbar, and icon-only SystemUI reload exposes a native MIUIX long-press tooltip without changing the action layout.
- Top-level page navigation now uses MIUIX Cross-Axis pager gesture ownership so horizontal page switching remains available while vertical child content is settling, without adding a second app-owned gesture recognizer.
- Companion-app MIUIX dependencies now track the validated published main-canary snapshot `0.9.4-2afdbb39-SNAPSHOT` from upstream revision `2afdbb39f1aac5747165cc354cafd4b918fa55a5`, with one shared dependency identity used across all MIUIX modules.
- Diagnostic reports now limit log collection to Combined Status-related runtime/share diagnostics and no longer collect broad third-party application/system share logs.
- Runtime state acquisition now favors authoritative event-driven platform/SystemUI sources and cached process-scoped state instead of repeated querying or polling.
- Wi-Fi and mobile semantic updates are committed before their verified SystemUI emitters proceed so Combined Status can enter the same UI frame as native icon changes.
- Render-state commits are atomic: incomplete candidates retain the last stable frame, while explicit Hidden/Unavailable states update immediately.
- Home rendering is independent of `BuildConfig.DEBUG`; build channels control diagnostics capability rather than core rendering behavior.
- SystemUI integration preserves native layout, translation, visibility, and animation ownership wherever practical; Combined Status-specific appearance remains in its own presentation/rendering layer.
- Diagnostics use bounded event-driven snapshots rather than continuous collection, and General mode avoids detailed per-transition diagnostic allocation.
- Hot reload rotates runtime-session identity, replaces the previous hook generation, revalidates compatibility, and restores current host health instead of stacking duplicate generations.
- The app uses native MIUIX components and shared production material definitions for navigation and appearance previews instead of separate visual approximations.
- Diagnostics, app descriptions, and user-facing copy consistently identify Xiaomi HyperOS as the target and use **mobile network / 移动网络** terminology.
- Build and release tooling separates Debug, Canary, and formal Release signing/CI responsibilities; distributable APK filenames use application version/build identity rather than GitHub Actions run numbers, while test-release tags may retain the run number as CI execution metadata.

### Fixed

- Native mobile suppression now also treats one-root dual-aggregated presentations as replaceable, preventing duplicate dual-row mobile visuals from remaining beside Combined Status while preserving separate dual-root presentations.
- Airplane-mode center presentation now reuses the left-facing HyperOS `stat_sys_signal_flightmode` shape family for parity with the live Home status bar, while retaining the existing unavailable mobile dots/cross and Wi-Fi precedence when Wi-Fi remains active.
- VPN-backed default networks no longer suppress an authoritative HyperOS mobile-type label at startup: Wi-Fi/cellular transports retain precedence, while VPN-only fallback waits for authoritative Wi-Fi absence before showing the mobile type.
- Native Combined Status tint updates now accept only the currently bound HyperOS status-bar battery view, preventing transient tint states from other `MiuiBatteryMeterView` instances from flashing through during light/dark inversion changes.
- Wi-Fi fallback rendering now uses the same semantic-readiness gate as native Wi-Fi suppression, so unknown OEM/VPN Wi-Fi variants remain fully native instead of being duplicated by an uncertain Combined Status Wi-Fi projection.
- Wi-Fi strength presentation now preserves all four SystemUI signal levels (0–3) as four distinct visual states using the existing three-path renderer, instead of collapsing native levels 2 and 3 into the same fully lit icon.
- Wi-Fi rendering now consumes the authoritative SystemUI `WifiIcon` resource emitted by the modern Wi-Fi pipeline instead of re-reading the bound `ImageView` tag; signal-level changes and SystemUI no-internet variants therefore update immediately even when Android selects cellular as the default network.
- Hot Reload restore now seeds the Home fallback renderer with the already-known native handoff ownership state, preventing the battery-anchored overlay from becoming visible for a frame before native CombinedStatus handoff is reasserted.
- Hot Reload now keeps SystemUI View and bindable-participant mutation on the SystemUI main thread: cross-generation transfer carries only stable runtime state and live SystemUI references, while the new generation re-adopts the existing native participant instead of synchronously detaching and transferring module-owned View/holder state from the framework callback thread.
- Native Combined Status Hot Reload now re-resolves participant handles from the current weakly held SystemUI host instead of weakly retaining a temporary resolver wrapper, preventing GC-driven `native-handles-missing` failures during prepare/detach.
- Runtime health now evaluates presentation-source readiness through the owning `presentationRuntime` subsystem, so tint/scene states that are legitimately not yet observed no longer mark an otherwise healthy runtime as degraded.
- Native Home Combined Status now uses a full-height custom participant shell matching its 108 px visual extent, preventing unlock appearance clipping while preserving SystemUI-owned status-icon and charging-island animation behavior.
- Network hook installation is fail-soft per source so failure in Wi-Fi or mobile resolution no longer tears down the other source.
- Verified network emitter resolution no longer depends on resolving Kotlin `Continuation` by name through the SystemUI ClassLoader, preventing optimized/Canary builds from losing network hooks.
- Wi-Fi state tracking follows the verified HyperOS Wi-Fi collector and registers the relevant root before the native binder proceeds.
- Combined Status remains visible when HyperOS temporarily hides the native battery container during Wi-Fi/mobile status transitions.
- Airplane-mode presentation follows the authoritative global setting through one event-driven ContentObserver owner; the mobile signal path no longer re-reads or writes airplane state.
- Center presentation now represents the active data connection only: Wi-Fi or mobile type when active, otherwise an explicit empty center, while cellular service state remains in the signal-dot area; transport-first handoff logic avoids transient Wi-Fi/mobile mismatch frames.
- Transparent or uninitialized tint samples no longer blank the Combined Status renderer.
- Per-app language selection preserves an explicit language choice even when it currently matches the system locale.
- CI signing verification remains pinned to the expected certificate while accepting current Android Build Tools signer output.

### Removed

- Removed the experimental battery-anchor charging-island follower after runtime evidence showed that the battery anchor was not the transition owner.
- Removed the experimental Home owned-slot padding mutation after validation showed that it altered native battery geometry and leaked layout effects into other scenes.

### Engineering

- Runtime diagnostics preference listening now has explicit lifecycle ownership outside `CombinedStatusModule`, keeping remote-preference registration and cleanup bounded across Hot Reload generations.
- Battery state acquisition moves from an app-owned `ACTION_BATTERY_CHANGED` receiver to the verified HyperOS `MiuiBatteryMeterView.onBatteryLevelChanged` callback with a dedicated runtime owner, leaving `StatusBarStableSession` responsible only for host/anchor diagnostics.
- Public documentation and contribution surfaces use **Combined Status** as the English display name while established technical identifiers such as `CombinedStatus` remain unchanged; contributor setup and pull-request guidance are documented at the appropriate public entry points.
- Pull-request CI now classifies ready `main` changes by affected paths, keeping documentation-only maintenance on Light validation while preserving Full validation for build, CI, dependency, tooling, and runtime-affecting stable-boundary changes.
- Stable GitHub Release notes now omit the changelog's Engineering section while retaining the complete engineering record in `CHANGELOG.md`; the dev-to-main readiness workflow is labeled explicitly in Actions.
- Contributor rules define MUST/SHOULD/MAY boundaries, fail-native fallback, staged ownership migration, changelog discipline, the normal `dev` contribution target, and private security-reporting expectations.
- Pull requests are explicitly treated as proposals: automated checks provide validation evidence, while final acceptance and any required maintainer-side device validation remain maintainer decisions.
- Merged PR branches now rely on GitHub's repository-level automatic head-branch deletion instead of a duplicate project-maintained cleanup workflow.
- Release automation now restricts signed test releases to `dev` or `main` and stable releases to `main`, keeping experiment/work branches in CI artifacts rather than GitHub Releases.
- Promotion readiness now runs as a lightweight post-Build workflow, so readiness infrastructure failures cannot turn an otherwise successful APK Build red; readiness still gates `dev -> main` through the same CI/device/changelog conditions.
- Home Combined Status rendering now hands off from the overlay fallback to a SystemUI-managed native status-bar participant only after model, tint, scene, attachment, and layout readiness are verified, while preserving fail-native fallback, native charging-island ownership, and lifecycle-safe Hot Reload replacement.
- CI validation now separates Light, Fast, Integration, and Full scopes: ordinary work branches prove changes with Debug, trusted `dev` runtime integration builds signed Canary only, and full Debug+Canary validation is reserved for build-system or stable-boundary risk.
- Dependabot version updates now target `dev`; minor/patch updates are grouped per ecosystem to reduce PR noise, major updates remain individually reviewable, and generated dependency PRs are not auto-merged by default.
- Contribution governance uses risk-based routing: repository text/governance and repository automation may move independently of runtime promotion when their own validation passes, shared `main` changes are history-preserving back-synced into `dev`, normal work reuses bounded active `feat/*`/`fix/*` branches instead of creating one branch per sub-task, device validation is checkpoint-based, hotfixes return to `dev`, and merged short-lived branches are cleaned up automatically.
- Upstream dependency adoption uses relevance classes, explicit maturity levels, exact-revision CI/artifact gates, isolated Canary validation, and a bounded work branch only when the branch-admission rules require one.
- Project source and contributions are licensed under Apache License 2.0, with third-party components retaining their upstream license obligations.
- Public/reproducible development now uses the checked-in official Gradle 9.7.1 Wrapper with distribution/integrity validation, commit-pinned GitHub Actions, secret-free pull-request validation, hardened ignore rules for local signing/environment artifacts, least-privilege workflow credentials, dependency-update automation, and explicit third-party dependency notices.
- Stable release automation is fail-closed: formal releases must come from a prepared `main` commit with a matching dated changelog section, pass target-profile/tests/Xposed-metadata/non-debuggable/signature checks, and use application release/build identity rather than CI run numbers for distributable APKs.
- Runtime architecture is moving toward explicit `Host -> HostSession -> owned resources` boundaries with required cleanup across host replacement, SystemUI recreation, and hot reload.
- Live SystemUI properties follow a single-writer rule; native layout geometry, Combined Status visual geometry, transition geometry, and optical adjustment remain separate responsibilities, and observation does not itself grant write ownership.
- Compatibility-sensitive hooks are tied to verified members from the pinned HyperOS SystemUI `17.03.260226.r` target profile and are validated against the live runtime when ownership matters.
