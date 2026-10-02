# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.5.
- `main` and `dev` are synchronized on the promoted 0.0.5 / Build 618 baseline.
- Stable version identity: `0.0.5` / versionCode `261002418` / Build `20261002-618`.
- Build 617 is the latest accepted runtime-affecting baseline; Build 618 changed only version/public-documentation identity.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- GPL-3.0-or-later remains the project license.

## Active objective

Branch: `feat/network-state-size-controls` / PR #195.

Build 619 adds two independent network-state visual controls:
- Airplane mode size: 40%-125%, default 100%.
- No-SIM size: 40%-125%, default 100%.

Both controls are profile-scoped exactly like Wi-Fi/mobile-type sizing, so Network centered and Battery centered remember separate values. Wi-Fi sizing remains independent and mobile-type sizing remains unchanged.

Renderer ownership stays narrow:
- HyperOS/native resources remain the drawable source.
- The new settings change only each resource's resolved draw-size constraint and matching optical avoidance/transition source geometry.
- No state source, slot ownership, native peer layout, gesture timeline, timing, alpha, or visibility writer changes.

## Validation state

- Build identity on this branch: `0.0.5` / versionCode `261002419` / Build `20261002-619`.
- Unit coverage includes settings bounds/profile participation and independent airplane/no-SIM renderer scaling.
- Automated Runtime validation is required before device testing.
- Focused device evidence is required because this changes steady/transition geometry for airplane and no-SIM states.

## Immediate next step

Complete code review, run exact-head Runtime CI, then request one signed Work Branch Canary for focused airplane/no-SIM validation. Freeze runtime after the Canary checkpoint until device evidence is returned.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.
