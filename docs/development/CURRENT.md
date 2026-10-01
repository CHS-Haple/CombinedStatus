# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.3.
- `main` and `dev` are synchronized at Build 511 / `20260930-511`.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- Build 510 non-charging Home transition and Build 511 charging Home transition are device-accepted.
- The current transition ownership, reservation, Fail-native, and release-parity boundaries from Build 511 remain protected.

## Active objective

PR #181 / `feat/battery-top-readout` now owns the battery-information controls, network/battery content layout, preview synchronization, and the associated steady/transition source geometry while preserving the accepted Home -> Control Center ownership contract.

Current checkpoint:
- Build 551 / `20261001-551` expands Preview Sandbox mobile-network coverage to None / 2G / E / 3G / H+ / 4G / LTE / 5G / 5G-A. Runtime rendering remains generic/native-label driven; no per-standard production rendering fork is added. The original four preview enum ordinals are preserved for rememberSaveable compatibility, while the UI uses an explicit display order and a horizontally scrollable MIUIX segmented row to avoid compressing nine labels into one screen width. Tests lock common native-label pass-through and legacy preview ordinals. Runtime CI pending; real-device Canary is required only to review the expanded sandbox control's ergonomics.
- Build 550 / `20261001-550` front-loaded continuous retract AB; maintainer feedback on Build 549 says total completion is now early enough but the first half still feels too similar to the previous version. Build 550 keeps the 35% completion point and adds one continuous input warp (`p + 0.45*p*(1-p)`) before the existing smoothstep, increasing consumed ring length from 15.6% -> 26.1% at local p=0.25 and from 50.0% -> 66.6% at local p=0.50 while preserving 0/1 endpoints, continuous topology, native CENTER motion, Battery handoff, and Build-542 island behavior. Runtime CI and exact-head Canary are pending.
- Build 549 / `20261001-549` faster continuous retract AB; Build-548 device video confirms continuity and topology are accepted while a short outlet-side residual arc still remains visually late. Build 549 changes only `TRANSITION_COMPLETE_PROGRESS` from 0.45 to 0.35; the same symmetric smoothstep, direction-aware ordered arc consumption, CENTER/native target path, Battery handoff, reverse symmetry, and Build-542 island behavior remain unchanged. Runtime CI and exact-head Canary are pending.
- Build 548 / `20261001-548` faster continuous retract AB; Build-547 device video confirms the remaining defect is still late ring clearance rather than topology, endpoint, or discontinuity. Build 548 changes only `TRANSITION_COMPLETE_PROGRESS` from 0.60 to 0.45, preserving the same symmetric smoothstep, direction-aware ordered arc consumption, CENTER/native target path, Battery handoff, reverse symmetry, and Build-542 island behavior. Runtime CI and exact-head Canary are pending.
- Build 547 / `20261001-547` faster continuous retract checkpoint; Runtime CI #2060 is green at `57da4ee71d4adc9f9e496a1115bbad2a365146c8`. Maintainer device evidence on Build 546 confirms the restored continuous retract removes Build 545's discontinuity, but the 80%-clock completion still feels too slow. Build 547 changes only `TRANSITION_COMPLETE_PROGRESS` from 0.80 to 0.60; the same smoothstep, direction-aware arc topology, CENTER/native target path, Battery handoff, and Build-542 island behavior remain unchanged.
- Build 546 / `20261001-546` continuous accelerated retract checkpoint; Runtime CI #2058 is green at `8ac5dd1fa9ac60d271aad02e8dbbaed63bca0947`. Maintainer device evidence rejects Build 545's live optical gate: the ring develops a conspicuous visual discontinuity during the pull, with the outlet-side arc disappearing as a chunk rather than continuing the accepted Build-543/544 retract. Build 546 removes the gate implementation entirely, restores Build 544's continuous arc topology, and only remaps Battery-ring retract progress so the ring completes at 80% of the existing transition clock while retaining the same smoothstep, native target path, and reverse symmetry.
- Build 545 / `20261001-545` live optical exit-gate checkpoint; Runtime CI #2056 is green at `4c38b59682d0bcabcf91fa09360da4ae4770b542`. Build 544 improves direction but maintainer device feedback says the outlet still yields too slowly. Build 545 does not globally accelerate the accepted 543 retract: it maps the live CENTER optical envelope into the current Battery local frame, opens the left-side portal early according to the live envelope width/height, and feeds only a minimum consumed-sweep floor into the existing retract. Once the original 543/544 retract catches that floor, it resumes sole authority for the remaining tail.
- Build 544 / `20261001-544` direction-aware battery-ring exit checkpoint; Runtime CI #2054 is green at `2b281befc98c4a20b2967f53b4bfd4dadb1f2a73`. Maintainer feedback accepts Build 543's arc-length retract visual quality but identifies one remaining choreography defect: the CENTER/network semantic can travel through the still-visible battery ring. Build 544 keeps all 543 motion timing and native targets, samples the real CENTER source->target horizontal direction, and only for a leftward exit consumes the ring from the left side first; unresolved CENTER targets Fail-native to Build-543 behavior.
- Build 543 / `20261001-543` battery-ring retract transition checkpoint; Runtime CI #2051 is green at `6cf21e85219fb2cc16f5da8edd0aa5ec39b24c89`; the runtime replaces the rejected Y-axis Battery fold with a transition-only ordered arc-length retract while preserving the existing native Battery target path, component handoff, reservation ownership, and Build-542 island-boundary projection;
- Build 542 / `20261001-542` is maintainer device-accepted at frozen head `f280c6c4b1e744843ec5b6e603bb2aa653b5f399`; exact-head Runtime CI #2050 and signed Work Branch Canary #610 are green, and focused real-device validation reports the Super-Island Home -> Control Center peer flow is normal with no extra sequential disappearance;
- Build 541 exact-head Runtime CI #2038 and signed Work Branch Canary #608 are green at `15cd544ed68d5303c78aa6e433242aee6f6fd4bc`, but maintainer device evidence rejects the peer-`forceAppear` correction: `reservationMode=native-progress-fake-island-freeze`, `islandPeerFreeze=active:3`, and a growing non-negative `nativeReservation` were all present while native peers still disappeared during the island pull; this route is disproven and must not be revived;
- Build 542 ordering-corrected Runtime CI #2049 is green at `36ba3c2e0a7dba9f8e253c2649523ea0b94a87e4`; exact JADX proves private `MiuiStatusIconContainer.getIslandTranslationX()` returns the active monitor's `getIslandWidth()`, and `calculateIconTranslations()` uses that value as the X collision boundary; the accepted runtime keeps HyperOS as the sole `islandWidth` owner and projects only that getter for the active QS_FAKE container;
- Build 540 exact-head Runtime CI #2032 and Work Branch Canary #607 are green at `3ea8a533179ec1a08fb48228109088e5f49cafb8`; cold-start device evidence successfully captured the QS_FAKE island contract and confirms the Build-539/540 guard still blocks native peer reservation during an island pull;
- Build 539 exact-head Runtime CI #2028 is green at `e41c7d77d60689f8c08fdc36bdc0d40fa724f554`; maintainer device evidence indicates the island guard suppresses the premature avoidance symptom but also removes the required continuous leftward peer reflow because the same native padding expansion was disabled wholesale;
- Build 538 exact-head Runtime CI #2027 and Work Branch Canary #604 are green at `0a3558fc1bff90904b177345ec5ee80275bcb0ec`; focused device evidence accepts the independent layout profiles / live TopSlot avoidance checkpoint but exposes one island-only Control Center regression: HyperOS hides the native end-side container while Guiyuan pixels are still visually far from the island;
- Build 537 exact-head Runtime CI #2025 and Work Branch Canary #602 are green at `5c7560769e2ff0926fba6a78eba022151aee1dd0`; maintainer feedback identifies two follow-up defects: layout-local settings are shared instead of independently remembered, and TopSlot network avoidance can retain an over-wide envelope when the visible network semantic becomes smaller;
- Build 536 / `20261001-536` is maintainer device-accepted: upward offset is visibly continuous, the logical-viewport / physical-overflow split works, and the 40%-160% size ranges are accepted;
- branch remains based on current `dev` and is not behind it;
- Build 524 improved the final native Battery-number target for HyperOS hollow-battery presentation;
- Build 525 corrected two Build-523 device defects:
  - positive offset now uses real render-View headroom instead of treating canonical `y=0` as a physical clip edge;
  - charging glyph Y aligns its alpha-weighted visible-ink center to the percentage text optical center;
