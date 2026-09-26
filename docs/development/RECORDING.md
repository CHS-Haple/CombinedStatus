# Development record writing guide

This guide defines **how** Combined Status engineering records are written.

`CONTRIBUTING.md` is the normative source for **when** a record is required and which document class must be synchronized. This file standardizes structure, evidence language, duplication boundaries, and maintenance style so another contributor can recover the same engineering state without chat history.

## 1. General writing rules

### 1.1 Separate current truth from history

- Current-state documents describe what is true **now**.
- `DEVLOG.md` records what was actually investigated, believed, implemented, tested, rejected, or accepted **at that time**.
- Never rewrite an old DEVLOG entry merely because later evidence changed the conclusion.
- When later evidence invalidates an older conclusion, append a correction and update the current-state document that depended on it.

### 1.2 Separate evidence, conclusion, and hypothesis

Use language that reflects evidence strength:

- **Observed / verified** — directly established by source inspection, runtime evidence, CI output, device feedback, or another attributable artifact.
- **Confirmed conclusion / root cause** — supported strongly enough to guide implementation.
- **High-confidence conclusion** — multiple observations agree, but one direct contract is still missing.
- **Hypothesis / open question** — not yet proven; must not be written as established fact.
- **Rejected / superseded** — no longer the active route; preserve why it was rejected.

Do not upgrade a hypothesis to a root cause merely because an implementation based on it happened to improve one symptom.

### 1.3 Use concrete identities

When applicable, record:
- display version;
- Build ID;
- source commit SHA;
- branch / PR;
- CI workflow/run;
- artifact identity and checksum;
- target SystemUI/device fingerprint.

Avoid durable phrases such as “latest build”, “current APK”, or “the previous version” without the concrete identity they refer to.

### 1.4 Keep one logical boundary

One record should describe one attributable engineering checkpoint or one coherent investigation.

Do not create separate entries for mechanical sub-steps such as:
- opening a file;
- renaming a local variable;
- rerunning formatting;
- reading one source file when it does not change the engineering conclusion.

Create or extend a record when the active problem, hypothesis, architecture boundary, implementation, validation state, or next step materially changes.

### 1.5 Record what did not change

For runtime-sensitive work, state important intentionally unchanged behavior when that protects the change boundary.

Examples:
- native tint authority unchanged;
- no new listener/hook;
- Phase 2B remains out of scope;
- no Battery translation/alpha/visibility writer added.

This makes regression attribution easier.

## 2. File responsibilities

### `CURRENT.md` — current recovery point

Purpose: let a new session recover the active engineering state quickly.

Keep:
- current branches / PR / display line;
- accepted baseline and current checkpoint;
- active phase;
- current architecture/ownership boundary;
- confirmed current issue;
- current validation state;
- immediate next step;
- critical non-negotiable boundaries.

Do not keep:
- full Build-by-Build chronology;
- long rejected-hypothesis narratives;
- old CI result lists after they stop affecting the current decision;
- duplicated DEVLOG history.

**Rule of thumb:** if removing a paragraph would not make the next contributor choose the wrong next action, it probably belongs in DEVLOG instead.

Recommended structure:

~~~markdown
# Current Development State

## Repository baseline
## Current phase
## Current architecture / ownership boundary
## Validation state
## Active problem or checkpoint objective
## Non-negotiable boundaries
## Immediate next step
## Reference priority
~~~

### `DEVLOG.md` — chronological engineering history

Purpose: preserve engineering reasoning and evidence across checkpoints.

Every APK-affecting engineering checkpoint must have an attributable entry. Major architecture/root-cause/compatibility conclusions should also be recorded even without a build.

Recommended entry structure:

~~~markdown
## YYYY-MM-DD — Short checkpoint title

**Type:** ...
**Display version:** ...
**Build / source:** ...
**Validation:** ...

### Problem / objective
What is wrong, missing, or being tested?

### Problem execution flow
What evidence path was followed?

### Evidence / findings
What was directly observed or verified?

### Root-cause status
Confirmed / high-confidence / open hypothesis, with boundaries.

### Alternatives considered
What plausible alternatives were reviewed and why were they rejected/deferred?

### Implementation / decision
What changed, who owns it, and what intentionally did not change?

### Review
- Ownership:
- Lifecycle:
- Single writer:
- Cleanup:
- Fail native:
- Performance:
- Compatibility:
- Future extension:

### CI / device validation
Concrete run/build/artifact/device result.

### Outcome / next step
Accepted, rejected, superseded, still pending; what happens next?
~~~

