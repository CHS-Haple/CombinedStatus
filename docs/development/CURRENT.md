# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical chronology and rejected routes belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.5.
- `main`: promoted Build 618 checkpoint.
- `dev`: accepted Build 619 integration baseline.
- Active branch / PR: `feat/battery-fill-retract-follow` / #197; latest checked state is 0 behind `dev`.
- Verified target: Xiaomi 15 Pro / HyperOS SystemUI 17.03.260226.r / Android 17 / SDK 37 / Modern Xposed API 102.

## Build 670 / 671 device conclusion

Build 670 exposed two independent defects:
- the gesture latch sampled native island state too late and could miss a peer already avoided earlier in the pull;
- charging-island capacity validation compared total reservation against only newly leased capacity, causing `fake-carrier-capacity-insufficient` and visible fallback to the native status bar.

Build 671 corrects those two mechanics and passes exact-head Runtime CI. It is superseded before Canary because new device video proves a stronger semantic defect: HyperOS' scalar island state can hide VPN even when the visible VPN glyph has not physically touched the island. A state-based latch would preserve that false positive.

## Build 672 candidate

Build 672 replaces QS_FAKE scalar island peer suppression with precise per-peer optical collision while retaining HyperOS motion/layout ownership everywhere else.

### Precise island avoidance

- The exact current `FakeContainerIslandMonitor` remains the island-presence seam.
- With a verified live island rectangle, its scalar width is exposed as 0 only for the current QS_FAKE Session.
- The fake row receives the already accepted bounded fixed capacity lease from Session start, so native overflow cannot hide unrelated peers.
- After each native fake-row layout, Guiyuan resolves each non-represented peer's visible optical content:
  - ImageView / StatusBarIconView: drawable bounds mapped through the native image matrix and padding;
  - TextView-like peers such as network speed: actual text-layout bounds;
  - nested visual peers: union of visible image/text descendants up to bounded depth.
- A peer is latched only when that optical rectangle actually intersects the live island rectangle.
- Missing optical geometry fails open for that peer (`keep-visible`) instead of over-hiding.
- Once truly collided, the peer stays presentation-clipped for the remainder of that fake Session, preserving the requested one-hide-per-gesture behavior.

### Capacity

- All QS_FAKE Sessions use the accepted fixed carrier-capacity lease.
- No-island activation origin remains 0.
- Island-native-layout activation snapshots the reservation already present at Session start; only later reservation growth consumes the lease delta.
- Charging-island `nativeHide`, reservation curve, motion projection and carrier anchor remain unchanged.

## Ownership

No native peer `visibleState`, `inIslandState`, alpha, View visibility or translation write is added. No island rectangle field or monitor field is written. The only peer presentation write is reversible `clipBounds`; the only layout-width write is the existing bounded fixed Session lease. No timer, poller, custom threshold or second animator is added.

## Validation state

Candidate identity: `0.0.5` / versionCode `261003672` / Build `20261003-672`.

Required automated gate: exact-head Runtime CI.

Required device gate after Runtime success:
1. ordinary island: network speed hides only when its visible content actually contacts the island;
2. VPN must remain visible while there is visible optical clearance, even if its outer View box overlaps;
3. once a peer truly collides, it remains hidden for the rest of the outward fake transition;
4. charging island + dual SIM: no `fake-carrier-capacity-insufficient`, no return to native status bar;
5. no initial/lease-activation left jump;
6. no-island pull remains unchanged;
7. export one Detailed diagnostic.

## Immediate next step

Run exact-head Runtime CI for Build 672, then one signed exact-head Work Branch Canary and freeze runtime for focused device validation.
