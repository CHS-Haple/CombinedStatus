# Contributing to CombinedStatus

This document is the engineering source of truth for CombinedStatus contributors. Apply its rules in proportion to risk: runtime-sensitive SystemUI work needs deeper ownership and device validation, while deterministic mechanical maintenance should remain lightweight.

## 1. Scope, language, and licensing

These rules apply to application code, SystemUI integration, runtime state, compatibility logic, diagnostics, UI/resources, dependencies, build/release logic, CI, engineering-governance documentation, and migration work.

Normative terms are deliberate:

- **MUST / MUST NOT** — required.
- **SHOULD / SHOULD NOT** — the default; deviations need a concrete reason.
- **MAY** — optional.

A justified exception to a MUST-level architectural rule must record the evidence, affected lifecycle, rollback/fallback boundary, compatibility risk, and required validation.

The application ID and package namespace are `com.chaners.combinedstatus`. Changing that identity requires an explicit compatibility and migration plan.

CombinedStatus is licensed under the [Apache License 2.0](LICENSE). Contributions submitted for inclusion are provided under the same license unless explicitly stated otherwise. Contributors must have the right to submit their material and must preserve required third-party attribution, notices, and license obligations.

## 2. Project principles

Every change should preserve four qualities.

### 2.1 Standardized

Follow Android, HyperOS, MIUIX, and Modern Xposed contracts before inventing project-specific behavior. Keep lifecycle, ownership, state flow, compatibility boundaries, and platform responsibilities explicit.

### 2.2 Lightweight

"Lightweight" means reducing unnecessary runtime work and architectural redundancy, not merely APK size.

Avoid unnecessary polling, duplicate state, duplicate hooks/listeners, resident Root processes, background services, repeated View-tree traversal, hot-path reflection, per-frame logging, and wakeups more frequent than the underlying state can meaningfully change.

### 2.3 Modern

Prefer maintained platform/library APIs when they satisfy the requirement. Build files and compatibility profiles are the source of truth for pinned versions.

A newer API or dependency is not automatically better. Adoption still needs lifecycle, stability, compatibility, and maintenance evidence.

### 2.4 Fail native

When CombinedStatus cannot safely establish the required contract, degrade toward native HyperOS behavior rather than leaving a partially active replacement.

Do not hide a native representation until the replacement is valid for the current session. Compatibility failure should disable the smallest affected feature, not destabilize SystemUI.

## 3. Change method

### 3.1 Evaluate before editing

Before changing code or behavior:

1. confirm the actual problem or requirement;
2. identify the relevant owner, state source, lifecycle, API contract, or layout rule;
3. define the smallest useful change boundary;
4. identify what must remain unchanged;
5. decide what evidence will prove the result.

Prefer a source-level fix over symptom compensation. Do not patch speculative behavior because it is easy to change.

### 3.2 Investigate in root-cause order

For defects, regressions, incompatibilities, and unexpected behavior:

1. **Find the root cause.**
2. **Fix the responsible source when practical.**
3. **Check project rules and authoritative upstream guidance.**
4. **Compare established patterns when authoritative guidance is insufficient.**
5. **Use a workaround only as the last viable option.**

A workaround must be narrow, conditional, removable, and documented with the reason a direct fix is not currently viable.

### 3.3 Keep one clear boundary

Planning depth should match risk.

For mechanical documentation/metadata work, affected files plus applicable validation are normally enough.

For runtime-sensitive, architectural, compatibility, build/release, or user-visible changes, identify as applicable:

- owner/call chain and affected layer;
- intentionally unchanged behavior;
- lifecycle/state/compatibility assumptions;
- diagnostics and fallback/rollback boundary;
- build-channel impact;
- CI and real-device validation.

Do not silently expand implementation into unrelated cleanup, speculative fixes, or additional features.

If new evidence changes the assumed owner, call chain, or platform behavior, stop expanding the current solution. Reframe the boundary before continuing.

### 3.4 Let evidence change the solution

New logs, recordings, crash traces, topology/geometry snapshots, or runtime evidence must reopen the solution choice when they contradict the current hypothesis.

Previous engineering effort is not evidence that the current path is still correct.

When several hypotheses remain plausible, prefer single-variable A/B diagnostics. Several changes may share one test build only when ownership, evidence, acceptance criteria, and rollback remain independently attributable.

