# Architecture document status

This directory contains architecture policy and scene/layout capability descriptions.

## Current status

The 0.0.2 development line has selected a pre-runtime Home/end-side carrier direction for the pinned target, and Build 394 is the first runtime proof of that direction. Older runtime architecture descriptions are retained as verified historical/current-code evidence but must not be mistaken for the selected 0.0.2 target architecture.

### Documents

- [layout-policy.md](layout-policy.md)
  - describes the shared geometry policy and the constraints around native geometry ownership;
  - its descriptions of the currently implemented PROJECTED path describe the existing/last-tested runtime model;
  - they do **not** require 0.0.2 to preserve the extra custom-participant carrier.

- [scene-policy.md](scene-policy.md)
  - describes the last verified scene capability boundaries;
  - Home capability evidence remains valid;
  - its existing PROJECTED/NATIVE_ONLY map is **not a commitment to the final 0.0.2 carrier/handoff architecture**.

- [../reference/README.md](../reference/README.md)
  - indexes generalized reusable implementation evidence;
  - reference evidence does not grant SystemUI write ownership.

## Superseded architecture route

The following route is preserved only as engineering evidence and must not be used as the default starting point for new 0.0.2 work:

`extra permanent status participant -> zero/full-width occupancy handoff -> compensate battery-slot release with custom slot/translation ownership`

Builds 386-393 demonstrated useful facts about native APPEAR, battery-slot release, peer occupancy, charging geometry, and panel anchors, but later evidence showed that the overall carrier model creates conflicting layout identities across scene transitions.

New 0.0.2 work must begin from the current decision in `docs/development/CURRENT.md`. The pre-runtime Home carrier contract is now closed for the pinned target; Build 394 must validate that contract at runtime before any historical mechanism is reconsidered.

## History policy

Do not rewrite historical DEVLOG entries to match this status. They record what was actually believed, implemented, and observed at the time. Supersession is expressed here and in current development-state documents.