- Build 525 Runtime CI #1966 passed, including the new physical-headroom and alpha-centroid tests;
- the pending Build-525 Canary request is superseded by Build 526 and must not be used as the integration checkpoint.

Build-523 device evidence also identified an independent Control Center peer-motion regression:
- Super-Island alone is normal;
- charging without that combined condition is not the reported failure;
- the failure is specifically Super-Island + charging, where surrounding native status icons do not follow the expected endpoint rule;
- the same Build-523 diagnostic reports `addBatteryIsland=false / batteryWidthDiff=0` during the affected pull while Guiyuan selected `reservationMode=native-peer-motion`.



Build 544 direction-aware ring exit:
- Build 543's core arc-length retract is visually accepted by the maintainer ("效果很好"), but the combined decomposition is not yet accepted because the CENTER/network icon can visibly pass through the still-present battery ring while moving toward its native target;
- the defect is choreography, not CENTER path geometry: changing the network path would break the already-accepted native-like direct motion, and drawing it behind the ring would merely turn "穿过" into "钻过去";
- Build 544 therefore keeps CENTER/native target geometry, 543 smoothstep, total remaining ring length, Battery target motion, battery-number/lightning handoff, and Build-542 island projection unchanged;
- each frame performs a read-only CENTER target sample using the existing cached target resolver and computes the real horizontal source->target delta. LEFT / RIGHT / NONE are derived from geometry rather than hardcoded RTL assumptions;
- LEFT exit changes only which ordered arc-length window remains: the left side is consumed first so the CENTER semantic gets a visual exit opening. RIGHT and NONE preserve Build 543's prefix behavior exactly;
- active battery color is not reinterpreted. For LEFT exit it is clipped by the intersection between the original battery-fill interval and the retained ring interval; RIGHT/NONE keep Build 543's active-length formula unchanged;
- missing or unreliable CENTER target resolves to NONE / Build-543 behavior. No speculative direction is used;
- Runtime CI #2053 correctly failed one existing Build-543 regression test because the first implementation accidentally changed NONE active-fill semantics (75% battery / 50% retract became 120° active instead of the accepted 90°). That implementation was not shipped;
- commit `2b281befc98c4a20b2967f53b4bfd4dadb1f2a73` restores the accepted NONE/RIGHT semantics and adds explicit regression coverage. Runtime CI #2054: success.

