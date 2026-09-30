# Current Development State

This file is the concise recovery point for active Guiyuan development.

## Accepted baseline

- Guiyuan 0.0.3.
- Accepted runtime/build baseline: Build 473 / 20260929-473.
- main and dev share the accepted Build-473 stable history before this workflow simplification.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Modern Xposed API 102.
- Build 472 companion-app presentation and Build 473 Wi-Fi optical normalization are accepted behavior.

Historical CI numbers, promotion mechanics, rejected Builds, and earlier hypotheses remain in DEVLOG/GitHub history.

## Active objective

Control Center transition animation is the active runtime task.

Current implementation line:
- PR #177 — feat/control-center-transition-matrix;
- current work-branch checkpoint: Build 501 / 20260930-501;
- exact-target direction: QS_FAKE role-5 to final-QS role-6 matrix projection;
- HyperOS remains translation/appearance authority; raw native expansion drives transition geometry and native fake/final Folme owns visual handoff;
- Guiyuan reads native transition state and renders only its owned Trinity correspondence; final role-6 top-level slots are read-only occupancy witnesses, while the existing QS_FAKE statusIcons-paddingEnd owner now provides a progress-synchronous semantic reservation so surrounding native peers move through SystemUI's own measure/layout path;
- Trinity transition is component-driven: Painter owns source optical bounds/semantic target/shape policy; role-6 top-level slots own final occupancy, child/drawable data refines optical alignment, and reservation width is the union of native-progress-interpolated semantic spans rather than the rendered pixel envelope;
- generic peer projection, Guiyuan-owned network composition, and native-only unsupported peers remain separated.

PR #174 is an older transition route and must not overwrite the newer active matrix line or accepted Build-473 renderer state.


Build 497 is the current focused Keyguard-island checkpoint. Build-496 device evidence shows that near the final Keyguard -> Control Center handoff, native Super-Island `showing=true` changes the native status-container geometry while the transition owner simultaneously disables progress reservation for `charging && nativeIslandShowing`. That clears the fake-status-icons reservation from ~300 px back to compact 105 px, then reapplies it when island/appearance state changes, producing a one-frame whole-row rebase in which ordinary peer slots such as VPN can flash. Build 497 makes the reservation policy scene-specific: HOME charging+island keeps Build-494 native-peer-motion behavior, while KEYGUARD retains continuous progress-synchronous reservation through the island handoff. No curve, callback phase, lease, island event source, Home behavior, or native visibility writer is changed.


Build 498 isolates the remaining Keyguard Super-Island hitch from diagnostic overhead. Build 497 device validation removes the one-frame VPN/layout flash, confirming the Keyguard reservation discontinuity is fixed, but a terminal hitch remains whenever the lockscreen Super-Island replay animation runs, independent of charging. The island hook remains read-only runtime state authority, but its 900 ms `OnPreDrawListener` diagnostic sampler is removed completely. Detailed diagnostics now records only the single island event snapshot; no per-frame island geometry traversal/logging runs during the animation. Transition, reservation, Keyguard lease/callback phase, island animation ownership, and Home behavior are unchanged.


Build 498 device validation is now accepted for its isolated goals: the Keyguard + Super-Island terminal hitch no longer reproduces after removing the diagnostic frame probe, and the Build-497 VPN/whole-row terminal flash remains gone. The Build-496 master-switch fail-native transaction is also device-accepted: disabling Guiyuan restores native presentation normally without requiring a Control Center pull. These accepted safety/performance results are protection boundaries for Build 499.

Build 499 returns to the deferred transition-geometry defects without changing those accepted boundaries. HyperOS native presentation now owns the No-SIM semantic before stale mobile cache can participate, so native `no_sim` immediately suppresses old subscription/signal/type state. Steady source basis remains frozen, but external motion follows the real source/fake/final `MiuiStatusIconContainer` carrier chain instead of projecting every component from one static source origin; this targets the visible vertical-first/high trajectory while keeping HyperOS progress as the sole gesture timeline. Final ImageView/StatusBarIcon targets use their live drawable frame rather than the whole slot box, so Mobile, Airplane and No-SIM optical endpoints inherit native glyph geometry. Participants that are actually hidden by the current Guiyuan presentation but have no independent compact source (for example an additional SIM, or supplemental Airplane/No-SIM semantics) use one ownership-gated latent projection: they share the same native carrier path, remain invisible while crossing other icons, and reveal only near their final native slot. Unmatched compact content, including the Mobile dots/unavailable-mark group when it has no native destination, follows the carrier and exits quickly without a separate shrink timeline. No new hook, animator, polling loop, native translation writer, or timing constant is introduced.

