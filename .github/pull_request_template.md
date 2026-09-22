## Summary

Describe the user-visible or engineering problem and the final change.

## Change boundary

- Verified owner / call chain:
- Changed layers/files:
- Explicitly unchanged layers/files:
- Compatibility assumptions:

## Ownership and lifecycle

For runtime-sensitive changes, state:

- who owns the affected state/host/property;
- whether a new listener, observer, hook, session, or writer is introduced;
- how it is cleaned up or replaced;
- fallback behavior when the integration is unavailable.

Use **N/A** only when the change cannot affect SystemUI runtime behavior.

## Validation

- [ ] Direction and bounded plan were evaluated before mutation.
- [ ] Unit/local checks pass where applicable.
- [ ] CI passes.
- [ ] Changelog entry added or explicitly not required.
- [ ] User-facing copy reviewed if changed.
- [ ] MIUIX UI reviewed if changed.
- [ ] Real-device scenarios are listed below for runtime-sensitive work.

Real-device validation:

- Scenario(s):
- Result: passed / awaiting device validation / N/A

## Diagnostics and evidence

Include only the evidence needed to support the change. Do not attach secrets, signing material, or unrelated personal data.

## Known limitations

List remaining compatibility or validation boundaries.
