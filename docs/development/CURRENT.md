# Current Development State

This file is the concise recovery point for active Combined Status development. Read it after `CONTRIBUTING.md` and before changing code. Keep detailed history in `DEVLOG.md` and future/deferred work in `ROADMAP.md`.

## Repository baseline

- Last refreshed: 2026-09-26
- Stable branch: `main`
- Stable runtime baseline at initialization: Build 351, commit `2477867278483b76b80ed0884de3a07c7ede668a`
- Integration branch: `dev`
- Current integration runtime baseline observed at initialization: Build 377, commit `f64fe0e3992eab4dd62ff479c3765d834ec7dfa4`
- Target profile: HyperOS SystemUI `17.03.260226.r`
- Modern Xposed API: 102
- Application ID: `com.chaners.combinedstatus`

The `main` and `dev` build references above describe the repository state when this development-memory system was initialized. Refresh them whenever either effective baseline changes.

## Current integration state

The Build 377 `dev` commit records the Combined Status master switch as validated on the target SystemUI, preserving native bindable lifecycle, composed battery suppression, and fail-native restoration.

No additional active implementation objective is asserted by this initialization document. The development session that owns the next task MUST update this section from verified current branch/PR/device evidence before or alongside the next engineering checkpoint.

## Confirmed engineering constraints

These are current repository-level constraints, not a substitute for the full contributor rules:

- Prefer verified Android/HyperOS/SystemUI state, behavior, and resources before project-local equivalents.
- Preserve explicit ownership and lifecycle boundaries for long-lived runtime objects.
- Keep native layout slot, Combined Status drawing geometry, transition geometry, and optical adjustment conceptually separate.
- One live property should have one runtime writer.
- Use fail-native behavior when the replacement contract cannot be established safely.
- Reused full-strength native visual resources use the shared visual-intensity normalization path; do not add per-resource grayscale or alpha magic numbers merely for visual matching.
- Use event-driven, bounded diagnostics rather than polling or per-frame logging.
- Device evidence that contradicts a hypothesis reopens the solution choice.

## Session startup checklist

Before continuing development:

1. Read the latest `CONTRIBUTING.md`.
2. Read this file.
3. Read `ROADMAP.md`.
4. Read recent DEVLOG entries plus any older entries relevant to the current symptom/owner.
5. Inspect the actual current branch/PR/CI state when it may have advanced since this file was last refreshed.
6. Update this file if the effective baseline, active objective, confirmed conclusions, validation state, or next step has changed.

## Active objective

Not set by the initialization change. Replace this section with the verified current objective when the next development task begins.

## Validation / blockers

- Development-memory initialization: documentation/governance only; no APK or device validation required.
- Runtime validation state must be maintained by the active development session rather than inferred from this document.

## Immediate next step

The next Combined Status development session should refresh this file from the current repository and device evidence, then proceed under the required read order and record its next CI/build checkpoint in `DEVLOG.md`.
