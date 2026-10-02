# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.4.
- `main` and `dev` remain synchronized at the promoted Build 613 repository baseline before this work branch.
- Latest accepted Runtime/SystemUI behavior before this branch: Build 612.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- GPL-3.0-or-later remains the project license.

## Active objective

Branch: `fix/wifi-ring-shape-avoidance`.

Build 616 / `20261002-616` supersedes the branch's 614/615 checkpoints and carries all of their fixes:
- shape-aware Wi-Fi battery-ring avoidance;
- overall 60%-100%, Wi-Fi/mobile-type 40%-125% ranges;
- 5G/5GA following user overall scale while host viewport scale remains compensated;
- SystemUI-main-thread visual-settings commits with stale slider snapshots coalesced.

Build 616 adds profile-default and top-information semantics requested after Build 615.

### Profile defaults

For profiles with no persisted override (including after restoring defaults):
- Network centered: battery number default = 120%.
- Battery centered: battery number default = 140%; mobile-type default = 80%.
- Charging-glyph defaults are unchanged.
- Existing explicitly saved profile values are not overwritten.

### Top information vertical offset

The previous persisted `battery_top_vertical_offset` key is retained for compatibility and remains profile-scoped, but its runtime/UI meaning is now “Top information vertical offset”.

Target follows the active layout:
- Network centered: battery number + charging glyph.
- Battery centered: top network content (Wi-Fi / mobile type / airplane / no-SIM as applicable).

Implementation contract:
- user-facing zero and ±10 range remain unchanged;
- Network-centered readout keeps the accepted legacy raw-offset reference;
- Battery-centered readout remains at its own baseline and no longer consumes the user offset;
- Battery-centered network translation receives the user-facing offset instead;
- the same translated geometry feeds drawing, ring avoidance, required top overflow and transition source bounds;
- UI control lives in the Global section and is always available, independent of number/charging switches;
- the value remains independently remembered for Network centered and Battery centered profiles.

No migration rewrites existing profile values, no new preference key is introduced, and no timer/animation/native-writer ownership changes are introduced.

## Validation state

Pre-commit review:
- requested profile defaults are represented by layout-aware default helpers;
- battery-centered mobile-type default is used by data construction, persisted fallback and UI default key-point;
- top-information offset has one tested target policy: readout-only in Network centered, network-only in Battery centered;
- all network-top call sites share the same translated geometry, including transition bounds;
- old battery-section offset UI is removed and the global control no longer depends on readout/charging visibility;
- Build 615 scaling/main-thread fixes remain intact.

Required next:
1. exact-head Runtime CI for Build 616;
2. signed Work Branch Canary;
3. verify defaults after restore/switching profiles;
4. verify Top information vertical offset: Network centered moves number+glyph only; Battery centered moves network only;
5. repeat the Build-615 regression checks for scale dragging, 40% endpoints, Wi-Fi gap and numeric avoidance.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.
