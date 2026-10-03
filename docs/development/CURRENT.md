# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical chronology and rejected routes belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.5.
- `main`: promoted Build 618 checkpoint.
- `dev`: accepted Build 619 integration baseline.
- Active branch / PR: `feat/battery-fill-retract-follow` / #197; latest checked state is 0 behind `dev`.
- Verified target: Xiaomi 15 Pro / HyperOS SystemUI 17.03.260226.r / Android 17 / SDK 37 / Modern Xposed API 102.

## Build 669 device conclusion

Build 669 proves the monitor-side 2D gate works: QS_FAKE no longer needs to consume Home's scalar island width after the translated fake row has physically left the island.

The returned video/log also exposes a separate near-endpoint loss mechanism. On the reverse path, while the fake row is still active:
- around fraction 0.87 / 0.75 / 0.62, `network_speed` is native `visibleState=2` while `inIslandState=20`;
- around fraction 0.50 / 0.37 it becomes visible again while still `inIslandState=20`;
- near fraction 0.25 it is hidden again with `inIslandState=10`.

That late hide is the already-proven Build-609 QS_FAKE underflow, not island collision: transition reservation reduces fake-row usable width and native overflow removes peers before the native fake->final appearance handoff.

## Build 670 candidate

Build 670 composes two accepted mechanisms by real 2D phase:

**Real island overlap**
- fixed fake-carrier capacity remains disabled;
- HyperOS retains native island collision/knife-hide;
- Build 669 monitor-width behavior stays native while overlapping.

**Overlap -> 2D separated**
- snapshot only non-represented peers already in native island-hidden state;
- hold only those peers with reversible `clipBounds`;
- then enable the accepted Build-611/612 fixed fake-carrier capacity lease for the separated phase;
- peers that survived the island boundary keep enough native measurement capacity and cannot later disappear from reservation underflow.

**Reverse separated -> real overlap**
- keep the peer latch while restoring baseline fake-carrier width;
- wait for the baseline-width native layout;
- then restore the latch clips and return peer visibility entirely to HyperOS.

No native peer `visibleState`, `inIslandState`, alpha, View visibility or translation is written. No island rectangle/width field is written. No timer, polling path, second animator or custom gesture threshold is added. Native peer state is read only once at the 2D separation boundary through the existing transition-state accessor.

## Validation state

Candidate identity: `0.0.5` / versionCode `261003670` / Build `20261003-670`.

Automated gate: exact-head Runtime CI.

Device gate after Runtime success:
1. charging island + dual SIM; slow Home -> Control Center and reverse;
2. a peer avoided by the island may disappear once, but must not reappear mid-down and disappear again later;
3. VPN/headset/other peers still visible at 2D separation must remain visible through the rest of the fake transition;
4. reverse starts with the same latched peer hidden and releases it only after real overlap returns and baseline fake capacity is restored;
5. no left jump when separated-phase capacity activates;
6. ordinary no-island pull remains unchanged;
7. export one Detailed diagnostic.

## Immediate next step

Run exact-head Runtime CI for Build 670, then one signed exact-head Work Branch Canary and freeze runtime for focused device validation.
