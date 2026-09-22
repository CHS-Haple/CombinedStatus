# CombinedStatus layout policy

## Purpose

CombinedStatus keeps visual geometry separate from native SystemUI layout and transition ownership.

This policy defines shared visual calculations for every scene so geometry rules do not drift into scene-specific hooks or compensation code.

## Current render modes

The runtime currently supports two layout modes:

### PROJECTED

CombinedStatus renders its visual against a verified native anchor while preserving the native SystemUI slot width.

In this mode:

- `visualSidePx` describes the CombinedStatus drawing size;
- `neighborGapPx` and `requestedSlotWidthPx` describe the desired visual/layout intent;
- `appliedSlotWidthPx` remains the native slot width supplied by SystemUI;
- the visual is end-anchored to the verified native anchor;
- the policy does not grant permission to rewrite native measurement, layout, translation, or visibility.

Home stable currently uses this mode.

### NATIVE_ONLY

CombinedStatus does not render on the surface. Native SystemUI content and motion remain authoritative.

Notification-shade transition, Control Center, keyguard, and AOD currently use this mode.

## Shared geometry

The shared policy owns only CombinedStatus-side calculations:

- canonical/base visual size;
- user visual scale;
- desired neighbor gap;
- requested visual slot width;
- end/right visual anchoring;
- vertically centered visual bounds.

Scene adapters must not duplicate these formulas.

The policy intentionally distinguishes:

1. native SystemUI slot geometry;
2. CombinedStatus visual geometry;
3. transition/motion geometry;
4. optical adjustment.

A value from one responsibility must not silently become the control value for another.

## Native slot preservation

`CombinedStatusLayoutPolicy.resolve()` currently preserves `host.nativeSlotWidthPx` as the applied slot width.

`requestedSlotWidthPx` is therefore not a production instruction to resize the native slot. It is a resolved CombinedStatus requirement that can be used for diagnostics, future capability evaluation, or a later explicitly owned layout contract.

Any future change that makes requested width affect native SystemUI geometry must first establish a new ownership contract and pass the validation requirements below.

## Motion ownership

Motion ownership is independent from layout size:

- `NONE` — no CombinedStatus-owned motion exists for the scene.
- `SYSTEM_UI` — SystemUI owns positioning/transition motion.
- `COMBINED_STATUS` — reserved for a future transition proven to be owned entirely by CombinedStatus.

A SystemUI-owned scene must not add independent translation formulas, width-difference corrections, or endpoint compensation.

## End-anchor invariant

Changing CombinedStatus visual scale should preserve the resolved end/right visual anchor.

Scaling affects the CombinedStatus drawing bounds. It must not be implemented by moving the final result with an unrelated `translationX` correction.

## Neighbor gap

Neighbor gap is part of the CombinedStatus visual/layout requirement, but the current PROJECTED integration does not claim native neighbor-layout ownership.

If a future native layout contract is established, the gap calculation must remain centralized here rather than being copied into individual scene adapters.

## Rejected geometry pattern

Runtime validation previously showed that mutating native battery-slot geometry can expand or move more of the SystemUI layout than the requested CombinedStatus visual size and can leak effects into other scenes.

That experiment was removed.

The project must not return to the pattern:

`custom native width -> scene-specific width difference -> translation/alignment compensation`

without new runtime evidence and an explicit ownership transfer.

## Requirements before any future native geometry ownership

Before CombinedStatus may write native slot geometry, contributors must verify:

1. the exact owning SystemUI host and lifecycle;
2. which component is the single writer for the affected property;
3. the stable end anchor;
4. adjacent-icon behavior when the slot changes;
5. notification-shade, keyguard, AOD, Control Center, and island-transition behavior;
6. restore/fallback behavior when CombinedStatus is unavailable or hidden;
7. cleanup across host replacement, SystemUI recreation, and hot reload;
8. that the change is safer than remaining PROJECTED.

Until those requirements are met, native SystemUI geometry remains authoritative.
