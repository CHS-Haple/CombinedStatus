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