Build 543 StatusBar Duo-inspired battery-ring retract:
- reference study is based on the maintainer-provided `StatusBar Duo_1.2.0.apk` (SHA-256 `a4e3467e847f7de40b201e1ae607719dfcba007b7f02cc6b424b1b3cbbffee0d`), decompiled with JADX 1.5.6;
- the useful visual contract is arc-length retraction, not a ring-to-battery topology morph: radius and stroke remain stable while the drawable circular path shortens from its ordered start; active battery color overlays the same shrinking prefix;
- Guiyuan does not copy Duo's independent `expandSpan`, animator, or native-reveal hooks. It consumes the already-verified Guiyuan `motionProgress`, applies a reversible smoothstep remaining-length function, and leaves native target geometry / appearance ownership unchanged;
- `BATTERY_FOLD` and the 0.72 Y-axis squash are removed. `BATTERY_RETRACT` is transition-only; steady Home drawing still passes `ringRetractProgress=null` and retains the accepted ring / top-readout / charging / color behavior;
- when the top number or charging glyph creates a dynamic ring gap, the transition treats the post-gap drawable arcs as one ordered path and consumes one shared sweep budget across them; individual segments do not shrink independently;
- no new Hook, Animator, timer, native View writer, interpolation clock, or transition owner was added;
- Build 543 requires focused visual device validation before it can replace Build 542 as an accepted rollback baseline.

Build 526 root-cause correction:
- the old reservation policy used `charging && SystemUiIslandMotionSource.currentIslandShowing()` as a proxy for HyperOS Battery-Island ownership;
- that proxy is too broad: a generic Super-Island can be showing while charging even when `ControlCenterHeaderExpandController.isAddBatteryIsland == false`;
- Build 526 carries the exact native `isAddBatteryIsland` Boolean through the existing Control Center callback/update path;
- Home semantic reservation is enabled only when charging is false or exact `isAddBatteryIsland == false`;
- generic island + charging with exact `isAddBatteryIsland=false`, non-island charging, and island-only keep semantic reservation;
- an unknown Battery-Island read fails native and does not claim semantic reservation;
- no local `batteryWidthDiff`, translation, endpoint, duration, or trajectory compensation is introduced.

Build 527 mobile-type typography correction:
- maintainer video evidence shows the compact Guiyuan `5G` visible ink is still about 1.35–1.40× the final native HyperOS `mobile_type` ink on the pinned target;
- compact mobile-type main text is rebased from 39 to 29 canonical px; suffix size/rise are reduced proportionally so 5G-A/4G suffix composition follows the same scale;
- target position/size convergence remains owned by the existing native `mobile_type_single/mobile_type` witness and transition matrix;
- when that exact native target is a TextView (or exposes one directly), its Typeface weight is read and the Guiyuan text weight interpolates continuously from the compact source weight to the native target weight using the existing HyperOS motion progress;
- if native target typography is unavailable, weight remains unchanged rather than guessing a target;
- no second animator, timing curve, native text writer, or screenshot-specific endpoint is added.

Build 528 latent-reveal timing correction:
- new device feedback confirms no-source / latent participants now wait for real occupancy correctly, but their opacity still reaches 100% too late on fast pulls, leaving a visibly empty target slot until the final part of expansion;
- root cause is the Build-509 rule `min(targetProximity, reservationProgress)`: both terms had to approach 1, so full opacity was mathematically tied to near-terminal target convergence;
- Build 528 preserves both existing safety gates: zero reservation still means invisible, and content outside one real target visual extent remains invisible;
- after both gates open, opacity now completes over the first 35% of the existing spatial reveal windows, using the same stateless smoothstep mapping for occupancy and target proximity;
- no delay, timer, Animator, new gesture curve, target coordinate, or native visibility writer is added;
- reverse collapse stays symmetric: occupancy/proximity falling back through the same window hides latent pixels before the slot fully closes.


