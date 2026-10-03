# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical chronology and rejected routes belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.5.
- `main`: promoted Build 618 checkpoint.
- `dev`: accepted Build 619 integration baseline.
- Active branch / PR: `feat/battery-fill-retract-follow` / #197; latest checked state is 0 behind `dev`.
- Verified target: Xiaomi 15 Pro / HyperOS SystemUI 17.03.260226.r / Android 17 / SDK 37 / Modern Xposed API 102.

## Build 670 device conclusion

Build 670 is rejected for two independent reasons.

1. **Island peer latch sampled too late.** During real overlap, fake `network_speed` / `vpn` can already be in native island-hidden state. By the exact 2D separation callback, HyperOS may have moved those states back to normal, so Build 670 records `islandPeerLatch snapshot=none` and the previously avoided peer becomes visible again.

2. **Mid-gesture capacity lease used the wrong capacity origin.** Charging-island sessions enter the fixed fake-carrier lease only after 2D separation, with a substantial reservation already present. Build 670 compared total reservation against the lease's newly added capacity. Near the capacity boundary it raised `fake-carrier-capacity-insufficient`, cleaned up the Guiyuan presentation, and native SystemUI visibly took over.

## Build 671 candidate

Build 671 preserves the accepted Build-669 2D monitor-width gate and Build-611/612 fixed capacity mechanism, but corrects their handoff semantics.

### Peer latch

- While real 2D overlap is active, after native `onLayout`, read only laid-out non-represented peers.
- Accumulate slot names that are currently or immediately previously in native island-hidden state.
- On overlap -> separated, freeze the accumulated slot set as the gesture latch.
- During the separated phase, rematch those slot names against current fake-row Views and maintain reversible empty `clipBounds`; this survives native child re-layout/rebinding without writing native visibility/state.
- Reverse real-overlap restores capacity first, waits for baseline native layout, then releases latch clips.

### Capacity lease

- No-island behavior keeps the accepted Build-611/612 origin of zero reservation.
- Island-native-layout sessions that activate the lease mid-gesture snapshot the reservation already present at activation.
- Capacity validation uses only `max(currentReservation - activationReservation, 0)`.
- The reservation curve itself is unchanged; charging-island `nativeHide` semantics remain untouched.
- Therefore pre-existing island/charging reservation does not consume newly leased capacity a second time.

## Ownership

No peer `visibleState`, `inIslandState`, alpha, View visibility or translation write is added. No island rectangle/width field is written. No timer, poller, custom threshold, new animator or alternate gesture timeline is added. The only peer presentation write remains slot-owned reversible `clipBounds`; width capacity remains the existing bounded fixed session lease.

## Validation state

Candidate identity: `0.0.5` / versionCode `261003671` / Build `20261003-671`.

Automated gate: exact-head Runtime CI.

Device gate after Runtime success:
1. ordinary island: a peer avoided once stays hidden for the rest of outward fake transition;
2. charging island + dual SIM: Guiyuan presentation must remain active through the whole pull; no return to native status bar;
3. peers not avoided by the island must not disappear near the endpoint;
4. reverse keeps the outward latch until real overlap returns, then hands back to native avoidance without a reveal flash;
5. no left jump when mid-gesture capacity lease activates;
6. ordinary no-island pull remains unchanged;
7. export one Detailed diagnostic.

## Immediate next step

Run exact-head Runtime CI for Build 671, then one signed exact-head Work Branch Canary and freeze runtime for focused device validation.
