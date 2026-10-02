# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version candidate: Guiyuan 0.0.5.
- `dev` contains the accepted Build 617 runtime/product line from PR #192.
- Version identity on this bump branch: `0.0.5` / versionCode `261002418` / Build `20261002-618`.
- Build 617 / `20261002-617` is the latest runtime-affecting checkpoint.
- Build 618 is version/public-documentation metadata only and introduces no runtime behavior change.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- GPL-3.0-or-later remains the project license.

## Build 617 accepted runtime scope

The integrated runtime line includes:
- component-aware Wi-Fi ring avoidance, including off-center hotspot/no-internet badge geometry;
- overall-size range 60%-100% and Wi-Fi/mobile-type ranges 40%-125%;
- 5G/5GA following user overall scale while host viewport scale remains compensated;
- SystemUI-main-thread visual-settings commits with stale slider snapshots coalesced;
- layout-profile defaults: Network centered battery number 120%; Battery centered battery number 140% and mobile type 80%;
- profile-scoped Top information vertical offset in the Global UI section, targeting number+charging glyph for Network centered and network content for Battery centered.

Validation evidence for the runtime head:
- PR #192 merged to `dev` as `fee0361e34b7e55722f4cbddf70415fe8966bca5`;
- exact-head Runtime CI #2215 succeeded for Build 617;
- signed Work Branch Canary #649 succeeded for Build 617, including trusted-source checkout, target profile, unit tests/build, Modern Xposed metadata, Haple signature and non-debuggable verification.

## Promotion objective

Current branch: `feat/version-0.0.5`.

Required next:
1. validate exact Build-618 version/documentation identity with Full CI;
2. merge the 0.0.5 bump into `dev`;
3. require trusted `dev` integration CI on the merged 0.0.5 state;
4. promote the exact validated `dev` state to `main`;
5. recreate/synchronize `dev` immediately if GitHub auto-deletes it after dev-to-main promotion.

No additional device validation is required for Build 618 because the delta is metadata/documentation-only relative to accepted Build 617 runtime behavior.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.
