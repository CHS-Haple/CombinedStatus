# Documentation map

This directory separates current development state, architecture policy, reusable reference evidence, and chronological engineering history.

## Start here

The existing required startup path remains unchanged:

1. [CONTRIBUTING.md](../CONTRIBUTING.md)
2. [development/CURRENT.md](development/CURRENT.md)
3. [development/ROADMAP.md](development/ROADMAP.md)
4. the recent and historically relevant parts of [development/DEVLOG.md](development/DEVLOG.md)

For SystemUI architecture, geometry, host, scene, transition, or sizing work, continue with:

5. [architecture/README.md](architecture/README.md)
6. [reference/README.md](reference/README.md)
7. the relevant reference/architecture entry for the task

## Authority by purpose

| Need | Document | Meaning |
| --- | --- | --- |
| Engineering rules | `CONTRIBUTING.md` | Normative |
| Current branch truth | `development/CURRENT.md` | Current source of truth |
| Planned direction | `development/ROADMAP.md` | Planning source of truth |
| Architecture policy/status | `architecture/` | Policy plus explicit supersession state |
| Reusable implementation evidence | `reference/` | Evidence only; never automatic write authority |
| Engineering history | `development/DEVLOG.md` | Chronological historical record |
| Release/net-change record | `CHANGELOG.md` | Durable release-state record |

## Historical-record rule

Historical engineering records are intentionally preserved.

When later evidence invalidates an older architecture or hypothesis:

- do not delete or rewrite the historical entry;
- do not make an old result appear as if it never happened;
- add a current supersession/status notice in the appropriate current-policy document;
- append a later correction to the development log when a durable engineering conclusion changes.

A historical implementation may remain useful evidence while being explicitly **not approved as the current architecture**.