Build 529 Battery ring -> native Battery shape-local morph:
- Build 528 Runtime CI #1982 is green and remains the previous rollback checkpoint.
- The old `BATTERY_FOLD` presentation only applied a whole-component Y squash (`1.0 -> 0.72`), which visually produced a flattened ring rather than a ring becoming a battery.
- Build 529 removes that whole-component squash and keeps the existing native `BatteryIcon` witness/motion matrix as position + outer similarity authority.
- The ring itself now morphs locally:
  - the source 240° open ring is sampled as one continuous perimeter parameter;
  - its two lower source ends map to the same final bottom-center point, so they gather inward while the native target height pulls them upward;
  - the source top midpoint maps to the final top midpoint;
  - the final local outline is a rounded battery silhouette whose aspect ratio is derived from the exact native Battery target width/height;
  - local axis compensation only restores the aspect ratio lost by the outer similarity matrix, so final root-space width/height remain native-owned.
- When the top percentage cutout exists, the cutout closes during the first half of the shape morph while the separately-owned Battery-number component leaves for its native target.
- The charging glyph does not receive an independent trajectory; when a native Battery target is available it fades out early, otherwise it preserves the prior Fail-native behavior.
- No native Battery View property, translation, alpha, visibility, or drawable is written by the morph.


Build 530 rollback + transition-only optical text convergence:
- Build 529 Battery ring topology morph is rejected by device visual review and fully rolled back to the Build-528 Battery fold implementation.
- Build-527 compact Mobile Type steady typography change is also rolled back: source/steady Mobile Type returns to the pre-change 39/23/8 geometry and original weight policy.
- Battery-top source typography is not altered by Build 530; user-configured/source size and weight remain authoritative in steady state.
- Size/weight convergence is now transition-only:
  - native target TextView weight is observed read-only;
  - each transition frame resolves the currently interpolated weight;
  - the current glyph optical bounds are remeasured;
  - the component matrix maps those current ink bounds to the existing native target geometry.
- Therefore p=0 preserves the original Guiyuan visual exactly, while p=1 maps the target-weight glyph ink envelope onto the native target optical envelope.
- Charging lightning no longer belongs to the Battery-body transition. During pull-down it is drawn inside the Battery-number component, shares the number's exact matrix/path, then fades only during late handoff (58% -> 88%) rather than fading in place.
- Build-528 quick latent reveal and Build-526 Battery-Island authority remain unchanged.


Build 531 exact native typography handoff:
- Build-530 device video proves geometry-only optical mapping plus `Typeface.weight` is still insufficient:
  - late-transition Mobile Type remains visibly thinner than native `5G` and is slightly undersized;
  - battery percentage weight differs substantially from the native hollow-battery number.
- Root causes:
  - Mobile Type still used `SHRINK_ONLY` similarity geometry, so an endpoint requiring enlargement/aspect correction could not reach the exact native basis;
  - `Typeface.weight` does not encode the complete native text Paint contract (actual Typeface instance/family, fake-bold, textScaleX, skew, letter spacing, stroke/style);
  - similarity interpolation preserves the source aspect envelope, so even remeasured current glyph bounds cannot guarantee exact target width/height at p=1.
- Build 531 keeps compact/steady 5G and battery-number appearance unchanged and upgrades only transition rendering:
  - target witnesses snapshot the real native TextView/custom Battery Paint typography;
  - late transition converges to native Typeface + Paint flags, reaching the exact target style by p=0.88;
  - Mobile Type no longer has a shrink-only ceiling;
  - Mobile Type and Battery Number use exact basis interpolation so p=1 geometry equals the native target basis, not a source-aspect similarity approximation;
  - dynamic source ink bounds are still remeasured each frame under the effective transition typography.
- Witness diagnostics now include weight, fakeBold, textScaleX, strokeWidth and Paint style for device verification.
- Build-530 lightning-follow-number ownership and Build-528 latent reveal remain unchanged.


Build 532 battery-top offset/root-gap correction:
- maintainer device evidence establishes that the previous physical +3 position is the desired user-facing vertical-offset zero; the UI range is now deliberately limited to -10..+10 around that reference.
- root cause of the old "above +3 does not move" defect is confirmed in `CombinedStatusBatteryTopLayoutPolicy.resolveCenterY()`: after the automatic base position had already been safety-bounded, the manual positive offset was clamped a second time by `maximumSafeRise = baseCenterY - minimumSafeCenterY`; on the pinned Home RenderView this remaining value is only about 3-4 canonical units, so +10/+20/+30 all collapsed to the same rendered Y.
- Build 532 keeps the automatic/default placement safety bound but removes the second hidden manual clamp. User offset is literal within the visible -10..+10 range; the previous raw +3 maps to UI 0.
- ring avoidance is no longer a fixed top-center gap with width-only padding / 118-degree cap:
  - the current visible number ink width/height already reflects configured size and Typeface weight, including synthetic extra stroke above native weight;
  - the current charging drawable uses the cached native alpha-envelope optical width/height at its configured scale;
  - their combined optical group bounds include the current vertical offset;
  - `CombinedStatusBatteryTopArcPolicy.resolveGap()` intersects that live envelope (plus half ring stroke and 2 canonical px visual clearance) with the actual ring geometry and derives the left/right gap shoulders.