Build 500 corrects the transition-specific geometry regressions exposed by Build 499 without changing steady layout. Device evidence shows three linked symptoms at gesture entry/final placement: the four Mobile dots jump downward on press, charging scenes still shift Trinity left on press, and decomposed endpoints can be biased. Root-cause review found two authority errors. First, carrier-relative projection normalized source/target offsets by each `MiuiStatusIconContainer` width/height and reapplied them using the live fake carrier size; the steady/fake rows have different heights, so a below-center compact component is displaced even at `p=0`. Build 500 keeps only the native carrier center translation and interpolates component offsets in physical pixels, so carrier-size changes cannot rescale Trinity internals and target geometry remains exact when carrier centers converge. Second, transition source position was rebuilt from the inner `battery_icon_container` center even though steady Guiyuan is end-anchored to a stable host slot; charging widens the outer Battery presentation and reintroduces the old left discontinuity. The retained witness now derives source position from the same Home/Keyguard overlay-host end slot used by steady layout, while the render View remains basis authority. The obsolete battery-anchor dependency is removed. Airplane, No-SIM and additional-SIM participants now share one latent policy: they use the final native target basis immediately, remain transparent until one final native slot-width of spacing has opened relative to the carried compact source, then reveal quickly over a short additional slot distance. No dedicated dual-SIM animation timeline remains. Build-491 callback/lease, Build-497 Keyguard reservation and Build-498 island-performance fixes remain untouched.


Build 501 follows Build-500 device evidence for latent single-icon projection and Control Center scene cleanup. Supplemental Airplane/No-SIM targets were resolving as whole status-icon slots when no semantic child entry was available; the resulting 77×75-style slot basis made 0→1 icons visibly oversized. Build 501 requires a unique drawable-bearing native ImageView for these single-icon latent targets and fails native for the latent frame if real optical geometry cannot be resolved, rather than falling back to the slot box. The same device run shows that enabling supplemental Airplane changes the entire Trinity decomposition path. Review found a feedback loop: latent Airplane/No-SIM/additional-SIM spans expanded the QS_FAKE statusIcons end reservation, while that same statusIcons row is the native motion carrier for every projected component. Latent participants therefore no longer own transition reservation; only components with an actual compact Trinity source may change that reservation. Finally, collapse no longer leaves the Control Center compact native presentation session alive after the Guiyuan overlay becomes hidden. `requestedVisible=false` now restores the existing ignored-slot/clip/reservation ownership synchronously; the attached fake host/render session may remain for reuse, and the next visible Control Center reacquires native presentation through the existing attach path. No Bluetooth-specific visibility rule is added.

## Current architecture boundary

### Home
SystemUI owns surrounding layout, Battery/slot behavior, tint semantics, scene visibility, and charging/Super-Island motion. Guiyuan owns its compact composition and narrowly scoped reversible presentation state.

### Control Center
Home/Keyguard may bridge through verified QS_FAKE transition ownership. Fully expanded Control Center remains native. Guiyuan must not create a second gesture timeline or take ownership of final Control Center layout/alpha/translation. Notification Shade remains native-only on the pinned target.

### Keyguard / AOD
Keyguard has an independent host/session while sharing domain/render semantics. AOD remains native-only. Home and Keyguard do not share mutable host ownership.

## Validation state

Build 473 is device-accepted and stable.

