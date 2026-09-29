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

**Build 466 / `20260929-466`** is the current device-accepted and `main`-promoted Guiyuan 0.0.3 product baseline.

- Identity migration PR #168 was integrated into `dev` as `83cfd4d4be139dd3ec9cac870a8450a6dce09d08`.
- Exact-head Full #1690 and signed Work Branch Canary #492 passed before integration; the Build 466 package/module identity was accepted on device.
- Post-merge `dev` Integration #1693 passed target-profile verification, tests/build, Modern Xposed metadata, Haple signing/signature verification, non-debuggable Canary verification, and artifact upload.
- The canonical repository is now `CHS-Haple/Guiyuan`; repository history, branches, PRs and Actions history were retained through the rename.
- Promotion branch and `dev` were identical at `8fca6f0e450da0231efd9be302b1893bab6d391e`.
- Ready-state promotion Build #1707 passed; PR #170 merged to `main` as `be3cc0ae872b328d0a41d49a2599ec53950f3476`.
- Post-merge main Full #1708 passed the stable validation surface, including signing/signature verification and artifact upload.
- Existing `CombinedStatus*` implementation class/object names remain intentional internal identifiers; they are not a second public product identity.

### Accepted lineage inside Build 466

- **SystemUI runtime behavior:** Build 456 / `20260929-456`, integrated by PR #163 as `d70b416ba531651c6690027b7404b1854fdb3056`. Device validation accepts Home, Keyguard, AOD-native gating, QS_FAKE transition ownership, session-scoped native ignored-slot ownership, Hot Reload continuity, and cleanup restoration on the pinned target.
- **App icon:** Build 452, integrated by PR #166 as `44b10371e0709d155468f7f2e67307fde5f11ab2`. The Guiyuan mark uses the accepted rotationally symmetric adaptive-icon geometry.
- **Companion app:** Build 464, integrated through PR #165 as `a2394db92ce208771956defcd558c954065000f7`; Build 465 advanced the integrated display line to 0.0.3. Home Runtime Status, production-rendered Preview Sandbox, Features hierarchy, Diagnostics action styling, and MIUIX-aligned Sandbox controls are part of the accepted baseline.

### Active presentation checkpoint

**Build 467 / `20260929-467`** is the active companion-app presentation checkpoint on `feat/presentation-ui-polish`, branched directly from synchronized `dev@2163d3a8b9134e6114d6e59387b7c808d9399a08`.

- Preview Sandbox now uses a consistent MIUIX `body1` / `body2` hierarchy and bounded low-contrast dividers to separate live results, network-local controls, device-level SIM/airplane state, and battery controls.
- The Diagnostics app card places the existing background-free Guiyuan foreground mark beside the product identity. The foreground vector rotates linearly counterclockwise once every 18 seconds; because the center is circular, it remains visually stationary while the outer orbit/nodes move.
- `app_description` is shortened so the Diagnostics card and LSPosed module list do not repeat the adjacent product name.
- No SystemUI/Xposed runtime, state source, renderer, Hook, persistent preference, or production icon geometry changes are included.
- Executable source checkpoint before documentation closure: `68151be263ab020da2085b28b97c2019f986be4c`.
- Exact-head Fast validation is pending.

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

There is no open device-blocking defect on the promoted Build 466 baseline.

Accepted evidence includes:
- Build 456 runtime device acceptance and post-merge Integration #1651;
- Build 452 icon device acceptance and post-merge Integration #1596;
- Build 464 companion-app UI acceptance;
- Build 466 identity/package device acceptance;
- Full #1690, Work Branch Canary #492 and `dev` Integration #1693;
- ready promotion Build #1707;
- post-merge `main` Full #1708.

Historical rejected/superseded Builds and hypotheses remain in `DEVLOG.md`; do not restore them from old branches or chats.

## Active branch boundary

- Active companion-app presentation work: `feat/presentation-ui-polish` / Build 467, created from synchronized `dev@2163d3a8b9134e6114d6e59387b7c808d9399a08`.
- This branch is UI/copy-only at the product-runtime boundary and must not absorb the pending SystemUI transition-animation work.
- Pre-promotion historical `feat/*` / `fix/*` branches remain invalid continuation bases; old open PRs must be re-evaluated rather than merged by age/name.
- New runtime work still starts from the synchronized current `dev` baseline after this independent presentation checkpoint is closed.

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

1. Complete Build 467 exact-head Fast validation and focused presentation review; request a signed Canary only if device visual evidence is actually needed.
2. Integrate the accepted presentation checkpoint into `dev` without coupling it to SystemUI runtime work.
3. The next runtime checkpoint remains the Home/Keyguard -> QS_FAKE -> native Control Center **visual transition-animation** gap. Start that work from the then-current synchronized `dev`, not from this presentation branch.
4. For transition work, reuse verified native progress/geometry/alpha/appearance ownership; do not create a duplicate animator, fixed-pixel follower, fraction threshold, timer or delay patch.
5. Only after transition animation is accepted, continue adaptive sizing/spacing and later battery-ring color-source controls. Keep 1.0.0 gated by the release-qualification matrix and explicit maintainer authorization.

## Reference priority

For new work, use this order:
1. current `CONTRIBUTING.md`;
2. this `CURRENT.md`;
3. `ROADMAP.md` and applicable architecture/reference docs;
4. current source + exact-target SystemUI evidence;
5. historical DEVLOG/device evidence only as context, never as automatic authority.