## 4. Runtime architecture and SystemUI ownership

### 4.1 Ownership and lifecycle

Every long-lived runtime object needs one explicit owner. This includes hosts, Views, sessions, state observers, listeners/callbacks, hook handles, animations, transition state, and host-derived caches.

For each owned runtime object, be able to answer:

1. who creates it;
2. who owns it;
3. when it becomes invalid;
4. who disposes/replaces it;
5. what happens on host replacement, SystemUI recreation, or hot reload.

Prefer:

`Host -> HostSession -> owned resources`

over global mutable ownership of multiple hosts.

Host-derived state stays host-scoped. A stale View, geometry snapshot, listener, or transition state must not survive host replacement.

Any session that owns resources must have an explicit cleanup path. Cleanup includes applicable observer/listener unregistering, callback cancellation, module-owned animation cancellation, hook/session release, cache invalidation, and reference clearing.

### 4.2 State flow, writers, and hooks

Runtime behavior should follow:

`native/event source -> domain state -> scene/presentation policy -> renderer`

State acquisition reports facts; presentation policy decides how/where to show them; rendering consumes resolved presentation state. Avoid callbacks that mix acquisition, lifecycle decisions, layout ownership, and drawing.

Prefer authoritative native state. If a fallback/duplicate source exists, define when it is active, which source wins, how disagreement is resolved, and when fallback state is discarded.

Observation does not grant ownership. A hook, reflection lookup, topology probe, or geometry sample may explain SystemUI behavior without giving CombinedStatus permission to write that property.

One live property should have one runtime writer. Treat measured/layout width, position, translation, alpha, visibility, tint, animation state, and parent/child attachment as ownership-sensitive. Do not add a second writer merely to counteract the first.

Hooks are integration points, not architecture. Each hook needs one responsibility, an owning subsystem, a lifecycle boundary, failure behavior, and a cleanup/hot-reload strategy where applicable.

`CombinedStatusModule` is a bootstrap/integration boundary. It may coordinate compatibility checks and top-level hook installation, but long-lived state/host responsibilities should move to dedicated owners rather than accumulating there.

When ownership moves, move one bounded responsibility at a time, keep one active owner, avoid unrelated feature/visual changes, and keep the migration independently testable and reversible.

### 4.3 Geometry and visual ownership

Keep these responsibilities conceptually separate:

1. native SystemUI layout slot;
2. CombinedStatus visual/drawing geometry;
3. transition/animation geometry;
4. optical adjustment.

A visual-width requirement does not automatically justify native layout-width mutation. An animation correction does not automatically change stable geometry. An optical offset must not silently become layout ownership.

Prefer solving CombinedStatus-specific appearance inside its own presentation/rendering layer. Native measured width, layout width, translation, or visibility writes are exceptional and require verified runtime evidence, a single owner, narrow scope, reversibility, and focused device validation.

Pixel correctness in one scene is not enough. For runtime-sensitive geometry or animation work, verify the expected writer, absence of competing writers, stable/transition separation, host replacement, and the relevant Home, keyguard, AOD, shade/Control Center, charging/island, and recreation paths.

### 4.4 Compatibility and fallback

Prefer exact verified integration points over broad reflection. A class/member existing in an APK does not prove that it owns live behavior.

Compatibility-sensitive hooks must use verified members from the pinned target profile where appropriate. Current target baseline:

- HyperOS SystemUI `17.03.260226.r`
- Modern Xposed API `102`

Keep one Java entry unless a verified API requirement changes that decision.

Reference strength must follow ownership. Non-owning observers should not extend host/View lifetime; owning sessions may retain strong references only for their valid session.

Live runtime topology is authoritative. Other modules and HyperOS variants may change the SystemUI tree, so inspect runtime parentage/identity when it matters.

Historical builds, constants, and patches are evidence, not reusable architecture. Reuse the verified requirement only after confirming that the old owner/cause still exists and that the historical fix does not violate current ownership.

## 5. Diagnostics, performance, app UI, and text

### 5.1 Diagnostics and runtime cost

Build channels must not silently change core feature semantics.

**Release** may retain only low-frequency operational diagnostics such as build identity, compatibility readiness, lifecycle/hook readiness, hot-reload result, and important errors.