The active Control Center transition line is not yet accepted. Build 480 is device-rejected on non-charging Home because full-target affine interpolation visibly flattened Trinity elements and parent-View targets collapsed unrelated semantics into the same geometry. Build 481 is also device-rejected: Mobile did not reliably move when internal final children were 0×0, Wi-Fi optical size/endpoint did not coincide with the native glyph, transition tint did not consistently follow final native peers, and child-first targeting did not represent real final slot occupancy. Build 482 replaces child-first targeting with read-only role-6 top-level slot witnesses; internal children/drawables only refine optical alignment, raw native expansion owns external motion, native fake alpha drives the local Mobile morph, and final SystemUI appearance remains native-owned. Exact-head Runtime Build #1833 and signed Work Branch Canary #522 pass for source `cfdf12ff4c2e8249f833e52e871cb35f1bad953b`. Focused device feedback says Build 482 is substantially improved and its Battery/Wi-Fi trajectory is the explicit rollback baseline, but it is not accepted because decomposed elements do not create live layout occupancy, surrounding native peers therefore do not move with the split, and Mobile needs a staged dots -> row -> bars morph before native handoff.

Build 483 kept the Build-482 external trajectory and added semantic reservation plus a local dots -> row -> bars Mobile morph. Build 484 then split component scale policy, added fast exit for semantics without a destination, and attempted a Battery ring -> Battery outline morph.

Build-484 device review rejects three details without rejecting the overall matrix/reservation line: (1) the Battery morph used the whole 105x169/135x169 MiuiBatteryMeterView fallback as if it were the glyph and therefore produced an oversized, hard-looking Battery; (2) the transition source sampled MiuiBatteryMeterView each frame, so charging/status-bar press transforms could flatten the whole Guiyuan source and every later component trajectory; (3) Mobile bars grew only upward from a bottom baseline and remained too short relative to the final native signal.

Build 485 removed the charging-press flattening but device review rejects its synthetic Battery contour, exposes a new external trajectory drift from making renderView the full source authority, leaves the Mobile signal baseline visually wrong, and shows 5G overlapping signal when a semantic child target is unavailable.

Build 486 restores the accepted pre-custom Battery treatment: the ring uses the Build-482 fold/projection handoff and no synthetic Battery body is drawn. Source ownership is split: MiuiBatteryMeterView contributes native center/translation while Guiyuan renderView contributes the stable basis, so transient charging/press scale cannot flatten Guiyuan without replacing the accepted native trajectory coordinate authority. Mobile first forms a row, then all four bars grow upward from one shared fixed lower baseline for the entire bar-growth phase. Target resolution now follows one fail-fast rule: semantic children such as wifi_signal, mobile_type(_single), and mobile_signal must be visible, attached and non-zero; otherwise that component uses the existing fast fade/slight-shrink exit instead of guessing a top-level slot position. HyperOS remains external progress, final appearance, peer-layout, and themed-asset authority.

Build 487 adds HyperCeiler dual-row signal compatibility plus bounded semantic fallback and has exact-head Runtime CI #1846 green; signed Canary #528 is available for focused dual-row validation.

Build 488 addresses a separate Keyguard-originated terminal handoff defect seen in device video. Frame review shows a short interval near fully expanded Control Center where the outgoing Keyguard status row is restored/re-laid out while the incoming final Control Center status row is also visible, producing an apparent one-frame stall/offset across native peers such as VPN and Bluetooth-device battery. The existing Keyguard renderer reports transient readiness loss as its host geometry disappears, and the module immediately restores represented native slots/reservation and revokes KEYGUARD Control Center eligibility. Build 488 adds no delay: once a KEYGUARD-originated Control Center gesture has non-zero native expansion, the already-established Keyguard presentation owns a lifecycle lease until native expansion returns to zero or an authoritative break occurs (AOD, real host detach/failure, feature disable, or Control Center source resolves away from KEYGUARD). During that lease, transient steady-source HOME/raw-state changes and renderer layout-readiness loss do not restore the outgoing Keyguard row. Final Control Center remains native-owned.

Bluetooth-device battery tint remains observationally separate: Guiyuan has no writer for `bluetooth_handsfree_battery` tint/alpha/visibility. HyperCeiler's current `StatusBarIcon` implementation only changes HyperOS RIGHT_BLOCK_LIST / CONTROL_CENTER_BLOCK_LIST membership for that slot. No tint mutation is added in Build 488; recurrence should be captured with a read-only slot-tint probe before any compatibility fix.


