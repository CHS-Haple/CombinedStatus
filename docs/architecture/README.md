# Architecture document status

This directory contains the current architecture policy and scene/layout capability boundaries for Combined Status.

## Current 0.0.2 status

The pinned-target Home carrier redesign has moved from pre-runtime evaluation into runtime validation.

- Builds 386-393 remain historical evidence for the superseded permanent extra-participant / occupancy-handoff route.
- Build 397 is the first device-accepted charging-carrier checkpoint on the new Home overlay architecture.
- Build 398 refines the stable width source to the live native `battery_icon_container`.
- Build 399 is a visual battery-ring compositing checkpoint and does not reopen carrier ownership.

Current Home direction:

`MiuiNotificationStatusContainer / system_icon_area -> HostSession overlay -> ResolvedLayout -> Combined Status renderer`

SystemUI retains native peer layout, Battery hide/presentation, tint authority, and live island motion. Combined Status owns only its compact composition plus the explicitly verified, reversible Home presentation state described in [layout-policy.md](layout-policy.md).

## Documents

- [layout-policy.md](layout-policy.md)
  - current shared geometry and Home carrier/reservation contract;
  - separation of visual geometry, native occupancy, motion and optical adjustment;
  - rejected geometry/writer patterns and future sizing boundary.

- [scene-policy.md](scene-policy.md)
  - current scene capability map;
  - Home is the only Combined Status rendering surface currently runtime-verified;
  - shade / Control Center, keyguard and AOD remain native-only until separately promoted.

- [../reference/README.md](../reference/README.md)
  - generalized reusable implementation evidence;
  - reference evidence never grants SystemUI write ownership by itself.

## Superseded architecture route

The default 0.0.2 architecture must not return to:

`extra permanent status participant -> zero/full-width occupancy handoff -> custom slot/translation compensation`

Those builds still provide useful evidence about native APPEAR behavior, battery-slot release, peer occupancy, charging geometry and panel anchors, but their carrier model created conflicting layout identities across scene transitions.

A superseded mechanism may be reconsidered only if new exact-target evidence invalidates the current route and a fresh ownership/lifecycle/single-writer review proves the alternative safer.

## History policy

Do not rewrite historical `DEVLOG.md` entries to match current conclusions. Preserve what was actually implemented and believed at the time, append later corrections, and keep current policy in this directory plus `docs/development/CURRENT.md`.