- therefore larger/bolder digits or a larger charging bolt widen the opening only as needed, moving the readout upward shrinks the opening, and asymmetric visible width can shift the gap center rather than adding dead symmetric padding.
- no native layout writer, timer, animator, polling path, or extra visual probe is added; existing cached glyph/drawable optical measurements are reused.

Build 533 companion-UI spacing correction:
- exact MIUIX revision review shows `BasicComponent` already inserts 8.dp between `startAction` and center content;
- the diagnostics leading-icon wrapper was adding a second 16.dp end padding on top of that MIUIX spacing;
- remove only the project-local 16.dp padding and retain the MIUIX-owned 8.dp spacing for diagnostics level / export / share;
- SystemUI/runtime rendering is unchanged by this checkpoint.

Build 534 battery-top physical-headroom remap:
- Build 532 correctly removed the hidden early ceiling, but a literal positive raw offset can still ask the Home overlay to draw above its actual View boundary and Android clips that ink;
- exact history review confirms Build 518's useful principle was to map the positive UI range across owned physical headroom, while Build 525 later corrected the physical-top coordinate into real RenderView space;
- preserve Build-532 user-facing zero: raw +3 / UI 0 remains the neutral anchor whenever physically safe;
- keep UI -10..0 as literal downward movement;
- remap only UI 0..+10 monotonically across the remaining safe distance between the accepted neutral center and the real RenderView top, using the current percentage ink height;
- if typography already consumes all headroom, clamp only at the real physical boundary rather than drawing clipped pixels;
- do not move the Home visual back to the outer status host, disable SystemUI clipping, or add a second motion writer; Build-424 native lifecycle ownership remains protected.

Build 535 corrected safe-range ownership:
- Build 534 device evidence shows UI 0 could already consume the complete positive range because the automatic optical base was safety-clamped before the manual range was mapped;
- the optical base now expresses design placement only; it no longer consumes physical headroom before user offset resolution;
- UI 0 keeps the accepted raw +3 reference whenever safe, and UI 0..+10 maps continuously onto the actual remaining physical top headroom;
- top safety uses the complete visible group rather than percentage text alone;
- the charging glyph contributes its real native optical top extent after alpha-centroid alignment, so 200% charging size reduces available rise or moves the group down instead of clipping;
- UI -10..0 remains literal downward travel;
- no carrier/overlay lifecycle change, parent clip mutation, second renderer, timer or native geometry writer is introduced.

Build 536 logical-viewport / physical-overflow split:
- Build 535 exact-head Runtime CI #2021 and signed Work Branch Canary #597 are green, but device video rejects its top-safe clamp: changing the upward slider value does not change visual Y once UI 0 reaches the safe ceiling.
- Root cause: logical Home slot geometry (about 105x108 on the verified device) was incorrectly treated as the complete physical draw surface; any policy that forbids negative logical Y necessarily consumes the user's upward range.
- Home keeps the same logical end slot and native status-bar height. No HyperOS peer geometry, parent padding, parent alpha/visibility, island translation, or status-bar height is changed.
- The Home renderer becomes one module-owned direct child of `MiuiStatusBatteryContainer` with zero LayoutParams participation in native measurement; exact target inspection shows the container measures/layouts only its three owned native fields.
- After native layout, Guiyuan alone measures/layouts that module child with a dynamic transparent top overflow derived from the current percentage + native charging-glyph optical bounds.
- The logical viewport is translated inside the larger physical surface by exactly the added top overflow, so logical y=0 retains the same screen coordinate and the ring/center/mobile steady geometry does not move.
- Manual top-readout Y is literal again; no positive safety clamp is allowed to flatten user input.
- The transition witness carries explicit logical left/top/width/height and freezes only that logical viewport. The extra transparent physical pixels are excluded from Home -> Control Center component geometry.
- This split intentionally leaves vertical draw overflow independent from future overall-size support. Later size changes may update visual scale and horizontal reservation without using the expanded physical canvas as slot geometry.
- Battery-number size and charging-lightning size user ranges are narrowed from 0%-200% to 40%-160%, with 100% unchanged and the existing 5% slider granularity preserved.