Build 489 keeps the Build-488 Keyguard handoff lease and fixes two independent transition details in the same test package. First, charging exposes a 135px `MiuiBatteryMeterView` around a stable 105px `battery_icon_container`; using the outer BatteryView center as transition position authority creates a 15px compact-source discontinuity at gesture entry. The transition source now uses the real compact carrier as position authority while renderView remains the stable basis/size authority, preserving the Build-486 anti-flattening split without a numeric offset. Second, the Mobile dots still row first, then bars expand vertically in both directions: a shared downward expansion derived from the shortest bar keeps all lower edges collinear, while each bar's remaining height grows upward. No additional animation timeline is introduced.

Build 488 is device-rejected as insufficient for Keyguard gesture responsiveness. The lifecycle lease can preserve ownership at handoff, but the 488 diagnostic shows the transition still updates `statusIcons.paddingEnd` nearly every display frame through `setPaddingRelative`. That is a layout-path mutation, not a draw/property animation, and it adds a one-layout-behind peer-row motion on an already busy Keyguard/Control Center handoff. Build 490 therefore keeps the lease but removes progress-synchronous padding reservation for KEYGUARD-originated transitions. Keyguard native peers remain entirely on HyperOS fake-root/child translation motion; Guiyuan only renders its own component transition. HOME keeps progress reservation unchanged pending separate evidence. Build 489 compact-carrier source continuity and bidirectional Mobile growth are retained.

Build 490 is device-rejected. 120fps device video isolates three separate defects: (1) charging transition source discontinuity — the Guiyuan ring center moves from approximately x=1307.7 to x=1277.6 in one 8.3ms frame while the adjacent native peer row remains effectively stationary; therefore the QS_FAKE carrier is not the steady visual source authority. (2) Keyguard reservation removal causes native peers to collapse into the decomposed Guiyuan transition region. (3) during a fast Keyguard fling, old QS_FAKE/Guiyuan and final Control Center status rows overlap with visibly different geometry for roughly 10 frames (~83ms), while slow pulls hide the mismatch. Build 491 restores verified-scene semantic reservation, commits expansion-driven reservation before HyperOS consumes the same native sample, freezes HOME/KEYGUARD steady render geometry as the transition source when available, and lets native final-surface alpha only catch outward geometry up during the native appearance handoff. No custom duration/threshold is added. Mobile shared downward growth is also corrected to half of the tallest bar's extra height so the tallest bar grows symmetrically about the original dot row while all four bottoms remain collinear.
Build 492 follows the 491 device result without reverting the accepted callback-phase/Keyguard-lease correction. Build 491 removes the fast-lockscreen handoff stall, but its intended steady-source freeze never becomes active in the supplied diagnostic: all sampled transition buckets report `sourceOrigin=qs-fake-live`. The implementation had coupled a read-only transition geometry witness to current presentation `layoutReady` and also required the steady render View to share the exact `rootView` object with `NotificationShadeWindowView`. Home/Keyguard steady presentation may legitimately become non-ready/hidden before the projected handoff while its last laid-out geometry remains valid, and Home may live in a distinct window root. Build 492 separates those contracts: an attached steady render View with retained non-zero bounds remains eligible as a one-shot transition witness even when presentation readiness has yielded; its transform is converted between distinct window roots using native screen/window origins before projection into the transition root. Same-root behavior is unchanged, QS_FAKE remains compatibility fallback, and no offset constant, new animator, native translation writer, or Battery width rule is introduced. This targets the remaining press-entry/charging left shift and the apparent peer-gap divergence while preserving Build 491's now-device-confirmed Keyguard responsiveness.

