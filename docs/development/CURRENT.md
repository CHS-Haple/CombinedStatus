# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.5.
- `main` remains on the promoted 0.0.5 / Build 618 stable checkpoint.
- `dev` is accepted through Build 619: `0.0.5` / versionCode `261002419` / Build `20261002-619`.
- Build 619 is the latest accepted runtime-affecting development baseline.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- GPL-3.0-only remains the project license.

## Active objective

Branch: `feat/aod-display-control` / PR #196.

Build 653 is the current Keyguard/AOD lifecycle correction candidate after Build-652 device evidence.

Build-652 device evidence:
- Keyguard + AOD ON: Keyguard -> Home and AOD -> Home followed by immediate fast Control Center pull can start and remain fully native; holding does not recover Guiyuan.
- Keyguard OFF / AOD ON: steady Keyguard correctly stays native, but AOD -> Keyguard releases Guiyuan slightly late; Keyguard/AOD -> Home first pull does not reproduce the dual-enabled failure.
- Keyguard ON / AOD OFF: Home -> AOD releases Guiyuan to native too late; Keyguard -> Home still reproduces the native first-pull failure, while AOD -> Home is normal.

The Build-652 diagnostic and video review narrow two independent lifecycle defects:
1. `MiuiBatteryMeterView.updateState()` is emitted by multiple Home/Keyguard Battery views. Build 652 let every structurally matching callback rewrite one global `steadyStatusSourceScene`, even when that source host was hidden. A hidden Keyguard source can therefore reassert KEYGUARD after visible Home already owns the screen and steer the immediate first pull back to native.
2. Build 652 fixed early single-child release by retaining the enabled outgoing child until `isAodAnimate=false`. That is too late. Exact-target AOD motion independently changes Keyguard host/status-icons/Battery visibility and alpha before the animation flag clears, so the native visual ownership boundary is observable directly.

Build 653 correction:
- keep structural scene evidence for host discovery, but only a currently shown native Home/Keyguard host may update steady source ownership or the stable-family scene latch;
- hidden structural Keyguard events may still refresh `SystemUiKeyguardHostResolver`; they cannot become scene authority;
- sample the verified native Keyguard status presentation read-only from host visibility plus status-icons/Battery `isShown` and alpha;
- Keyguard-only mode retains Guiyuan only while that native Keyguard presentation is visibly active; once it yields during AOD animation, the disabled AOD target returns to Native without waiting for animation-end;
- AOD-only mode returns to Native only when a visible, qualified KEYGUARD source and visible native Keyguard status presentation agree;
- dual-enabled Keyguard/AOD family retargeting remains unchanged;
- no timer, delay, polling, copied AOD motion, native alpha/visibility/geometry writer, or `animToAod` direction inference is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003653` / Build `20261003-653`.
- PR #196 remains 0 behind current `dev` at the Build-652 checkpoint.
- Focused unit coverage locks hidden-host rejection, surface matching, native Keyguard visual presence, single-enabled visual handoff and conservative fallback when visual evidence is unavailable.
- Exact-HEAD Runtime CI is required before Canary.
- Device evidence remains mandatory because the change affects scene ownership and native AOD visual cutover timing.

## Device gate

After exact-HEAD Runtime passes, validate one signed Build-653 Canary:

1. Keyguard + AOD both ON
   - repeat Keyguard -> Home -> immediate fast/partial Control Center pull;
   - repeat AOD -> Home -> immediate fast/partial Control Center pull;
   - first non-zero pull must be Guiyuan and must remain Guiyuan without holding/recovery.

2. Keyguard OFF / AOD ON
   - steady Keyguard remains native;
   - AOD -> Keyguard should release at the native Keyguard visual takeover, not at animation end;
   - AOD -> Home must remain regression-free with no native represented-icon flash before Home takes over.

3. Keyguard ON / AOD OFF
   - Home -> AOD and Keyguard -> AOD should release Guiyuan when the native Keyguard status presentation yields, not at animation end;
   - stable AOD remains native;
   - Keyguard -> Home immediate pull must remain Guiyuan.

4. Regression
   - dual-enabled Keyguard <-> AOD continuity;
   - ordinary Home and Keyguard pull-down;
   - Hot Reload first pull;
   - no hidden-host scene evidence may switch effective Control Center source;
   - no repeated presentation cleanup/reacquire loop while a valid visible source remains projected.

## Immediate next step

Post-review Build 653 is ready to fast-forward onto #196. Then run exact-HEAD Runtime CI and generate one signed Canary for the focused device gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. `SystemUI-Reference` exact-target findings and task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.
