# Guiyuan layout policy

## Purpose

Guiyuan keeps four responsibilities separate:

1. native SystemUI layout/occupancy;
2. Guiyuan visual geometry;
3. transition/motion geometry;
4. optical adjustment.

A value from one responsibility must not silently become the control value for another.

## Current 0.0.2 Home contract

The current work-branch Home path uses the existing native Home host rather than a permanent extra status participant:

`MiuiNotificationStatusContainer / system_icon_area (HostSession) -> MiuiStatusBatteryContainer / system_icons.overlay (visual carrier) -> CombinedStatusHomeLayoutResolver -> Guiyuan renderer`

Build 397 is the first device-accepted charging-carrier checkpoint for this route. Build 398 refines the carrier-width authority to the live `battery_icon_container`; Build 399 changes only battery-ring compositing and does not alter this layout contract.

### Ownership

- HyperOS owns native Battery composition, Battery hide state, peer layout behavior, tint/scene facts and island/Folme motion.
- Guiyuan owns its overlay drawing, resolved replacement-slot intent, temporary represented-slot exclusions, reversible visual masks and one conflict-detected status-icon end reservation.
- Native alpha, visibility, translation and Battery measured/layout width are not Guiyuan write properties.

## Render modes

### PROJECTED

Guiyuan renders against verified native host geometry while SystemUI remains authoritative for surrounding layout and motion.

Home currently uses this mode. Build 424 places the visual inside the native `system_icons` carrier while keeping the outer `system_icon_area` as the HostSession/ancestor-motion boundary.

### NATIVE_ONLY

Guiyuan does not render on the surface. Native SystemUI content and motion remain authoritative.

Notification Shade, keyguard and AOD currently use this mode. Control Center has a separately verified Build-420 projection path in the active Phase-2B branch; its longer-term transition carrier is under exact-target review and is not promoted here as a new settled layout contract.

## Shared `ResolvedLayout` contract

### Guiyuan inputs

The shared resolver may consume presentation intent only:

- canonical/base visual size;
- user visual scale;
- desired neighbor/leading optical gap;
- relative per-glyph scale;
- bounded optical adjustment inside the Guiyuan presentation space.

These inputs are independent. Visual scale is not automatically a native slot-width write, and optical adjustment is not native translation.

### Host/scene inputs

A host adapter supplies verified environment facts only:

- host height;
- real end anchor;
- verified stable carrier/available width;
- scene render capability;
- motion owner;
- source geometry needed by a later projection layer.

A host adapter must not invent a scene-specific width difference, timing curve or translation compensation.

### Resolved outputs

The shared resolver keeps separate:

- whether Guiyuan may render;
- visual size/bounds;
- requested neighbor gap;
- requested replacement-slot width;
- host-applied/stable carrier width;
- visual-to-slot relationship;
- per-glyph scale;
- optical adjustment;
- stable source bounds for later projection;
- motion ownership.

Native transition progress, duration/interpolators and target-View translation remain outside the layout resolver.

### Current Home resolution

The active Home adapter resolves the live native `battery_icon_container` under the bound `MiuiBatteryMeterView` and uses its stable width as the carrier-width authority.

At the current default:
- `userScale = 1`;
- neighbor gap is zero;
- visual side is bounded by the smaller of carrier width and host height;
- applied slot width is the stable carrier width;
- the visual is anchored to the Home host end.

The full `MiuiBatteryMeterView` width is **not** the replacement visual-width authority because charging-only presentation can make the Battery root wider than its stable body.

## Native represented-slot exclusion

Represented Wi-Fi/mobile/airplane/no-SIM slots are excluded only while the exact target `MiuiStatusIconContainer.onMeasure/onLayout` call executes.

The active HostSession:
1. reads the existing `ignoredSlots` collection;
2. adds only missing Guiyuan-owned entries;
3. lets native measure/layout run;
4. removes exactly those owned entries in `finally`.

The project must not clear or replace the platform collection wholesale.

The Home HostSession lifetime is independent from transient Shade/Control Center overlay visibility. Once the Home owner is structurally valid, scene transitions must not repeatedly destroy/recreate that session merely to let another SystemUI surface render. The Home-only exclusions/masks/reservation may remain scoped to the Home host while target transition surfaces keep their own native presentation authority.

## Reversible visual masking

Native represented Views remain attached and state/tint/lifecycle capable.

For each masked View:
- snapshot its existing `clipBounds`;
- apply the module-owned empty clip;
- restore only when the live clip still matches the module-applied value.

