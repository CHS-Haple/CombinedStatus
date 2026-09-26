# Architecture documentation

This directory contains current reusable architecture policy for Combined Status. It describes ownership, geometry, scene, cleanup, and compatibility boundaries rather than build-by-build history.

## Documents

- [layout-policy.md](layout-policy.md) — shared geometry, layout ownership, visual-width, transition, and fallback policy.
- [scene-policy.md](scene-policy.md) — scene capability boundaries for Home, panel transitions, keyguard, AOD, and related SystemUI surfaces.

## Authority

- `docs/development/CURRENT.md` defines the current active project state.
- Architecture files define reusable current policy and must not become a chronological DEVLOG.
- Historical experiments and invalidated routes remain in `docs/development/DEVLOG.md`.
- Reference evidence under `docs/reference/` may inform architecture, but evidence alone never grants write ownership.

When an active work branch changes an architecture contract, update the applicable policy only when that contract is the current selected direction; preserve older investigation history in DEVLOG rather than rewriting it.
