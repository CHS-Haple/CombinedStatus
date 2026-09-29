# Current Development State

This file is the concise recovery point for active Guiyuan development. Read it after `CONTRIBUTING.md`. Keep chronological Build/investigation history in `DEVLOG.md`, future direction in `ROADMAP.md`, version semantics in `VERSIONING.md`, and record-writing rules in `RECORDING.md`.

## Repository baseline

- Last refreshed: 2026-09-29
- Repository: `CHS-Haple/Guiyuan`
- Stable branch: `main`
- Integration branch: `dev`
- Active display-version line: **0.0.3**
- First planned formal release: **1.0.0**
- Target profile: HyperOS SystemUI `17.03.260226.r`
- Modern Xposed API: 102
- Android namespace/applicationId: `com.chaners.guiyuan`
- Public product identity: **Guiyuan / 归元**
- Pinned MIUIX baseline: `0.9.4-5c91d5e5-SNAPSHOT`

### Stable product baseline

**Build 473 / `20260929-473`** is the current device-accepted and `main`-promoted Guiyuan 0.0.3 product baseline.

- Build 472 companion-app presentation/Sandbox work was accepted on device after exact-head Fast #1778 and signed Canary #506, then integrated by PR #173.
- Build 473 Wi-Fi optical normalization was accepted on device after exact-head Fast #1782 and signed Canary #509, then integrated by PR #175.
- The synchronized Build-473 integration passed exact-head Fast #1788 and post-merge `dev` Integration #1789.
- `validation/dev` was advanced to the genuinely device-validated runtime commit `8feb0d51a4974442f6683d4550608739986d87a2`; Build #1792 passed and promotion-readiness #143 reported `READY: dev is CI-green and device-validated`, carrying validation only across the later documentation-only delta.
- Promotion PR #176 used exact READY `dev@a701445430602cf636323ed551a60c1ecc62a09f`; promotion Build #1793 passed.
- PR #176 merged to `main` with the required merge commit `7db7159a340642564bb389519da362f756f7a884`.
- Post-merge `main` Build #1794 passed target-profile verification, build/tests, Modern Xposed metadata, Haple signing/signature verification, non-debuggable Canary verification and artifact upload.
- Main-push CodeQL run #82 passed both Python and Actions analysis jobs.
- `dev` was fast-forwarded to the same promotion merge commit so stable and integration history remain aligned without a duplicate synchronization commit.
- This documentation closure does not create a new runtime Build or alter the APK.

### Accepted lineage inherited from Build 466

- **SystemUI runtime behavior:** Build 456 / `20260929-456`, integrated by PR #163 as `d70b416ba531651c6690027b7404b1854fdb3056`. Device validation accepts Home, Keyguard, AOD-native gating, QS_FAKE transition ownership, session-scoped native ignored-slot ownership, Hot Reload continuity, and cleanup restoration on the pinned target.
- **App icon:** Build 452, integrated by PR #166 as `44b10371e0709d155468f7f2e67307fde5f11ab2`. The Guiyuan mark uses the accepted rotationally symmetric adaptive-icon geometry.
- **Companion app:** Build 464, integrated through PR #165 as `a2394db92ce208771956defcd558c954065000f7`; Build 465 advanced the integrated display line to 0.0.3. Home Runtime Status, production-rendered Preview Sandbox, Features hierarchy, Diagnostics action styling, and MIUIX-aligned Sandbox controls are part of the accepted baseline.

### Build 472-473 additions in the stable baseline

- **Build 472 / PR #173:** Preview Sandbox setting structure uses MIUIX `BasicComponent` / native preference title ownership with moderate spacing; the Diagnostics Guiyuan identity is drawn directly at final vector size before rotation. Fast #1778, signed Canary #506 and maintainer device review accepted the result.
- **Build 473 / PR #175:** connected, no-Internet and hotspot native Wi-Fi variants use the same-level connected Wi-Fi drawable as the optical-fit reference while preserving each HyperOS drawable's authored viewport/badge relationship. Fast #1782, signed Canary #509 and maintainer device review accepted the result.
- Build 473 adds no Wi-Fi semantic source, Hook, observer, listener, transition owner, copied drawable, or per-state scale constant. `CombinedStatusPainter` remains the single shared renderer used by Preview Sandbox and real Guiyuan SystemUI presentation.

## Current phase

Phases 2A, 2B, 3 and 4 are complete for the validated baseline:

- **Phase 2A:** Home carrier / presentation architecture.
- **Phase 2B:** unlocked Home continuity + bounded Control Center transition ownership/geometry bridge is structurally complete; visual transition-animation adaptation remains pending.
- **Phase 3:** Keyguard / lockscreen / AOD steady-scene ownership is complete for the current target; Keyguard-originated visual transition animation remains pending with the shared QS_FAKE animation work.
- **Phase 4:** companion-app Home / Preview Sandbox integration.

The next executable work on the 0.0.3 line is **transition-animation adaptation across Home/Keyguard -> QS_FAKE -> native Control Center**, before adaptive sizing, spacing and broader visual controls. The carrier/ownership bridge is verified, but Guiyuan's visual motion/alpha/shape continuity across the gesture is not yet fully adapted.