Do not replace this with permanent `GONE`, alpha racing or translation writes merely to hide duplicate visuals.

## Home end-reservation contract

The overlay itself does not consume native layout space. The current target therefore uses one narrow, reversible `MiuiStatusIconContainer.paddingEnd` reservation so the replacement and native peers share one coherent end boundary.

Inputs:
- requested replacement-slot width from `ResolvedLayout`;
- actual native Battery-root presentation width;
- native `mIsHideBattery` as a read-only scene/layout fact.

The signed reservation is:

- native Battery present: `requestedSlotWidth - actualBatteryWidth`;
- native Battery released: `requestedSlotWidth`.

This intentionally permits a negative delta when charging-only Battery presentation is wider than the stable replacement carrier. The value is derived from live/native geometry; it is not a hard-coded charging offset.

The reservation:
- is scoped to one Home HostSession;
- snapshots the pre-session relative padding;
- reacts only to low-frequency Battery/carrier layout and native hide-state events;
- rejects unexpected competing padding writers;
- restores only the exact module-applied state;
- fails native when the carrier, width, hide-state or writer contract is unavailable.

## Motion ownership

Motion ownership is independent from layout size:

- `NONE` — no Guiyuan-owned motion is needed;
- `SYSTEM_UI` — SystemUI owns positioning/transition motion;
- `COMBINED_STATUS` — reserved for a future transition proven to be fully module-owned.

Home island motion is `SYSTEM_UI`: the visual overlay lives in native `system_icons` and therefore inherits that carrier's own alpha/visibility/translation while also remaining under the ancestor `system_icon_area` island transform. Guiyuan must not add a battery-translation follower, duplicate animator or custom timing curve.

Phase 2B transition rendering must not reopen Home carrier ownership. Exact-target review establishes a narrower transition-only exception: once HyperOS has updated the role-5 QS_FAKE and role-6 final-QS Views for the current frame, Guiyuan may read their full transforms into a window-root overlay **only for Guiyuan-owned Trinity correspondence**. Native status-icon peers, network speed, fake/final block-list membership, appearance, tint and final-only icon entry remain SystemUI-owned. The transition source geometry must come from the real role-5 native carrier/Battery transform, never from an overlay child's local coordinates.

## Future size / spacing

Future user scale or gap settings must change shared layout inputs only.

They must not introduce:
- scene-specific hooks;
- charging-specific constants;
- translation compensation;
- duplicate native slot writers.

If a requested visual/slot size exceeds what a verified host can safely support, the capability remains explicit and fails native rather than being hidden with a correction.

## Rejected geometry / ownership patterns

Do not return to these without new exact-target evidence and a fresh ownership review:

- permanent extra `combined_status` participant as the default carrier;
- zero/full-width participant occupancy handoff;
- overriding native Battery-hide requests to preserve module layout;
- Battery-descendant translation/visibility as the steady Home anchor;
- live charging-inflated Battery root width as replacement visual width;
- fixed 105/135 or 448/478 correction chains;
- per-frame/pre-draw translation or pivot races;
- peer translation/alpha/visibility compensation;
- generic native-peer or network-speed projection when the matching SystemUI fake/final surfaces already own their transition;
- project-owned tint interpolation or progress thresholds that replace `CcFakeStatusBarIcons` / `CcStatusBarIcons` / Header appearance ownership;
- Home/Keyguard `realSystemIcons` -> final-QS RectF interpolation as a replacement for the verified QS_FAKE(role 5) -> final-QS(role 6) transition;
- using an overlay child's `getLocationOnScreen()` as a substitute for the actual native transform chain.

Build-specific history and rejected experiments belong in `docs/development/DEVLOG.md`.

## Requirements for any new native geometry write

Before Guiyuan takes ownership of another native geometry property, verify:

1. the exact owning SystemUI host and lifecycle;
2. the current writer set and single-writer boundary;
3. stable and transition geometry separately;
4. adjacent-icon behavior;
5. relevant Home/shade/Control Center/keyguard/AOD/island paths;
6. restoration and failure behavior;
7. host replacement, SystemUI recreation and Hot Reload cleanup;
8. compatibility/fingerprint scope;
9. that the change is safer than keeping the property SystemUI-owned.

## Reference evidence

Generalized reusable evidence lives under `docs/reference/`. It may justify an investigation direction, but target-specific write ownership still requires exact SystemUI proof and device validation.