**Debug** may additionally expose bounded topology, geometry, state, lifecycle, and ownership probes.

Prefer:

`event -> bounded snapshot -> report`

over polling or continuous sampling.

Before adding a hook, observer, listener, coroutine, callback, reflection path, shell call, or View-tree probe, ask whether it wakes more often than the underlying state can meaningfully change.

Prefer native callbacks, one-shot inspection, cached immutable metadata, shared helpers, scoped coroutines, and bounded user-triggered Root work. Root work should be user-triggered where practical and must have timeout/failure handling.

### 5.2 App and MIUIX

Persist only real user preferences. Do not confuse UI preview state with module runtime state.

For UI changes, review against the currently pinned MIUIX build. Prefer official components/defaults for spacing, typography, shape, pressed/disabled states, dialogs, navigation, and backdrop behavior.

Check the affected information hierarchy, section grouping, light/dark/dynamic themes, predictive/system back, transitions, accessibility, localized text expansion, and dialog action hierarchy.

Do not hand-tune values merely to imitate MIUIX when an official component already provides the intended behavior.

### 5.3 Text and public documentation

Any user-facing text change needs copy review.

Fixed terminology:

- Chinese: **移动网络**
- English: **mobile network**
- Internal domain naming: `mobileNetwork` / `mobileSignal`

Keep exact upstream Android/HyperOS class, field, method, and resource identifiers unchanged.

Normal settings UI should not expose internal terms such as host, role, writer, hook chain, probe path, or implementation class unless the screen is explicitly diagnostic.

Public documentation should describe current behavior, compatibility, architecture, dependencies, and user-facing limitations. Omit superseded implementation lineage and unrelated projects. Document actual third-party dependencies/assets and required notices accurately.

## 6. Git and branch workflow

Choose the lightest path that preserves correctness.

### 6.1 Route the change first

#### A. Repository text and governance

Text-only repository maintenance does not participate in runtime stability promotion when it cannot affect the installed application, SystemUI integration, build output, dependency resolution, compatibility, signing, CI/release execution, or published release facts.

Such changes may go directly to `main` with lightweight review/checking and be mirrored promptly to `dev`. They do not require a work branch, Canary, device validation, `validation/dev`, or a `dev -> main` promotion checkpoint.

This route includes, when their effect is purely textual:

- README/docs corrections and clarification;
- developer/contributor documentation and normative engineering guidance;
- typo/grammar/formatting/dead-link fixes;
- comments and non-executable metadata;
- documentation restructuring that preserves accurate current project state.

Normative contributor rules are judged for **semantic consistency**, not runtime stability. They may use this route as long as the edit itself does not change executable automation, dependency/build behavior, compatibility contracts, release artifacts, or runtime behavior.

Documentation that describes a runtime feature not yet present on `main` must not present that feature as current stable behavior. Either keep that user-facing documentation with the feature promotion or clearly scope it to development state.

Because `dev` continues toward the next stable baseline, keep equivalent repository-policy/documentation changes synchronized across `main` and `dev`. The commits need not share a SHA; the effective text must remain consistent where the branches are intended to share policy.

If a supposedly textual change requires executable workflow/configuration changes to become true, it is not text-only and must use the applicable engineering path.

File extension does not determine the route. YAML, Gradle, scripts, release metadata, and compatibility data remain engineering inputs even though they are text files.

#### B. Normal product/engineering work

Behavioral, architectural, compatibility, dependency, build/CI, release-automation, or runtime-affecting changes use:

`feat/* or fix/* -> dev -> validated promotion -> main`

Continue an existing unmerged work branch when it already owns the same objective and acceptance boundary. A new feature does **not** automatically justify a new branch.

#### C. Urgent stable-baseline defect

Use `hotfix/* -> main` only when the current `main` baseline has an urgent defect that should not wait for the normal `dev` cycle. Propagate the equivalent fix back to `dev` before the next promotion.

### 6.2 Branch roles

- `feat/*` — one bounded capability, intentional behavior change, architecture/ownership migration, dependency adoption, or engineering-governance change.
- `fix/*` — one bounded correction for intended behavior that is already defined.
- `dev` — integration branch for completed work and integrated validation.
- `validation/dev` — state marker for the most recent `dev` runtime baseline whose required integrated device scenarios passed; it is not a development branch and must contain no unique commits.
- `promote/*` — exact validated `dev` candidate for `main`; no new feature/fix/cleanup belongs here.
- `hotfix/*` — urgent isolated correction created from `main`.
- `main` — current stable, installable, accepted baseline.