## Current architecture / ownership boundary

### Home

`MiuiNotificationStatusContainer / system_icon_area (HostSession) -> MiuiStatusBatteryContainer / system_icons.overlay (visual carrier) -> resolved Home layout -> Guiyuan renderer`

SystemUI owns surrounding native layout, Battery presentation/hide behavior, native tint semantics, Home visibility, and charging/Super-Island motion. Guiyuan owns its compact composition and only narrowly scoped reversible presentation state.

### Control Center / QS_FAKE

Unlocked/Home and enabled Keyguard source scenes may project Guiyuan through the verified top-level `ControlCenterFakeStatusIcons` QS_FAKE transition owner. Compact cutover waits for native layout readiness. Fully expanded Control Center is native-only through HyperOS fake/final appearance ownership.

Notification Shade itself does not present the target status-icon row on the pinned profile and remains native-only.

### Keyguard / AOD

Keyguard uses a separate host/session adapter while sharing domain/render semantics. The user setting controls steady Keyguard Guiyuan eligibility. AOD remains native-only through the verified native AOD authority. Home and Keyguard do not share mutable host/session ownership.

### State, layout and cleanup

- Prefer authoritative HyperOS/SystemUI semantic state and native resources.
- One mutable runtime property has one writer.
- Stable geometry, transition geometry, native slot occupancy, and optical adjustment remain separate concerns.
- Keyguard/QS_FAKE ignored-slot state is session-owned and restored by owned delta; Home keeps its separately validated scoped path.
- Hot Reload preserves continuous generation handoff without forcing native peer reflow between generations.
- Compatibility uncertainty fails native rather than leaving a partial replacement.

## Validation state

There is no open device-blocking defect on the current Build-473 stable/development baseline.

Accepted promotion evidence includes:
- Build 472 companion-app presentation: Fast #1778, signed Canary #506, maintainer device acceptance, post-merge `dev` Integration #1786;
- Build 473 Wi-Fi optical normalization: Fast #1782, signed Canary #509, maintainer device acceptance;
- synchronized Build-473 integration: Fast #1788 and post-merge `dev` Integration #1789;
- validation marker Build #1792 plus promotion-readiness #143 = READY;
- promotion PR #176 / Build #1793 = success;
- `main` promotion merge `7db7159a340642564bb389519da362f756f7a884`;
- post-merge `main` Build #1794 = success;
- main-push CodeQL #82: Python and Actions analysis both successful.

Build 466 remains the prior stable checkpoint and its evidence is historical, not the current stable baseline.

Historical rejected/superseded Builds and hypotheses remain in `DEVLOG.md`; do not restore them from old branches or chats.

## Active branch boundary

- The Build-472 presentation line and Build-473 Wi-Fi optical line are closed, promoted, and part of the current stable/development baseline.
- `main` and `dev` share the Build-473 promotion history; the documentation closure is text-only and must remain synchronized between both branches.
- The separate transition-animation PR #174 / `feat/control-center-transition-projection` remains open. Relative to the Build-473 promotion baseline it is 23 commits ahead and 5 commits behind and therefore diverged.
- PR #174 also changes `CombinedStatusPainter`; before any transition-line integration, it must synchronize with current `dev` and preserve the accepted Build-473 Wi-Fi optical-reference behavior rather than overwriting the renderer with its older base.
- Pre-promotion historical branches remain invalid continuation bases.

## Non-negotiable boundaries

- Root-cause-first; no geometry/motion/timing compensation without verified ownership.
- Preserve native HyperOS state/resource authority where available.
- Do not add duplicate polling, state machines, transition animators, or per-frame diagnostic work.
- Keep Home, Keyguard, QS_FAKE and final Control Center ownership boundaries explicit.
- Notification Shade and AOD remain native-only unless new exact-target evidence deliberately changes that contract.
- Runtime failure must restore/defer to native presentation.
- Current icon geometry remains frozen unless new device evidence reopens it.
- `CombinedStatus*` internal symbols may remain; public/app/package identity is Guiyuan / 归元 / `com.chaners.guiyuan`.

## Immediate next step

1. Treat Build 473 / the synchronized `main` + `dev` stable history as the accepted executable baseline for subsequent work.
2. Synchronize the separate Control Center transition-animation PR #174 onto current `dev` before further integration, explicitly reviewing its `CombinedStatusPainter` overlap against the accepted Wi-Fi optical-reference path.
3. Continue transition-animation adaptation without reopening the closed Build-472 companion UI or Build-473 Wi-Fi optical decisions unless new device evidence contradicts them.
4. Keep 1.0.0 gated by the release-qualification matrix and explicit maintainer authorization.

## Reference priority

For new work, use this order:
1. current `CONTRIBUTING.md`;
2. this `CURRENT.md`;
3. `ROADMAP.md` and applicable architecture/reference docs;
4. current source + exact-target SystemUI evidence;
5. historical DEVLOG/device evidence only as context, never as automatic authority.
