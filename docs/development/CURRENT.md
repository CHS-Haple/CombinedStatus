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

## Current phase

Phases 2A, 2B, 3 and 4 are complete for the validated baseline:

- **Phase 2A:** Home carrier / presentation architecture.
- **Phase 2B:** unlocked Home continuity + bounded Control Center transition ownership/geometry bridge is structurally complete; visual transition-animation adaptation remains pending.
- **Phase 3:** Keyguard / lockscreen / AOD steady-scene ownership is complete for the current target; Keyguard-originated visual transition animation remains pending with the shared QS_FAKE animation work.
- **Phase 4:** companion-app Home / Preview Sandbox integration.

The active executable work on the 0.0.3 line is **transition-animation adaptation across Home/Keyguard -> QS_FAKE -> native Control Center**, before adaptive sizing, spacing and broader visual controls.

Current candidate: `feat/control-center-transition-projection`, Build **470 / 20260929-470**.

Root-cause review now separates two facts:
- HyperOS native `onExpansionChanged(progress)` and fake/final Folme appearance ownership are still running and remain the motion/appearance authority.
- Guiyuan's session-long compact slot exclusion changes which child icons participate in the QS_FAKE layout, so the stock per-icon source/target correspondence is no longer available from the compact child layout alone.

Build 470 therefore keeps compact occupancy but adds a transition-only visual projection: common native peer slots are projected from HyperOS's selected source Views toward the final QS Views, while Guiyuan reuses its production painter and separates Battery / center / mobile components toward corresponding native endpoints. Raw native expansion progress is the baseline timeline. No project-local gesture animator or replacement fake/final fade is introduced; later optical shaping is allowed only if device evidence shows a small local adjustment is needed to stay visually coherent with HyperOS.

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

No pre-promotion `feat/*` or `fix/*` branch is a valid continuation base for Phase 5.

- `feat/visual-tuning-controls` is fully behind current `dev` and carries no unique current commits.
- Other inspected historical feature/fix branches are substantially diverged and/or still reference the pre-Guiyuan package layout.
- Old open PRs must not be merged merely because they remain open; their requirement must be re-evaluated against current `dev`.

New executable work must branch from the synchronized current `dev` baseline.

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

1. Close the remaining Home/Keyguard -> QS_FAKE -> native Control Center **visual transition-animation** gap before adaptive sizing or broader visual controls.
2. Start from the synchronized current `dev` baseline on a new focused `feat/*` branch; do not revive historical transition/layout branches.
3. First review the exact-target native progress, geometry, alpha and appearance ownership across source steady scene -> QS_FAKE -> final QS. Reuse verified SystemUI motion/appearance state; do not create a duplicate project-local animator, fixed-pixel follower, fraction threshold, timer or delay patch.
4. Keep carrier/scene ownership, stable geometry, transition geometry and animation presentation as separate responsibilities. Any Guiyuan-owned interpolation must be a narrow derivation from authoritative native transition facts and must fail native when those facts are unavailable.
5. Only after transition animation is accepted, continue adaptive sizing/spacing and later battery-ring color-source controls. Keep 1.0.0 gated by the release-qualification matrix and explicit maintainer authorization.

## Reference priority

For new work, use this order:
1. current `CONTRIBUTING.md`;
2. this `CURRENT.md`;
3. `ROADMAP.md` and applicable architecture/reference docs;
4. current source + exact-target SystemUI evidence;
5. historical DEVLOG/device evidence only as context, never as automatic authority.
