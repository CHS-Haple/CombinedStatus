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

Build 663 is the focused recovery candidate after Build-662 device rejection.

Build-662 device result:
- no reported target is fixed;
- Keyguard ON / AOD OFF regresses AOD -> Keyguard: native represented icons become visibly separated / stationary before Guiyuan finally composes, while the original late response remains;
- frame evidence shows the raw native network/battery row is exposed during the transition before the compact Guiyuan owner appears;
- the diagnostic shows `animateIconContainer(true)` at the native visual boundary, followed about 2 ms later by a Keyguard end-reservation write, while the stable Keyguard family edge arrives about 0.38 s later. Build 662 therefore acquired native layout ownership inside the native animation window;
- Home -> AOD flash is unchanged, so the Build-662 Home-origin change has no accepted device value.

Build-663 correction:
- revert the unaccepted Build-662 synchronous boundary-layout takeover and Home-origin policy expansion back to the Build-660 rules;
- for only AOD -> Keyguard when Keyguard is enabled and AOD is disabled, split handoff into two phases under the same presentation owner:
  1. at native `animateIconContainer(true)`, attach the ready Keyguard renderer and clip-mask only the represented native views; do not write ignored slots, padding, or any native layout geometry;
  2. after native state reaches stable Keyguard, commit ignored-slot/end-reservation ownership while those native views remain masked, then complete compact layout normally;
- native layouts that occur during the visual-only phase may refresh the existing clip mask but cannot complete compact ownership or write reservation geometry;
- Keyguard OFF / AOD ON and dual-enabled family paths are not changed by this recovery.

No timer, delay, polling, copied animation timeline, native alpha/visibility/translation writer, or geometry compensation is introduced. The existing clipBounds presentation writer remains the only native visual mask writer.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003663` / Build `20261003-663`.
- PR #196 is 0 behind `dev` before Build-663 authoring.
- Unit coverage constrains the visual-only path to incoming enabled Keyguard from stable AOD and proves deferred visual handoff cannot write native layout or complete compact layout before explicit commit.
- Runtime CI is required before Canary.
- Device validation is mandatory.

## Device gate

This build is intentionally one-variable after the Build-662 regression.

1. Keyguard ON / AOD OFF — primary
   - AOD -> Keyguard must no longer expose the separated native mobile/battery row.
   - Guiyuan should become visible at the native status-icon boundary rather than only at the final stable edge.
   - No ignored-slot/padding movement is allowed during the visual-only interval.
   - Keyguard -> AOD remains at the Build-660 behavior.

2. Keyguard OFF / AOD ON — regression only
   - must remain identical to the accepted Build-660 timing.

3. Both ON / Home -> AOD
   - no new behavior is claimed in Build 663; the existing flash remains an open blocker for the next isolated change.

## Immediate next step

Review Build 663 against Build 660/662, run exact-HEAD Runtime, then issue one signed Canary only if automated validation is clean.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. `SystemUI-Reference` exact-target findings and task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.