### 6.3 Work-branch admission

A work branch represents one independently mergeable/reversible change boundary, **not** every task, sub-feature, file, build, experiment, visual adjustment, or test response.

Before creating another `feat/*` or `fix/*`, check whether an active unmerged branch already owns the same objective. Stay in that branch for coherent implementation sub-steps, diagnostics, validation fixes, acceptance-driven UI/behavior adjustments, and multiple checkpoints.

Create a new work branch only when at least one is true:

- the work can be merged/shipped independently;
- it can be reverted independently without invalidating the active branch;
- it has a different root cause, runtime/engineering owner, or validation surface;
- real parallel development requires isolation;
- keeping it together would mix unrelated work or destroy failure attribution.

Keep simultaneously active work branches low. A branch with no active PR, no diagnostic value, and no meaningful progress for seven days should be reviewed for closure rather than kept indefinitely.

### 6.4 Work-branch lifecycle and merge gate

Normal lifecycle:

1. create from current `dev`;
2. implement only the declared boundary;
3. open/update a PR to `dev` when useful;
4. satisfy applicable review/CI/validation;
5. squash merge to `dev`;
6. delete the branch after no explicit short-term rollback/diagnostic need remains.

Opening a PR does not freeze development. Related follow-up inside the same acceptance boundary should remain in that branch until merge.

Do not reuse a merged branch. Abandoned/superseded branches should be closed and removed once their diagnostic value is exhausted.

A work branch may merge to `dev` when:

- its boundary is complete;
- deterministic blockers are resolved;
- required CI passes;
- diagnostics/fallback are adequate for the risk;
- branch-level validation passed or remaining integrated device validation is explicitly marked `awaiting device validation`.

`awaiting device validation` is allowed only when validation genuinely depends on a trusted `dev` build or integrated state. It blocks promotion to `main`.

Known reproducible crashes, ownership conflicts, invalid fallbacks, or failed required device scenarios must not be merged merely to obtain another build.

### 6.5 Promotion and device-validation marker

Promotion is a stability decision, not ordinary development.

After all required integrated device scenarios pass for one `dev` runtime baseline and no known runtime blocker remains, `validation/dev` may move to that SHA.

A later `dev` commit does not automatically invalidate device validation. Readiness may carry the validation forward only when automation can prove that every change since `validation/dev` is outside APK/runtime-affecting paths. Any runtime/build/compatibility delta that can change the installed behavior makes the marker stale for promotion purposes and requires applicable re-validation.

Never move `validation/dev` merely to satisfy a gate.

Promotion readiness is READY only when the candidate:

- is ahead of `main`;
- has a successful trusted `dev` Build;
- is device-validated directly or inherits validation only across a proven non-runtime delta;
- has the required changelog boundary;
- has no unresolved blocker or affected `awaiting device validation` state.

Create `promote/*` from the exact READY `dev` SHA. The promotion branch contains no new functional/engineering work. Any required fix returns through `feat/*` or `fix/*` and `dev` first.

Use a merge commit for `promote/* -> main` so the stable-baseline boundary remains explicit.

### 6.6 Hotfix

A hotfix must:

- branch from the affected `main`;
- contain the smallest necessary correction;
- avoid unrelated cleanup/features/dependency adoption/architecture migration;
- pass validation appropriate to the defect;
- merge to `main`;
- be propagated back to `dev` before the next normal promotion.

Use squash merge by default.

### 6.7 Cleanup and repository history

Use squash merge for `feat/* -> dev` and `fix/* -> dev`. Internal branch commits may remain granular for development/review.

Merged short-lived branches should be removed automatically when role and target are provably correct; otherwise clean them up manually. Never auto-delete `main`, `dev`, `validation/dev`, fork branches, or unknown branch roles.

## 7. Versioning, changelog, and release

### 7.1 Versioning

The display version changes only when the formal external version is explicitly advanced.

Normal APK-affecting iterations advance internal `versionCode` / `buildId` according to project rules. Documentation-only changes do not require an APK build-number bump.

GitHub Actions run numbers are CI execution metadata, not application version identifiers.

