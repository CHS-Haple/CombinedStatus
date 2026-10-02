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

Build 625 is device-rejected: repeated Keyguard/AOD switching still exposes the native Wi-Fi/mobile/battery represented set for a short interval even though Keyguard and AOD already share one host-scoped presentation/render family.

Build 626 narrows the remaining root cause to callback ordering inside that shared family:
- `SystemUiKeyguardAodStateSource` stores the new HyperOS AOD state before invoking Guiyuan;
- Build 625 then delivered that state to the currently-labelled render scene first, allowing the outgoing Keyguard/AOD scene to publish `readiness=false` and restore native represented slots;
- only afterwards did `onKeyguardHostResolution()` retarget the same family session to the destination scene;
- Build 626 reverses only those two module-level steps: resolve/retarget the family owner first, then let the already-retargeted render session consume the same AOD update.

This preserves the Build-625 single-owner architecture and adds no timer, delay, polling, duplicate animator, native translation writer, or guessed AOD direction.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003626` / Build `20261003-626`.
- Build 625 device evidence rejects the prior ordering because native represented icons still flash during scene switching.
- The Build-625 diagnostic sequence and video agree on the failure shape: outgoing readiness cleanup precedes incoming AOD/Keyguard cutover.
- Build 626 changes no host topology, represented-slot set, compact-layout rule, AOD child semantics, or Control Center eligibility; it changes only family handoff ordering.
- Runtime CI #2289 already passed the exact source-ordering change before the Build-id-only bump.
- Current Build-626 Runtime CI is the final automated gate before a focused Canary/device check.

## Device gate

Validate Build 626 with emphasis on:
- repeated Home -> AOD -> Home switching: no temporary native represented icons, blank interval or duplicate set;
- repeated Keyguard -> AOD -> Keyguard with both child switches enabled: no temporary native represented icons, blank interval or duplicate set;
- AOD on / Keyguard off and Keyguard on / AOD off still preserve independent child behavior;
- global Guiyuan off still restores native presentation immediately;
- no stuck outgoing frame, stale AOD alpha on Keyguard, or AOD leakage into Control Center.

## Immediate next step

Finish Runtime CI and review. If clean, runtime is frozen and one signed Work Branch Canary is required because this fix targets a device-visible family handoff defect.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.