Build 537 visual-control / layout integration:
- the old battery-top master switch is narrowed to **battery number only**; charging glyph visibility is independent and defaults on;
- battery number and charging glyph each own an independent “follow battery-ring color” preference, default on; when disabled they use the current native status-icon foreground tint;
- user-facing “center icon” wording is replaced by **network status**;
- content layout is a persisted two-state semantic policy: **Network centered** (existing layout) or **Battery centered** (network moves to the top slot while battery number + charging glyph move to the center);
- transition targets do not swap: network components still target native network slots and battery information still targets native Battery Number; only source bounds change;
- top-ring avoidance is derived from whichever semantic currently occupies the top slot;
- Home keeps the Build-536 device-accepted direct-child logical viewport / physical-overflow split;
- the opt-in Keyguard renderer reuses the same module-owned vertical-overflow policy so top content is not clipped, while keeping separate Keyguard session/tint/AOD ownership;
- Home preview and Preview Sandbox now consume the real persisted VisualSettings and native SystemUI charging-resource families instead of silently rendering defaults;
- visual-settings persistence / remote mirroring / SystemUI runtime decoding now share one read/write contract, removing the three-copy key list that caused new settings to be omitted from runtime transport;
- Features uses one MIUIX page with three Cards: Global / Network / Battery; no custom nested page or hand-built pseudo-MIUIX control is introduced.

Build 538 independent layout profiles + live TopSlot avoidance:
- **Network centered** and **Battery centered** now own independent local visual profiles. Layout selection itself remains global; local network/battery visual controls are persisted under the active layout namespace.
- Existing Build-537 flat values are fallback only for a profile key that has never been written, so explicit old user values are preserved without continuing to couple the two layouts.
- Battery-centered profile defaults percentage size and charging-glyph size to **120%**; Network-centered keeps **100%**. MIUIX slider key points read these same layout-default functions, so the marked default and actual default cannot drift.
- Ring avoidance uses the same rectangle-to-arc gap solver for battery information and network TopSlot content. Its input is now the current visible optical envelope, not a remembered or maximum network template.
- Native network drawing and avoidance share one resolved draw geometry. A connected/reference resource may define the visual fit scale, but the **current drawable's** optical rect defines the avoidance envelope.
- Wi-Fi fallback uses the combined real Path bounds; Mobile Type uses current measured text bounds; airplane/no-SIM use current native drawable optical bounds.
- During the existing 100 ms center transition, the ring gap unions the previous/current bounds after the same enter/exit scale. Once the old state reaches zero appearance it contributes zero gap, so 5G -> Wi-Fi settles to Wi-Fi-sized avoidance.
- Physical top overflow is capacity, not animation geometry: it reserves both transition endpoints at full size so later transition frames cannot clip while logical slot geometry remains unchanged.
- Control Center semantic targets, HyperOS transition progress, Home/Keyguard logical viewport, native peer reservation and motion ownership are unchanged.
Build 539 island-collision reservation isolation:
- device log/video evidence shows `nativeHide=false` while native `mEndSideContent` is already `alpha=0 / visibility=INVISIBLE` during island-state pull-down;
- the same frames report `addBatteryIsland=false / batteryWidthDiff=0`, so this is not Battery-Island ownership and must not reopen Build-526's generic-island proxy rejection;
- Build 539 separated logical reservation from native padding exposure and blocked expanded native padding while a generic Home island was showing;
- maintainer device evidence then showed the trade-off directly: premature island avoidance appears suppressed, but surrounding native peers no longer move left as the Guiyuan decomposition opens;
- therefore Build 539 is retained only as diagnostic evidence, not the final correction.

Build 540 QS_FAKE island-contract probe:
- maintainer analysis identifies the more specific native-assumption mismatch: steady Home island avoidance may legitimately classify icons from horizontal occupancy because stock QS_FAKE begins from the same fixed icon membership as steady Home; Guiyuan uniquely decomposes one compact semantic into additional transition pixels while the fake row is already moving vertically away;
- Build 540 changes no functional path from 539 and adds one bounded, read-only Detailed-diagnostics snapshot when a QS_FAKE root attaches;
- the cold-start Build-540 report successfully captured the contract: QS_FAKE `MiuiStatusIconContainer` exposes `getIslandMonitor/getIslandShowing/getIslandTranslationX/setIslandController`; mobile animator children expose `getHideByIsland/setHideByIsland` and `getIslandState/setIslandState`; the container's existing per-child transition state also exposes `forceAppear`;
- the same report shows steady Home starts with `paddingEnd=0`, while island-state Control Center transition diagnostics keep growing logical `reservation` but report `nativeReservation=-1 / reservationMode=internal-progress-island-guard`; this confirms that the missing leftward peer reflow is the retained Build-539 guard, not a stale padding value surviving SystemUI restart;
- the exact-target SystemUI reference further establishes that `FakeContainerIslandMonitor` consumes Home `statusContainerSpace`, updates the fake `MiuiStatusIconContainer.islandWidth`, marks island-width state changed, and requests layout;
- therefore the final correction must leave Home island ownership intact, restore Guiyuan's native peer reservation, and isolate only the QS_FAKE transition peers from the semantically invalid island-hide consequence created by Guiyuan-only decomposition.
Build 541 QS_FAKE peer-freeze correction — **device rejected**:
- Build 541 successfully reacquired progress-synchronous native reservation and its runtime diagnostics prove the `forceAppear` contract was acquired: the affected pull reports `reservationMode=native-progress-fake-island-freeze`, `islandPeerFreeze=active:3`, and `nativeReservation` growing with transition progress;
- despite that, the recording still shows native peers disappearing during the generic-island pull, so per-child `forceAppear` is not the final island-layout authority for this path;
- this disproves the Build-541 seam rather than the broader native-assumption model. The failure occurs above individual child visibility state, inside QS_FAKE island-layout accounting.