### 7.2 Changelog

`CHANGELOG.md` records durable **net project state**, not the development diary.

Add an entry when a change materially affects user-visible behavior/settings, supported runtime behavior, compatibility/fallback, contributor-relevant diagnostics, build/release channels, persistent architecture/lifecycle ownership, or engineering rules that future work must follow.

Use the smallest accurate category:

- **Added** — new durable capability.
- **Changed** — existing capability behaves/presents/integrates differently.
- **Fixed** — incorrect behavior corrected.
- **Removed** — meaningful mechanism intentionally removed.
- **Engineering** — durable non-user-facing architecture/lifecycle/compatibility/build/contributor constraint.

Normally omit CI run numbers, temporary probes, intermediate UI attempts, trial constants, failed hypotheses, one-off instrumentation, superseded implementations, and internal cleanup with no durable effect.

One bullet should describe one final outcome in present-state wording. Collapse superseded experiments into the final result.

Before keeping an entry, ask: would it still be true and useful to someone who sees only the final project state? If not, it usually belongs in Git/PR history instead.

### 7.3 Release boundary

Before the first formal release, `[Unreleased]` describes the current net state intended for that release.

For a formal release:

1. freeze applicable `[Unreleased]` changes into `## [<version>] - YYYY-MM-DD` in the final release-preparation commit;
2. leave a fresh `[Unreleased]` section;
3. publish only from the prepared `main` commit;
4. do not add unrelated development between preparation and publication.

Stable publication must verify the intended display version, dated changelog section, release notes, tag uniqueness, target profile/tests, Xposed metadata, non-debuggable release properties, and expected signing.

Stable tags/APK filenames use application release/build identity rather than CI run numbers.

## 8. Validation and CI

Validation is **checkpoint-based and risk-based**, not commit-based.

### 8.1 Local/checkpoint validation

Atomic commits may accumulate between meaningful checkpoints. Do not create commits merely to trigger CI.

For an ordinary code checkpoint presented for integration, the normal local baseline is:

~~~bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
~~~

Add applicable checks when Release/Canary behavior, resources, shrinking, Xposed metadata, dependencies, or build configuration change.

Never commit local SDK paths, signing material, generated APK/AAB files, or environment-specific Gradle configuration.

### 8.2 CI scope

CI verifies source/build state; it does not prove SystemUI runtime correctness.

Use the lightest applicable CI:

- Draft PR — lightweight repository checks unless deeper validation is specifically needed.
- Ready PR to `dev` — path-aware; full Android validation only when APK/build/compatibility/CI-affecting paths require it.
- Mechanical direct maintenance on `main` / `dev` — lightweight checks only when the change is proven non-behavioral.
- PR to `main` for promotion/hotfix — full applicable validation.
- Trusted APK-affecting pushes to `dev` / `main` — signed Debug/Canary validation where applicable.
- Release workflow — deliberate publication only.

A CI-workflow change is itself CI-affecting and requires full validation.

Superseded runs for the same PR/branch should be cancelled when a newer source state makes them irrelevant.

Pull-request CI must remain safe for untrusted forks: never require or expose repository signing secrets. Secret-independent tests/build/compatibility/metadata checks are allowed; project-signed artifacts remain a trusted-maintainer responsibility.

### 8.3 Device validation

Real-device testing is tied to observable behavior checkpoints, not individual commits.

Request/repeat device testing when:

- a state first becomes meaningfully testable;
- device evidence is needed to choose between hypotheses;
- a later change can affect a previously validated owner/lifecycle/scene/transition/geometry/compatibility path;
- a `dev` integration checkpoint needs cross-feature validation;
- a candidate is being prepared for `main`.

Use impact-based regression scope. Test the affected behavior plus credible shared dependencies, not unrelated full-device scenarios after every commit.

Several completed changes may share one `dev` integration checkpoint only when acceptance criteria and failure attribution remain clear. If not, split validation or return to single-variable A/B.

A runtime-sensitive result is complete only when the declared scenarios pass against the accepted source/build state. "Installed successfully" or "did not crash once" is not enough for a broader runtime plan.

### 8.4 Failure handling

Understand a failed CI run before retrying.