Build 493 adds explicit semantic expansion for states whose fully expanded native status row contains more independently visible semantics than the compact Trinity source. The correspondence model is now explicit: existing components remain 1→1 morphs; an additional final mobile subscription is a 1→N split from the compact Mobile source; Wi-Fi with airplane mode is a 0→1 reveal for the independent final airplane slot. These projected extras are drawn only by Guiyuan's transition overlay and use read-only final slot geometry; native final alpha/visibility/translation remain untouched. Secondary Mobile reads its own subscription signal level rather than cloning the primary SIM. Reservation spans include the same extra final semantics so peer layout and projected drawing describe one occupancy set. Mobile capsule height also changes from an empirical 0.90 target factor to an optical budget that subtracts one round-cap radius from the native target height before bounding the bar body, keeping the rounded endpoint inside the intended visual envelope.

Build 494 is device-rejected. It does not restore native-equivalent transition motion: with Guiyuan enabled the row first drops vertically and only then joins the native lower-left trajectory, charging press entry remains left-biased, and charging + island can still end with peer overlap. Those motion defects remain queued after the master-switch safety regression below.

Build 495 is a safety-only checkpoint after device evidence showed that disabling the Guiyuan master switch can leave native status icons suppressed across scenes. Root cause review found three reacquisition gaps: Control Center eligibility did not include `settings.enabled`; Home presentation readiness could call `SystemUiHomePresentationOwner.activate()` after disable; and runtime feature changes were never forwarded to `SystemUiNativeCombinedParticipantOwner`, so its battery/network suppression handoff could remain active. Build 495 makes master-switch-off a hard acquisition gate, routes the setting to the native participant owner, releases Home/Keyguard/Control Center presentation ownership plus native battery/network suppression, and adds a race-safe guard to the native participant handoff callback. No Build-494 transition geometry or animation code is changed.

Build 495 device validation narrows the remaining master-switch failure: disabling Guiyuan restores Home presentation bookkeeping, but the icons previously covered/suppressed by Guiyuan remain absent in steady Home while Control Center native icons are correct. The diagnostic proves the release transaction is split across threads: Home render feature/handoff updates run on the SystemUI main thread, while `homePresentation cleanup source=feature-disabled` executes on the RemotePreferences callback worker. Build 496 moves the entire feature-settings ownership transaction onto the SystemUI main looper before touching native participant state, presentation owners, suppression owners, or layout. It adds no visibility writer and does not change transition motion.


Device testing is requested only when the result can change implementation choice or acceptance. Mechanical/documentation steps continue without a new APK round trip.

## Non-negotiable boundaries

- Root-cause first; no speculative geometry/timing compensation.
- Prefer authoritative HyperOS state/resources/motion.
- One mutable runtime property has one writer.
- No duplicate polling, state machines, gesture animators, or high-frequency diagnostics.
- Keep stable geometry, transition geometry, native occupancy, and optical adjustment separate.
- Cleanup/Hot Reload restores only Guiyuan-owned state.
- Compatibility uncertainty fails native.
- Accepted Build-473 Wi-Fi optical behavior must not be overwritten by older transition branches.

## Immediate next step

1. Run exact-head Runtime CI for Build 499 and produce one signed work-branch Canary from that exact source.
2. Device gate: verify native No-SIM entry/exit, supplemental participant reveal, Mobile native optical height, and the full Home/Keyguard Control Center trajectory in both directions.
3. Regression gate: Build-498 Keyguard + Super-Island must remain hitch-free with no VPN/whole-row flash; Build-496 master-switch fail-native behavior must remain accepted.
4. Only after those gates pass should PR #177 be considered for integration.

Historical safety checklist (retained for traceability): with Guiyuan enabled, disable the master switch while watching steady Home. Previously covered/suppressed native icons must return immediately without pulling Control Center.
3. Still disabled, pull and collapse Control Center once; steady Home must remain native-correct before and after the gesture.
4. Re-enable Guiyuan and verify compact presentation reacquires without SystemUI restart/Hot Reload.
5. Only after this gate passes, resume transition motion ownership work: native peers move directly lower-left, while Guiyuan currently inserts an incorrect initial vertical-only segment. Charging press-left bias and charging-island final overlap remain queued with that motion review.

## Reference priority

1. CONTRIBUTING.md;
2. this file;
3. task-specific architecture/reference docs;
4. current source and exact-target SystemUI evidence;
5. relevant historical DEVLOG entries when needed.