Build 542 QS_FAKE island-boundary projection:
- exact-target JADX closes the collision formula: `MiuiStatusIconContainer.onLayout()` starts peer placement from `width - paddingEnd`, then compares each native peer's `layoutTranslationX` against private `getIslandTranslationX()`; once one peer falls below that boundary, that peer and the remaining left-side peers are assigned `visibleState=2 / inIslandState=10`;
- exact-target JADX also confirms `FakeContainerIslandMonitor` alone consumes Home `statusContainerSpace` and owns the native island-width state. Guiyuan must not become a second writer of that field;
- Guiyuan's semantic decomposition increases `paddingEnd` by `Δ`, shifting every fake peer's layout X left by the same `Δ` while stock HyperOS would keep the island boundary unchanged. That is the direct cause of the sequential disappearance reproduced on 541;
- Build 542 preserves the native classification invariant by projecting only the fake collision read: for the active QS_FAKE container, `getIslandTranslationX()` returns `max(0, W - Δ)` when native `W > 0`; other containers and non-island values return the native result unchanged;
- mathematically, `x - Δ >= W - Δ` is equivalent to `x >= W`, so peers visible at the steady start remain visible while peers already excluded by the native island remain excluded;
- exact JADX confirms the getter itself is only a native monitor read: it returns `-1` when no usable monitor exists and otherwise returns `_islandMonitor.getIslandWidth()`; no separate animation endpoint is mixed into this value;
- exact layout order starts peer placement from `width - paddingEnd`, evaluates the island threshold before the later RTL mirror, so the same `W-Δ` invariant applies to the pinned target's logical pre-mirror layout coordinates;
- Build 542 registers the new projection delta **before** changing `statusIcons.paddingEnd`, so the layout requested by the padding write cannot observe new peer X with an old island boundary; if the reservation write fails, the projection is restored to the previous delta (or cleared when none existed);
- `statusIcons.paddingEnd` remains the sole Guiyuan native peer-layout writer. HyperOS remains the sole writer of `islandWidth`, `islandWidthChanged`, controller state, child `visibleState`, View translation/alpha/visibility, and final Control Center geometry;
- the projection is stored in a weak map keyed by the actual active QS_FAKE `MiuiStatusIconContainer`; the exact getter Hook returns the untouched native value for Home, Keyguard, final Control Center, and every unregistered container;
- if the projection Hook is unavailable, Home + generic-island semantic reservation stays guarded and the runtime fails native rather than exposing expanded padding with an uncompensated island boundary;
- provisional Build-542 direct `islandWidth` writes and monitor-detach/bypass variants were reviewed and rejected before device packaging because they violate native state ownership or alter steady-island membership too broadly.

## Validation state

Confirmed:
- Build 537 exact-head Runtime CI #2025 and Work Branch Canary #602 are green at `5c7560769e2ff0926fba6a78eba022151aee1dd0`; Build 538 is a follow-up driven by maintainer feedback, not a rollback of the 536 logical-viewport/overflow contract.
- Build 536 exact-head Runtime CI #2022 and signed Work Branch Canary #599 are green; maintainer device validation accepted the visible upward movement, overflow surface, and 40%-160% sizing.
- Build 523 focused device evidence reproduced the battery-top vertical ceiling and charging bolt/number Y mismatch.
- Build 523 device evidence isolates the native-peer endpoint regression to the island + charging combination; island-only behavior is normal.
- The affected diagnostic showed `addBatteryIsland=false / batteryWidthDiff=0` while the old policy had already switched to `native-peer-motion`.
- Build 524 Runtime CI #1961: green.
- Build 525 Runtime CI #1966: green.
- Build 526 exact-head Runtime CI #1975: green; signed Work Branch Canary #583: green at exact SHA `9c833f79fff222b8551485349a0361812f6ba897`.
- Build 531 exact-head Runtime CI #2003: green; signed Work Branch Canary #590: green at exact SHA `08f3a7453ad2f626ce0f1d7fbe1a481c2a1306d4`.
- Build 526 static review:
  - exact HyperOS `isAddBatteryIsland` is read from the already-resolved `ControlCenterHeaderExpandController` contract;
  - no new hook count, listener, polling path, timer, animator, native translation writer, or layout writer is added;
  - `statusIcons.paddingEnd` remains the sole Guiyuan peer-layout writer;
  - the generic island callback remains available only for its existing island-owner diagnostics/motion evidence and no longer decides Battery-Island reservation authority;
  - expansion samples clear a stale prior Battery-Island value if the exact native read becomes unavailable;
  - unknown exact authority remains native-peer-motion rather than guessing `false`.