- Fix deterministic code/config/dependency/metadata/signing failures and validate the new source state.
- Rerun the same SHA only with reasonable evidence of a transient runner/network/package/upstream failure.
- If unclear, inspect logs or reproduce first.
- Do not rerun repeatedly to obtain a green result from an unresolved deterministic failure.

Before `dev -> main` promotion, all required focused device validation must pass and no affected change may remain `awaiting device validation`.

## 9. Upstream dependency adoption

Do not adopt a dependency update merely because it is newer.

### 9.1 Classify relevance and maturity

Classify meaningful upstream changes:

- **A — priority**: fixes a current/likely project issue, removes a workaround, addresses lifecycle/state/crash risk, or contains important maintainer guidance for an API/component in use.
- **B — canary candidate**: clear interaction/stability/performance/compatibility/maintainability benefit worth isolated validation.
- **C — normally ignore**: unrelated churn, docs/examples only, or components not used by CombinedStatus.

Treat maturity separately:

1. open/experimental PR — investigation only;
2. merged main/canary — may be considered when an exact published revision exists and passes the project gate;
3. stable release — preferred when it already contains the needed change.

Maintainer warnings, known regressions, incomplete follow-ups, or missing artifacts block adoption regardless of freshness.

### 9.2 Exact-revision gate

Pin one exact revision and verify evidence for that same revision.

For MIUIX main snapshots, require successful upstream tests/example build/package publication, actual resolvability of the commit-specific snapshot, consistent revision across all MIUIX modules in use, and credentials outside the repository.

Use equivalent evidence for other dependencies where available. Do not substitute an unpublished head for the newest fully green published revision.

### 9.3 Adoption workflow

Dependency adoption follows the normal branch-admission rules. Use one bounded dependency work branch only when an existing active branch does not already own the same adoption boundary.

Normal sequence:

1. inspect upstream issue/PR/maintainer guidance and classify relevance;
2. identify the exact revision and maturity;
3. centralize related version identity where practical;
4. update related modules atomically;
5. update notices/repository references where applicable;
6. run CI and dependency-only Canary when runtime/UI may change;
7. separate a new upstream API integration from dependency adoption when that materially improves fault isolation;
8. perform focused device validation;
9. review for unrelated changes;
10. squash into `dev` and remove the short-lived branch.

After a dependency revision is validated, a later upstream revision is a new adoption boundary rather than an append-only continuation of the already-validated state.

### 9.4 Dependabot policy

Dependabot is discovery and proposal automation, not an acceptance authority.

For normal version updates:

- Dependabot targets `dev`, not `main`.
- Minor and patch updates may be grouped per package ecosystem to reduce pull-request churn.
- Major updates remain separate so breaking-change review, migration notes, and rollback remain attributable.
- Dependency pull requests are not auto-merged by default. Merge only after the applicable relevance/maturity review and CI/Canary/device validation required by this section.

Security updates are higher priority and may follow GitHub's default-branch security-update behavior. When a security update lands on `main`, reconcile the equivalent update into `dev` before the next normal promotion.

Do not keep obsolete Dependabot pull requests open merely because they were generated automatically. Close superseded, irrelevant, or intentionally deferred proposals with the reason recorded where useful.

## 10. Definition of done and change record

Review only what is relevant to the change; do not create artificial N/A-heavy process.

For applicable runtime/engineering work, verify:

- no duplicate hooks/observers/listeners/jobs/state sources;
- each live property still has a clear writer;
- host/session references respect lifecycle;
- owned resources have cleanup/replacement paths;
- hot paths avoid unnecessary polling/reflection/tree traversal/wakeups/high-frequency logging;
- geometry/animation ownership did not move accidentally;
- fallback behavior remains safe;
- compatibility profiles/dependency notices remain accurate;
- user-facing copy and MIUIX behavior were reviewed when changed;
- applicable CI/local/device validation passed;
- changelog reflects the durable final outcome when required.

A successful build alone is not runtime proof.

A completed non-trivial change should leave a concise record answering:

1. What changed and why?
2. What was intentionally left unchanged?
3. How was it validated?
4. What limitation or follow-up remains?

For runtime-sensitive/architectural work, also record the relevant owner/call chain, lifecycle/cleanup impact, fallback behavior, meaningful evidence, and required device scenarios.

Use the PR template when a PR exists. Mechanical maintenance may use a short commit description as long as its non-behavioral nature and branch synchronization are clear.
