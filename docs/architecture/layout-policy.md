# CombinedStatus layout policy

## Purpose

CombinedStatus must support one visual/layout rule across Home, keyguard, AOD, and transition surfaces without copying geometry formulas into each scene.

This document defines the architectural boundary before any production slot mutation is introduced.

## Shared policy

The shared policy owns:

- user visual scale;
- canonical/base visual size;
- size-to-neighbor-gap scaling;
- requested CombinedStatus slot width;
- end/right anchoring;
- centered vertical visual bounds.

A scene must not reimplement these calculations.

The current policy intentionally accepts base visual size and base neighbor gap as inputs. **No production default gap is locked yet.** That value will be calibrated only after a real owned-slot experiment proves how the adjacent native status icon reacts.

## Scene capabilities

A scene adapter supplies only host facts and capabilities.

### OWNED_SLOT

CombinedStatus is allowed to own a real layout slot. The applied slot width equals the globally requested slot width.

This is the eventual target for a verified Home-stable implementation.

### PROJECTED

CombinedStatus reuses the exact same global visual size, gap request, and end anchor, but the native slot width is preserved.

This is intended for surfaces where SystemUI owns geometry or transition motion and a visual projection is safer than mutating layout.

### NATIVE_ONLY

CombinedStatus does not render on that surface. The native slot and native motion remain authoritative.

Shade/Control Center handoff is a likely use case unless later runtime evidence proves a safer combined rendering path.

## Motion ownership

Motion ownership is explicit and separate from size/layout policy:

- `COMBINED_STATUS` — only for stable geometry that CombinedStatus actually owns.
- `SYSTEM_UI` — SystemUI is already animating/positioning the surface.

A scene whose motion owner is SystemUI must not add independent translation formulas, width-difference corrections, or endpoint compensation.

## Right/end-anchor invariant

Changing user scale expands or contracts the visual toward the leading/left side while keeping the end/right edge stable.

This is deliberate: a larger CombinedStatus visual should request more leading space and push neighboring icons through layout, rather than move the visual afterward with `translationX`.

## Neighbor gap

The gap is part of the requested CombinedStatus slot and sits on the leading side of the visual.

The initial pure policy scales this gap from one global base value together with user scale. If real-device validation later shows that a nonlinear or clamped gap feels better, that formula must still remain in this single policy rather than move into scene adapters.

## What build 83 does not do

Build 83 does not:

- resize the current Home overlay;
- hide native icons;
- mutate `MiuiBatteryMeterView` width;
- write any SystemUI translation;
- add keyguard/AOD/Control Center rendering;
- expose a user size setting.

It only establishes the reusable policy and tests its invariants before production integration.

## Required validation before runtime integration

Before an adapter may use `OWNED_SLOT`, verify:

1. the owning Host and its lifecycle;
2. the stable end anchor;
3. adjacent-icon behavior when slot width changes;
4. no duplicate ownership during shade/keyguard/AOD transitions;
5. restore behavior when CombinedStatus becomes hidden;
6. zero custom translation during SystemUI-owned motion.

Any scene-specific exception must be documented with SystemUI artifact/runtime evidence before code is changed.
