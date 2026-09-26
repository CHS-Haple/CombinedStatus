# Runtime Reference Library

This directory stores generalized runtime-integration patterns that have been verified from mature Android/SystemUI implementations or exact target-platform behavior and may inform future Combined Status work.

It is an **engineering reference**, not a dependency declaration, implementation lineage, or permission to copy another project's code.

## Rules

- Record reusable behavior and ownership patterns, not third-party product/package/class names.
- Do not copy third-party source code, proprietary assets, or implementation-specific constants into this repository.
- Keep platform-specific identifiers only when they are necessary to describe a verified target-SystemUI contract.
- Separate **observed behavior** from **Combined Status design decisions**.
- A reference pattern is not automatically valid on the current target. Revalidate the host, lifecycle, writer, fallback, and device behavior before adopting it.
- Prefer the smallest reusable concept: ownership, lifecycle, geometry, restoration, projection, or sizing contract.
- Keep contradictory or superseded evidence rather than converting it into an unqualified rule.

## Current entries

- [Status-bar composition and scene-projection patterns](statusbar-composition-patterns.md) — existing-host composition, scoped slot suppression, reversible visual masking, host-scoped state, sizing separation, native-progress projection, and cleanup/fail-native behavior.

## Confidence language

- **Observed** — directly evidenced in the inspected implementation/runtime.
- **Strong inference** — supported by several independent observations but not directly exposed as one explicit contract.
- **Candidate for Combined Status** — potentially useful architecture; still requires target-specific validation.
- **Not established** — insufficient evidence to use as a design premise.
