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

No near-term implementation route is asserted by the initialization change. The active development session MUST populate this section from verified current work rather than from stale conversation memory.

## Known future design themes to revalidate

The following themes have been discussed in Combined Status development and are retained only as **revalidation candidates**, not as approved implementation commitments:

### Adaptive sizing and spacing

Longer-term presentation may allow user-adjustable Combined Status visual size with spacing derived from the resolved visual geometry rather than a permanently fixed slot assumption.

**Design seam to preserve:** keep native SystemUI slot ownership separate from Combined Status drawing geometry so future visual scaling does not require arbitrary native-width mutation.

### Dual-SIM behavior

Future mobile presentation may need explicit dual-SIM semantics beyond the currently validated single/selected-source behavior.

**Design seam to preserve:** mobile state acquisition and presentation policy should not hard-wire one transient View topology as the permanent domain model.

### Island / SystemUI transition participation

Combined Status should continue to align with native SystemUI scene/transition behavior rather than implementing a parallel animation authority.

**Design seam to preserve:** stable geometry and transition geometry remain separate, and native visibility/scene ownership should be reused when verifiable.

### Runtime ownership migration

If long-lived lifecycle responsibilities begin accumulating again in the module bootstrap, move bounded ownership into dedicated session/owner components one responsibility at a time.

**Trigger:** a responsibility becomes independently ownable, testable, and removable, or the bootstrap begins retaining lifecycle state that violates the ownership rules.

### Native resource reuse

New HyperOS/SystemUI visual resources should be integrated through verified runtime resource identity and the shared tint/intensity contract rather than copied or manually gray-matched.

**Design seam to preserve:** resource resolution, semantic state, tint authority, and rendering normalization remain independently testable.

## Deferred / rejected approaches

Add an item here when a future-looking idea is intentionally deferred or rejected and the reason matters to later design. Detailed experimental history still belongs in `DEVLOG.md`.

## Update trigger

Update this file when:
- a new planned direction is accepted for later work;
- a prerequisite/trigger becomes known;
- implementation evidence invalidates a planned direction;
- current work introduces a deliberate seam for a future feature;
- an item moves from future intent into an active development objective.
