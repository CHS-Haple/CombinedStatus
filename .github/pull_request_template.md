## Summary

Describe the problem or engineering need and the final bounded change.

## Change classification

- Branch role: feat / fix / promote / hotfix
- Target branch: dev / main
- Promotion candidate `dev` revision (promotion only):
- Hotfix source `main` revision (hotfix only):

## Change boundary

- Verified owner / call chain or governing rule:
- Changed layers/files:
- Explicitly unchanged layers/files:
- Compatibility assumptions:
- Boundary check: could any included change be independently tested or reverted without invalidating the rest? If yes, explain why it remains one change.

## Ownership and lifecycle

For runtime-sensitive changes, state:

- who owns the affected state/host/property;
- whether a new listener, observer, hook, session, or writer is introduced;
- how it is cleaned up or replaced;
- fallback behavior when the integration is unavailable.

Use **N/A** only when the change cannot affect SystemUI runtime behavior.

## Validation

- [ ] Declared change boundary is complete.
- [ ] No known deterministic blocker remains inside the boundary.
- [ ] Unit/local checks pass where applicable.
- [ ] Required CI passes.
- [ ] Changelog entry added or explicitly not required.
- [ ] User-facing copy reviewed if changed.
- [ ] MIUIX UI reviewed if changed.

### Real-device validation

- Required: yes / no
- Scenario(s):
- Tested build / source revision:
- Result: passed / awaiting device validation / N/A
- If awaiting device validation, why integration through `dev` is required:

`awaiting device validation` may be used only when the remaining validation reasonably depends on a trusted `dev` build, interaction with other integrated work, or another integration-only condition. It blocks promotion of the affected state to `main`.

## Promotion / hotfix closure

For a `promote/* -> main` PR:

- [ ] The branch points to the exact validated `dev` candidate and contains no new functional or engineering work.
- [ ] All required device-validation scenarios for the candidate passed.
- [ ] No affected change remains `awaiting device validation`.

For a `hotfix/* -> main` PR:

- Back-propagation plan to `dev`:
- [ ] The hotfix is limited to the stable-baseline defect and contains no unrelated work.

## Diagnostics and evidence

Include only the evidence needed to support the change. Do not attach secrets, signing material, or unrelated personal data.

## Known limitations

List remaining compatibility or validation boundaries.
