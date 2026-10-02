# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.5.
- `dev` is promotion-ready on the integrated 0.0.5 / Build 618 state.
- Version identity: `0.0.5` / versionCode `261002418` / Build `20261002-618`.
- Build 617 / `20261002-617` is the latest runtime-affecting checkpoint.
- Build 618 changes only version/public-documentation identity; runtime behavior remains Build 617.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- GPL-3.0-or-later remains the project license.

## Integrated Build 617 scope

- component-aware Wi-Fi ring avoidance, including off-center hotspot/no-internet badge geometry;
- overall-size range 60%-100% and Wi-Fi/mobile-type ranges 40%-125%;
- 5G/5GA follows user overall scale while host viewport scale remains compensated;
- SystemUI-main-thread visual-settings commits with stale slider snapshots coalesced;
- profile defaults: Network centered battery number 120%; Battery centered battery number 140% and mobile type 80%;
- profile-scoped Top information vertical offset in Global settings, targeting number+charging glyph for Network centered and network content for Battery centered.

## Validation evidence

Runtime/product line:
- PR #192 merged to `dev` as `fee0361e34b7e55722f4cbddf70415fe8966bca5`;
- exact-head Runtime CI #2215 succeeded for Build 617;
- signed Work Branch Canary #649 succeeded for Build 617, including trusted-source checkout, target profile, unit tests/build, Modern Xposed metadata, Haple signature and non-debuggable verification.

Version/promotion line:
- PR #193 advanced the development line to 0.0.5 / Build 618;
- exact-head Full CI #2217 succeeded on the 0.0.5 bump;
- trusted `dev` integration CI #2218 succeeded on `9e2611f9140f74046dd7ec409a4d74a87c151b35`, including Haple signing, signed APK verification, target profile, tests/build, Modern Xposed metadata and non-debuggable verification.

## Promotion objective

The exact validated `dev` state is ready for direct `dev -> main` promotion. The promotion must preserve history with a merge commit. If GitHub auto-deletes `dev` after the promotion, recreate `dev` immediately at the promoted `main` SHA and verify both branches are synchronized.

No additional device validation is required for Build 618 because its delta from accepted Build 617 is metadata/documentation-only.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.
