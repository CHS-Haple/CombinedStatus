# Combined Status Development Roadmap

This file stores future directions, deferred work, trigger conditions, prerequisites, and design seams that should be preserved for later work. It is not a promise that every item will be implemented.

Current behavior belongs in `CURRENT.md`; investigation/build history belongs in `DEVLOG.md`; stable user-facing net changes belong in `CHANGELOG.md`.

## Roadmap rules

- Revalidate every roadmap item against the latest source, SystemUI behavior, dependency state, and device evidence before implementation.
- Do not turn an old plan into code merely because it was once discussed.
- Record the trigger or prerequisite for deferred work whenever known.
- Preserve future-compatible seams when doing so is cheap and does not add speculative runtime machinery.
- Prefer native HyperOS/SystemUI behavior and resources where a verified contract exists.
- Delete or revise roadmap items when evidence makes them obsolete; record a meaningful reversal in `DEVLOG.md`.

## Active near-term route

### Native slot + Combined Status transition-geometry adapter

Exact SystemUI source and Build 384 runtime evidence confirm why the previous 0px/105px fixes formed a cycle:

- the native battery slot must remain the single 105px end-side layout occupancy owner;
- a full-width custom status-icon participant therefore duplicates occupancy;
- a zero-width custom participant avoids duplicate occupancy, but HyperOS APPEAR normally computes its pivot from that zero View width.

Build 385 tests the smallest source-level separation: retain the zero-width layout shell and native battery slot, but replace only the native APPEAR pivot initialization for the module-owned Combined Status root with the renderer's actual visual center.

**Acceptance boundary:**
- clean centered OFF -> ON APPEAR;
- clean centered ON -> OFF DISAPPEAR;
- correct steady placement;
- no first/last-frame transition shift;
- native Control Center anchor remains 478+105;
- no repeated/per-frame project writer;
- exact callback incompatibility fails native.

If Build 385 passes, retire temporary Build 383/384 probes that no longer provide ongoing compatibility value.

If Build 385 fails, reopen native end-side ownership. Do not add timing retries, repeated pivot writes, translation offsets, or duplicate layout occupancy.

## Known future design themes to revalidate

### Adaptive sizing and spacing

Longer-term presentation may allow user-adjustable Combined Status visual size with spacing derived from resolved visual geometry.

**Design seam to preserve:** native slot occupancy, renderer visual width, transition pivot, and optical spacing remain independently resolved. Build 385 derives APPEAR pivot from the resolved renderer width rather than a fixed 105px constant.

**Prerequisite:** the active transition checkpoint must pass before exposing user scaling.

### Dual-SIM behavior

Future mobile presentation may need explicit dual-SIM semantics beyond the currently validated selected/effective data source behavior.

**Design seam to preserve:** mobile state acquisition and presentation policy should not hard-wire one transient View topology as the permanent domain model.

### Island / SystemUI transition participation

Combined Status should continue to align with native SystemUI scene/transition behavior rather than implement a parallel animation authority.

**Design seam to preserve:** HyperOS owns scene, visibility, alpha/scale curve and panel/island transition state. Combined Status may adapt only transition geometry that is inherently different because drawing width is deliberately decoupled from layout width.

### Runtime ownership migration

If long-lived lifecycle responsibilities accumulate in the bootstrap, move bounded ownership into dedicated owner/session components.

### Native resource reuse

New HyperOS/SystemUI visual resources should continue through verified runtime identity and shared tint/intensity normalization.

## Deferred / rejected approaches

- **Native battery slot + full-width Combined Status participant:** rejected; duplicate steady occupancy caused left shift.
- **Hide native battery layout + full-width participant:** rejected; diagnostics showed invalid Control Center anchor semantics and non-steady shift.
- **Zero-width participant without transition-geometry adaptation:** rejected; native APPEAR derives pivot from shell width and writes `pivotX=0`.
- **Build 384 pre-draw / repeated / per-frame pivot rewrites:** rejected; runtime proved the native writer occurs later, and racing it violates ownership/lightweight rules.
- **`HomeStatusBarViewBinderInjector.mBatteryContainer` as renderer wrapper:** rejected; exact `battery_digital_view.xml` proves it is battery-internal icon content with battery-specific alpha behavior.
- **Magic translation/margin/padding/delay compensation:** rejected unless future evidence proves no direct ownership fix is viable.

## Update trigger

Update this file when:
- Build 385 device evidence accepts or invalidates the native APPEAR pivot adapter;
- temporary diagnostics can be retired;
- adaptive sizing gains a validated dynamic slot/transition contract;
- another ownership boundary changes.
