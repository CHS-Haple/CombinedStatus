# Combined Status layout policy

## Purpose

Combined Status keeps visual geometry separate from native SystemUI layout and transition ownership.

This policy defines shared visual calculations for every scene so geometry rules do not drift into scene-specific hooks or compensation code.

## 0.0.2 architecture status

This document preserves verified layout-policy constraints and describes the last implemented PROJECTED path where relevant. That existing carrier is **not the selected default architecture for 0.0.2**.

Builds 386-393 remain evidence about target-SystemUI geometry and animation behavior, but their permanent extra-participant / occupancy-handoff route is superseded as the default starting point for new work.

For current implementation direction, read `docs/development/CURRENT.md`, `docs/development/ROADMAP.md`, `docs/architecture/README.md`, and the applicable `docs/reference/` evidence before using historical PROJECTED behavior as a design premise.

## Current render modes

The runtime currently supports two layout modes:

### PROJECTED

Combined Status renders its visual against a verified native anchor while preserving the native SystemUI slot width.

In this mode:

- `visualSidePx` describes the Combined Status drawing size;
- `neighborGapPx` and `requestedSlotWidthPx` describe the desired visual/layout intent;
- `appliedSlotWidthPx` remains the native slot width supplied by SystemUI;
- the visual is end-anchored to the verified native anchor;
- the policy does not grant permission to rewrite native measurement, layout, translation, or visibility.

Home stable currently uses this mode.

### NATIVE_ONLY

Combined Status does not render on the surface. Native SystemUI content and motion remain authoritative.

Notification-shade transition, Control Center, keyguard, and AOD currently use this mode.

## Shared geometry

The shared policy owns only Combined Status-side calculations:

- canonical/base visual size;
- user visual scale;
- desired neighbor gap;
- requested visual slot width;
- end/right visual anchoring;
- vertically centered visual bounds.

Scene adapters must not duplicate these formulas.

The policy intentionally distinguishes:

1. native SystemUI slot geometry;
2. Combined Status visual geometry;
3. transition/motion geometry;
4. optical adjustment.

A value from one responsibility must not silently become the control value for another.

## Native slot preservation

`CombinedStatusLayoutPolicy.resolve()` currently preserves `host.nativeSlotWidthPx` as the applied slot width.

`requestedSlotWidthPx` is therefore not a production instruction to resize the native slot. It is a resolved Combined Status requirement that can be used for diagnostics, future capability evaluation, or a later explicitly owned layout contract.

Any future change that makes requested width affect native SystemUI geometry must first establish a new ownership contract and pass the validation requirements below.

## Motion ownership

Motion ownership is independent from layout size:

- `NONE` — no Combined Status-owned motion exists for the scene.
- `SYSTEM_UI` — SystemUI owns positioning/transition motion.
- `COMBINED_STATUS` — reserved for a future transition proven to be owned entirely by Combined Status.

A SystemUI-owned scene must not add independent translation formulas, width-difference corrections, or endpoint compensation.

## End-anchor invariant

Changing Combined Status visual scale should preserve the resolved end/right visual anchor.

Scaling affects the Combined Status drawing bounds. It must not be implemented by moving the final result with an unrelated `translationX` correction.

## Neighbor gap

Neighbor gap is part of the Combined Status visual/layout requirement, but the current PROJECTED integration does not claim native neighbor-layout ownership.

If a future native layout contract is established, the gap calculation must remain centralized here rather than being copied into individual scene adapters.

## Rejected geometry pattern

Runtime validation previously showed that mutating native battery-slot geometry can expand or move more of the SystemUI layout than the requested Combined Status visual size and can leak effects into other scenes.

That experiment was removed.

The project must not return to the pattern:

`custom native width -> scene-specific width difference -> translation/alignment compensation`

without new runtime evidence and an explicit ownership transfer.

## Requirements before any future native geometry ownership

Before Combined Status may write native slot geometry, contributors must verify:

1. the exact owning SystemUI host and lifecycle;
2. which component is the single writer for the affected property;
3. the stable end anchor;
4. adjacent-icon behavior when the slot changes;
5. notification-shade, keyguard, AOD, Control Center, and island-transition behavior;
6. restore/fallback behavior when Combined Status is unavailable or hidden;
7. cleanup across host replacement, SystemUI recreation, and hot reload;
8. that the change is safer than remaining PROJECTED.

Until those requirements are met, native SystemUI geometry remains authoritative.


## Reference patterns under evaluation

The current production policy above describes verified project behavior; it is not a requirement to preserve the same carrier implementation in 0.0.2.

Generalized external implementation evidence that may inform the next target-specific architecture is stored in:
- `docs/reference/README.md`;
- `docs/reference/statusbar-composition-patterns.md`.

Those notes are evidence, not ownership permission. Any pattern adopted from them must still satisfy this document's native-geometry requirements, exact target verification, fail-native behavior, and device validation.
