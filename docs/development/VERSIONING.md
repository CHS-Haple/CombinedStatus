# Versioning plan

## Current development version

The active display-version line is **0.0.2**.

Development versions continue to advance according to actual engineering milestones and explicit maintainer direction. A development checkpoint does not become a formal release merely because its display version changes.

## First formal release target

The first planned formal release is **1.0.0**.

The project is not yet considered to have reached that release boundary.

Until the 1.0.0 acceptance boundary is satisfied:
- development may continue through `0.0.x` display versions;
- build/internal identity continues to follow the repository build rules;
- Canary/Debug checkpoints remain development artifacts;
- do not change the display version to `1.0.0` solely because a feature or architecture phase completes.

The display version may become `1.0.0` only when:
1. the maintainer explicitly authorizes the formal-release version transition;
2. the supported scene/compatibility scope intended for the first release has passed its acceptance matrix;
3. release documentation and changelog state are prepared;
4. formal Release validation/signing gates pass.

## Historical versions

Historical Build records keep the display version that was actually used at that time.

Do not rewrite older `0.0.1` / `0.0.2` DEVLOG or CI history to match the current release plan.


## Work-branch Canary validation entry

Focused work-branch device testing normally uses the trusted automatic path:

`ready feat/* or fix/* PR -> Fast Build -> default-branch Work Branch Canary -> signed non-debuggable APK`.

If GitHub does not deliver the pull-request event into Actions, the default-branch `Work Branch Canary` keeps the validation checkpoint reachable through trusted fallback admission. The normal operator fallback is an exact repository-owner `/canary` comment on an open same-repository PR with a `feat/*` or `fix/*` head; the workflow resolves the PR's live head branch/SHA before checkout. A repository-owner-only manual dispatch for an explicit same-repository work branch remains the final fallback. Both paths record the exact resolved source and perform the same target-profile, tests, Xposed metadata, Haple signature and non-debuggable checks before artifact publication.

These fallbacks do not change application version semantics, do not create a release, do not establish a `dev` integration baseline, and do not authorize merge.