Not every heading is mandatory for trivial work. Use only sections that carry real information, but do not omit a relevant risk dimension just to make the entry shorter.

Corrections must be appended as later entries or clearly appended correction sections. Do not silently edit the old conclusion into the new one.

### `ROADMAP.md` — future direction

Purpose: preserve planned sequence, confirmed future design, prerequisites, exit criteria, and deferred/rejected routes that constrain future work.

Keep:
- macro phases;
- active phase route;
- phase exit criteria;
- confirmed future product design;
- prerequisites / trigger conditions;
- deliberately deferred work;
- rejected routes when forgetting them would cause likely regression.

Do not keep:
- detailed CI history;
- every Build correction;
- transient investigation notes;
- current checkpoint details that do not affect future sequencing.

When a route is implemented and becomes current architecture, move detailed current behavior into `CURRENT.md` / architecture policy and leave only the remaining roadmap consequence here.

### `docs/architecture/` — current reusable architecture policy

Purpose: describe stable ownership, lifecycle, geometry, scene, cleanup, and compatibility contracts.

Write in mechanism/policy terms rather than Build chronology.

Good:
- “Home island motion is SystemUI-owned through the animated host.”
- “The Home session restores only its own applied clip/padding state.”

Avoid:
- a long sequence of Build 394 -> 395 -> 396 experiments;
- temporary diagnostics;
- historical narrative already preserved in DEVLOG.

Historical Build evidence may be cited briefly when needed to explain why a route is prohibited.

### `docs/reference/` — reusable evidence

Purpose: preserve generalized implementation/platform evidence that may inform future work.

Each entry should distinguish:
1. **Observed evidence**;
2. **Reusable principle**;
3. **Combined Status applicability / current adoption status**;
4. **What is not established**.

Do not turn a reference pattern into automatic write authority. Exact-target verification remains required.

Do not copy unrelated project identities, implementation-specific source code, proprietary assets, or constants merely to document the concept.

### `CHANGELOG.md` — durable net project change

Purpose: describe the current unreleased net state and released changes.

Write what the project **ends up doing**, not the sequence of experiments used to get there.

Include:
- durable user-visible behavior;
- meaningful compatibility/runtime behavior;
- durable engineering/repository behavior when appropriate.

Exclude:
- failed experiments;
- diagnostic-only checkpoints;
- superseded mechanics;
- Build-by-Build debugging chronology.

### `VERSIONING.md`

Purpose: current display-version semantics and formal-release boundary only.

Historical Build versions stay in DEVLOG. Do not rewrite historical version identity to match the current plan.

## 3. Synchronization after a meaningful checkpoint

Follow the mapping in `CONTRIBUTING.md`:

| Engineering result | Update |
| --- | --- |
| Active problem, conclusion, blocker, validation, or next action changes | `CURRENT.md` |
| Major investigation / Build / device result | `DEVLOG.md` |
| Future sequence, prerequisite, trigger, phase boundary changes | `ROADMAP.md` |
| Architecture/ownership contract changes | relevant `docs/architecture/` + current/roadmap as applicable |
| Reusable evidence changes | relevant `docs/reference/` |
| Durable net behavior changes | `CHANGELOG.md` |
| Display/release semantics change | `VERSIONING.md` + affected public text |
| Active PR purpose materially changes | PR title/body |

Synchronize the affected record **before treating the checkpoint as complete**.

## 4. Cross-file consistency rules

- `CURRENT.md` may summarize a DEVLOG conclusion but must not reproduce the whole historical entry.
- `ROADMAP.md` must not describe an already accepted current mechanism as if it were still an unproven future candidate.
- Architecture policy must not preserve a superseded mechanism as the active recommendation.
- Reference evidence may remain historically true even when Combined Status chooses a different implementation.
- `CHANGELOG.md` must describe final net behavior, not abandoned intermediate implementation details.
- Public README text must never present work-branch-only behavior as stable `main` behavior unless explicitly scoped as development state.

## 5. Review before committing record changes

For a meaningful engineering record, check:

- Does the title say what changed rather than only the Build number?
- Are facts separated from inference?
- Is the actual target/build/source identity concrete?
- Is the root-cause status explicit?
- Are rejected alternatives preserved when they matter?
- Are ownership/lifecycle/single-writer/cleanup/fail-native boundaries recorded where relevant?
- Is validation status factual rather than implied?
- Is the next step actionable?
- Did any current/future document become stale because of this result?
- Did this record accidentally duplicate content that belongs in another layer?

The goal is not maximal documentation volume. The goal is **accurate, recoverable engineering state with minimal duplication**.