Pending:
- run final exact-head Build-542 CI after this documentation closure; Runtime CI #2049 is green at `36ba3c2e0a7dba9f8e253c2649523ea0b94a87e4`.
- if green, freeze the exact SHA and produce one signed Work Branch Canary because the fake island-collision projection requires device evidence.
- focused Build-542 device gate: with a generic Super-Island present, pull Home -> Control Center normally and quickly; surrounding native peers must continue their leftward reflow while the extra sequential disappearance reproduced on 541 is absent.
- export Detailed diagnostics and confirm `reservationMode=native-progress-fake-island-projected`, non-negative `nativeReservation`, and `islandBoundaryProjection=active:delta=<Δ>`. Any projection-unavailable path, continued premature disappearance, an icon hidden at steady start unexpectedly appearing, overlap/jump, stale spacing after collapse, or non-island regression rejects the checkpoint.
- the broader PR acceptance matrix remains:
  - configure noticeably different number size, number weight, vertical offset, charging-glyph size and color-link switches in each layout; switching layouts must restore each profile independently;
  - an unmodified Battery-centered profile must show 120% percentage size and 120% charging-glyph size, with the slider key point at 120%; Network-centered remains 100%;
  - explicit 537 persisted size values must be preserved as first-use fallback instead of being overwritten by the new default;
  - with network in the TopSlot, compare 5G/5G-A, Wi-Fi, no-network Wi-Fi, airplane and no-SIM: the opening must resize to the current visible optical envelope rather than a previous/max template;
  - during 5G <-> Wi-Fi changes, the opening may follow the existing 100 ms cross-fade but must settle to the new state with no stale excess gap;
  - battery-information TopSlot avoidance must retain Build-536/537 behavior for size, weight, lightning and vertical offset;
  - Home and enabled Keyguard remain unclipped; Home/Keyguard -> Control Center semantic targets remain unchanged;
  - Home preview and Preview Sandbox must display the active profile immediately.
  - Preview surfaces must remain visually centered: the existing 32dp top overflow allowance is mirrored with equal bottom space, while the logical preview viewport size remains unchanged.
## Runtime / rendering contract

- HyperOS remains authoritative for battery state, charging-glyph resource selection, Control Center expansion/motion, and Battery-Island activation.
- `ControlCenterHeaderExpandController.isAddBatteryIsland` is the only Battery-Island reservation-authority signal; generic Super-Island visibility is not equivalent.
- Guiyuan only observes native state and native resources; it does not write HyperOS island translations or `batteryWidthDiff`.
- The existing Guiyuan painter remains the only writer of Guiyuan pixels.
- The existing Battery transition component remains the only Guiyuan owner of battery-component transition rendering.
- `statusIcons.paddingEnd` remains the sole Guiyuan native peer-layout writer.
- Native drawable visual geometry is obtained from one bounded cached probe; envelope geometry and alpha-weighted ink center are read-only measurements from the same probe.
- No polling, delayed state inference, new frame hook, duplicate charge-speed observer, or second gesture animator is introduced.
- Battery number remains default-off; the charging glyph is an independent default-on visual setting. Existing feature/scene gates still fail native outside supported surfaces.

## Non-negotiable boundaries

- Root-cause first; no screenshot-fitted timing, fixed resource Y offsets, or Battery-Island X compensation.
- Preserve accepted Build-510/511 transition behavior unless contradictory device evidence appears.
- One mutable runtime property has one writer.
- Cleanup / Hot Reload restores only Guiyuan-owned state.
- Compatibility uncertainty must not impersonate Battery-Island state.
- HyperOS resources / state / motion are preferred over project-local copies or guesses.
- Do not revive the previously rejected local `batteryWidthDiff` normalization route without new exact-frame evidence that Guiyuan diverges from native QS_FAKE peers.

## Immediate next step

1. finish the CURRENT/DEVLOG closure for the getter-projection implementation and run one final exact-head Build-542 Runtime CI;
2. if green, freeze that exact SHA and produce one signed Work Branch Canary;
3. validate the generic-island Home -> Control Center path: native peer leftward reflow must remain, the extra sequential disappearance seen on 541 must be absent, and peers already hidden by the steady island must not be resurrected;
4. reject 542 on projection-unavailable, stale projection cleanup, overlap/jump, or non-island regression; do not merge PR #181 until this island path is device-accepted.
## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. task-specific architecture / reference docs;
4. current source and exact-target SystemUI evidence;
5. relevant `DEVLOG.md` history when needed.
