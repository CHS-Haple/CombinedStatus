# Runtime Reference Library

This directory stores generalized implementation/platform evidence that may inform future Combined Status work.

It is an engineering reference, not a dependency declaration, implementation lineage, or automatic permission to mutate SystemUI.

## Rules

- Record reusable ownership, lifecycle, geometry, restoration, state-source, or sizing evidence.
- Separate observed evidence from Combined Status design decisions.
- Revalidate a pattern against the exact target before adopting it.
- Do not copy unrelated third-party source code, proprietary assets, or implementation-specific constants merely to document a concept.
- Keep contradictory or superseded evidence when it remains useful, and state its confidence/current applicability explicitly.

## Relationship to current architecture

`docs/development/CURRENT.md` and `docs/architecture/` define what the project currently selects. Reference evidence can support or challenge that selection, but does not override ownership, lifecycle, compatibility, or fail-native rules in `CONTRIBUTING.md`.

Specific reusable reference entries may be added as their evidence is reviewed and generalized.
