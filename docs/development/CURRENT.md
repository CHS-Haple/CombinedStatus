# Current Development State

## Active recovery line

- Branch: `fix/qs-fake-native-source-sync`.
- Base: Build 652 checkpoint `f8df74f`.
- Build 673 candidate restores HyperOS native QS_FAKE island authority and adds temporary read-only diagnostics.
- PR #197 / Builds 653-672 remain historical experimental evidence and are not the runtime base for this line.

## Ownership model under test

- HyperOS owns Home island avoidance, QS_FAKE source synchronization, island lifecycle, fake width and peer visibility decisions.
- Guiyuan only adapts represented Wi-Fi/mobile/Battery slots and its combined-slot reservation.
- No custom island boundary, 2D/optical collision, gesture latch or peer island state writer is used.
- Charging island (`mIsHideBattery=true`) remains a separate follow-up because native Battery removal changes the combined-slot reservation contract.

## Build 673 diagnostic gate

Temporary `nativeSourceSyncDiag` events are emitted only when observed state changes. They compare Home and QS_FAKE island state, Battery hide state, host/group widths, reservation/capacity values, and each non-represented peer's native visibility/island state. The diagnostic path is read-only and will be removed after localization.

## Immediate test

Test ordinary island only:
1. island already present before pull;
2. slow pull down and reverse;
3. if convenient, change island length/state while partially pulled;
4. observe whether Home and QS_FAKE peer changes stay synchronized;
5. export Detailed Diagnostic and a short recording.

Charging-island behavior is not an acceptance gate for Build 673.
