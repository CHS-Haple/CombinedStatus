# Contributing to CombinedStatus

This document defines the contribution and engineering rules for **developers and contributors** working on CombinedStatus. Requirements are applied in proportion to the change: runtime-sensitive work needs deeper ownership and device validation, while documentation-only or mechanical changes use a lighter review path.

## 1. Scope and normative language

These rules apply to changes involving application code, SystemUI hooks, runtime state, compatibility logic, diagnostics, UI, resources, build logic, CI, documentation that changes engineering behavior, and migration work.

Normative terms are used deliberately:

- **MUST / MUST NOT**: required. A change that violates the rule is not acceptable unless an exception is explicitly justified by verified evidence.
- **SHOULD / SHOULD NOT**: the default. A different approach is allowed only when the contributor can explain why it is safer or more appropriate.
- **MAY**: optional.

When an exception to a MUST-level architectural rule is genuinely required, the change record or pull request MUST explain the evidence, affected lifecycle, rollback boundary, compatibility risk, and required validation.

The application ID and package namespace are `com.chaners.combinedstatus`. Changing that identity requires an explicit compatibility and migration plan.

### 1.1 Contribution licensing

CombinedStatus is licensed under the [Apache License 2.0](LICENSE). Unless explicitly stated otherwise, any contribution intentionally submitted for inclusion in CombinedStatus is provided under the same license, without additional terms or conditions.

Contributors MUST only submit material they have the right to license to the project. Third-party code, assets, or derived material MUST retain any attribution, notice, and license obligations required by their upstream source.

## 2. Project principles

Every change MUST preserve three project qualities.

### 2.1 Standardized

Follow Android, HyperOS, MIUIX, and Modern Xposed conventions. Keep lifecycle, ownership, state flow, compatibility boundaries, and platform responsibilities explicit.

### 2.2 Lightweight

"Lightweight" means minimizing unnecessary runtime work and architectural redundancy, not merely reducing APK size.

Avoid unnecessary:

- polling;
- duplicated state;
- duplicate hooks or listeners;
- resident Root processes;
- background services;
- repeated View-tree traversal;
- per-frame logging;
- reflection on hot paths;
- wakeups or callbacks that occur more frequently than the underlying state can meaningfully change.

### 2.3 Modern

Prefer maintained platform and library APIs over custom infrastructure when they satisfy the requirement.

Repository build files and compatibility profiles are the source of truth for pinned versions. Architectural expectations include:

- Modern Xposed APIs for module integration;
- the currently pinned MIUIX build for the companion app;
- Android LocaleManager for per-app language;
- DataStore only for real persistent preferences;
- current Android navigation and predictive-back APIs.

A newer mechanism is not automatically a better one. Adoption still MUST satisfy lifecycle, stability, compatibility, and maintenance requirements.

## 3. Change workflow

### 3.1 Evaluate before editing

Before changing code or behavior, confirm the problem or requirement, the available evidence, and the likely cause.

Prefer the simplest safe solution that:

- addresses the cause rather than only the symptom;
- respects lifecycle and ownership;
- uses native or maintained APIs where practical;
- avoids duplicate state, special cases, and unnecessary runtime cost;
- does not create avoidable compatibility debt.

Do not patch speculative behavior merely because it is easy to change.

### 3.2 Investigate in root-cause order

For defects, regressions, incompatibilities, or unexpected behavior, investigate in this order:

1. **Find the root cause.** Identify the owner, state source, lifecycle transition, API contract, layout rule, or integration point that produces the behavior.
2. **Fix the source when practical.** Prefer correcting the responsible lifecycle, ownership boundary, state source, or contract violation when it can be done safely.
3. **Check existing rules and authoritative guidance.** Review this project's rules and architecture, the documented contracts of libraries and APIs actually in use, and relevant official Android, HyperOS, Modern Xposed, MIUIX, or other upstream documentation and maintainer guidance.
4. **Compare established practice.** If authoritative sources do not determine the solution, review well-understood patterns for the same class of lifecycle, UI, compatibility, performance, or integration problem and compare their trade-offs.
5. **Patch last.** Use a workaround only when the root cause cannot currently be corrected safely and no better authoritative or established solution is viable.

A workaround MUST be narrow, conditionally activated, and removable. Record why a direct fix is not currently viable, when the workaround applies, when it can be removed, and how it is validated.

### 3.3 Define the change boundary proportionally

Every change MUST have a clear boundary; planning depth should match risk.

For documentation-only, metadata-only, or mechanical changes, identifying the affected files and applicable validation is normally sufficient.

For runtime-sensitive, architectural, compatibility, build/release, or user-visible behavior changes, identify as applicable:

- the relevant owner/call chain and affected layer;
- what is intentionally left unchanged;
- lifecycle, state, and compatibility assumptions;
- build-channel impact;
- diagnostics and rollback/fallback boundary;
- CI and real-device validation.

Use the smallest change that can solve the verified problem.

### 3.4 Validate assumptions

Before implementation, confirm that critical steps do not depend on unverified assumptions, unrelated cleanup is excluded, diagnostics are bounded, rollback/fallback is defined, and runtime-sensitive claims have a real-device validation path.

### 3.5 Stay inside the boundary

Implementation MUST NOT silently expand into unrelated cleanup, speculative fixes, or additional feature work.

Read-only investigation such as code inspection, logs, CI status, APK/source analysis, and runtime evidence review may proceed without redefining the mutation boundary.

### 3.6 Stop when evidence changes the problem

If new evidence invalidates the assumed owner, call chain, scope, or platform behavior, stop at that boundary. Record the new evidence, identify what remains untouched, and revise the plan before continuing.

Do not stack another workaround on top of an invalid assumption.

### 3.7 Reassess after meaningful diagnostics

New logs, recordings, crash traces, geometry snapshots, or other evidence MUST reopen the solution choice. Re-run the root-cause order in §3.2 instead of merely tuning the current implementation.

Previous engineering effort is not evidence that the current path remains correct.

### 3.8 Preserve diagnostic isolation

When several hypotheses remain plausible, prefer single-variable A/B builds.

Several fixes MAY share one test build only when their owners, diagnostics, acceptance criteria, and rollback paths remain independently interpretable.

A test build MUST NOT recreate the question: "which change caused this result?"

## 4. Runtime ownership and lifecycle

### 4.1 Every long-lived runtime object MUST have one explicit owner

This applies to:

- SystemUI host references;
- CombinedStatus Views;
- host/render sessions;
- state observers;
- listeners and callbacks;
- hook handles;
- module-owned animations;
- temporary transition state;
- host-derived geometry caches.

For each long-lived object, contributors MUST be able to answer:

1. who creates it;
2. who owns it;
3. when it becomes invalid;
4. who disposes it;
5. what happens when its SystemUI host or module generation is replaced.

If these questions cannot be answered, the object is not ready to become part of the runtime architecture.

A global singleton MUST NOT implicitly become the lifetime owner of SystemUI Views or host-specific state.

### 4.2 Host-scoped state MUST remain host-scoped

Prefer:

`Host -> HostSession -> owned resources`

over:

`global module -> multiple hosts -> shared mutable state`

State derived from a specific SystemUI host MUST NOT silently become global runtime state.

When multiple hosts exist for normal status bar, keyguard, AOD, Control Center, transition/fake hosts, or future platform variants, each host that requires independent behavior SHOULD have an explicit identity and lifecycle boundary.

A stale host-specific View, geometry snapshot, listener, or transition state MUST NOT be reused after host replacement.

### 4.3 Every resource-owning session MUST have a disposal path

Creating a session creates an obligation to dispose it.

A session that owns runtime resources MUST provide an explicit cleanup path equivalent to `close()` or `dispose()`.

Cleanup MUST cover every resource owned by that session where applicable:

- unregister observers;
- remove listeners;
- cancel pending callbacks;
- cancel module-owned animations;
- release hook/session handles;
- invalidate host-specific caches;
- clear host/View references;
- discard temporary transition state.

SystemUI recreation, host replacement, and Modern Xposed hot reload MUST NOT leave the previous generation active.

Resource creation without a verified cleanup path is incomplete implementation.

### 4.4 One live property SHOULD have one runtime writer

Before modifying a live SystemUI property, identify its current writer.

Treat these as ownership-sensitive:

- measured width;
- layout width;
- position;
- `translationX` / `translationY`;
- alpha;
- visibility;
- tint;
- animation state;
- parent/child attachment.

Contributors MUST NOT introduce a second writer merely to counteract the result of the first writer.

If SystemUI already owns a property, prefer observing or deriving from it.

If CombinedStatus must become the writer, the ownership transfer MUST be deliberate, narrow, documented, reversible, and validated across the affected lifecycle.

Two independent writers controlling the same property are an **ownership conflict**, not an animation-tuning problem.

## 5. Architecture boundaries

### 5.1 Keep acquisition, state, presentation, and rendering separate

Runtime behavior SHOULD follow this conceptual flow:

`native/event source -> domain state -> scene/presentation policy -> renderer`

A state source reports facts. It SHOULD NOT decide layout.

A scene or presentation policy decides whether and how the feature should appear. It SHOULD NOT acquire unrelated native state.

A renderer consumes resolved presentation state. It MUST NOT become a second SystemUI state repository.

Avoid callbacks that mix state acquisition, lifecycle decisions, layout writes, and drawing in one execution path.

### 5.2 Prefer authoritative native state

When HyperOS exposes a reliable authoritative state or event, prefer observing it rather than maintaining a parallel model.

Any duplicate or fallback state source MUST define:

- when it is active;
- which source has authority;
- how disagreement is resolved;
- when fallback state is discarded.

Two equivalent sources MUST NOT update the same domain state without defined priority.

### 5.3 Observation does not grant ownership

A hook, reflection lookup, View-tree probe, runtime trace, or geometry sample MAY be used to understand SystemUI without granting CombinedStatus control of that behavior.

The normal progression is:

`observe -> identify owner -> understand contract -> choose integration point -> modify only if necessary`

Do not turn a diagnostic observation into a production writer without separately justifying ownership.

### 5.4 Hooks are integration points, not architecture

A hook MUST have:

- one specific responsibility;
- an owning subsystem;
- a lifecycle boundary;
- a hot-reload/cleanup strategy where applicable;
- defined failure behavior.

The runtime architecture MUST NOT degrade into a growing collection of unrelated hook callbacks.

Hooks SHOULD feed owned state sources, sessions, or integration components.

### 5.5 CombinedStatusModule is a bootstrap/integration boundary

`CombinedStatusModule` MUST NOT become the permanent owner of every long-lived runtime concern.

It MAY coordinate bootstrap, compatibility checks, and top-level hook installation, but long-lived domain/state/host ownership SHOULD live in dedicated runtime components.

Do not add permanent responsibilities to `CombinedStatusModule` merely because it is the convenient place where a hook is installed.

## 6. Ownership boundary

`CombinedStatusModule` is an integration boundary, not a catch-all lifetime owner. A change MUST NOT add another persistent host, state, observer, or hook responsibility there when that responsibility has a clear dedicated owner.

When ownership needs to move:

- move one bounded responsibility at a time;
- keep one active owner for each responsibility;
- old and new owners MUST NOT run concurrently except in an explicit A/B diagnostic;
- do not combine ownership movement with unrelated visual redesign or feature work;
- keep each step independently testable and reversible;
- once ownership has moved, later code MUST use the dedicated owner rather than reintroducing the old path.

Current ownership status belongs in architecture documentation or code, not in a permanent ordered migration list in this contributor guide.

## 7. SystemUI geometry and visual ownership

Preserve native HyperOS geometry ownership wherever practical.

### 7.1 Separate geometry responsibilities

The following MUST remain conceptually independent:

1. native SystemUI layout slot;
2. CombinedStatus visual/drawing geometry;
3. transition/animation geometry;
4. optical adjustment.

A visual-width requirement MUST NOT automatically change native layout width.

An animation correction MUST NOT automatically change stable-state geometry.

An optical offset MUST NOT silently become layout ownership.

Do not use one hard-coded width or translation value to satisfy several unrelated responsibilities.

### 7.2 Native geometry writes are exceptional

Do not modify native `measuredWidth`, layout width, translation, or visibility as the first-choice solution.

Prefer solving CombinedStatus-specific appearance inside its own drawing/presentation layer.

An unavoidable native geometry change MUST be:

- supported by verified runtime evidence;
- owned by one clearly identified component;
- limited to the narrowest state and lifecycle;
- reversible;
- documented;
- covered by focused real-device testing.

### 7.3 Pixel correctness is not sufficient

A visually correct result on one device state does not prove architectural correctness.

For runtime-sensitive geometry or animation fixes, verify that:

- the expected owner performed the write;
- no competing writer counteracted it;
- stable-state and transition-state geometry remain separate;
- host replacement does not leave stale state;
- relevant charging, keyguard, AOD, Control Center, island, and recreation paths still behave correctly.

Accidental compensation between incorrect writers MUST NOT be accepted as a stable fix.

## 8. Failure and fallback behavior

### 8.1 Fail native, not broken

SystemUI integration SHOULD degrade toward native HyperOS behavior.

If CombinedStatus cannot safely establish the required host, compatibility, lifecycle, or state contract, prefer:

`CombinedStatus unavailable -> native status representation remains/restores`

rather than:

`CombinedStatus partially active -> native representation hidden -> broken or missing status`

Native-icon suppression and replacement activation MUST be coordinated.

Do not hide a native representation until the replacement is valid for the current session.

Compatibility failure SHOULD disable the smallest affected feature rather than destabilize SystemUI.

### 8.2 Historical fixes are evidence, not reusable architecture

Previous CombinedStatus builds, successful constants, and old patches MAY be used to understand observed SystemUI behavior.

They MUST NOT be reintroduced automatically.

Before reusing a historical fix, determine:

- what real behavior it compensated for;
- which runtime owner produced that behavior;
- whether that owner still exists;
- whether the current architecture already addresses the cause;
- whether the old fix would violate current lifecycle or geometry ownership.

Preserve the verified requirement, not the historical implementation.

## 9. SystemUI and compatibility rules

### 9.1 Verified integration points

Prefer exact verified hook points over broad reflection.

A class or member existing in an APK does not prove that it owns live behavior. Use runtime evidence when hierarchy, ownership, or transitions matter.

Hooks MUST be based on verified members from the exact target APKs and represented in the pinned compatibility profile where appropriate.

Current baseline:

- HyperOS SystemUI `17.03.260226.r`

Do not expand module scope to unrelated packages without a verified runtime dependency.

### 9.2 References and host retention

Reference strength MUST follow ownership and lifecycle. Non-owning observers SHOULD avoid extending the lifetime of SystemUI hosts or Views; an owning session MAY hold a strong reference only for the session's valid lifetime.

Never retain an Activity, Context, or View beyond its valid lifecycle, and clear owned references when the owning session is disposed or replaced.

### 9.3 Modern Xposed

Use Modern Xposed API 102.

Keep one Java entry unless a verified API requirement changes that decision.

Hot reload MUST replace or migrate hook/session ownership rather than stacking duplicate generations.

CI success does not prove runtime hook correctness or hot reload correctness.

### 9.4 Runtime structure is authoritative

Other modules or HyperOS variants may alter the live SystemUI tree.

Diagnostics SHOULD inspect actual runtime topology when parentage, identity, ownership, or transition behavior matters instead of assuming the stock hierarchy.

## 10. Diagnostics, performance, and energy

### 10.1 Diagnostics policy

Build channels MUST NOT silently change core user-facing feature semantics. Channel differences SHOULD be limited to diagnostics, development probes, optimization, signing, or other explicitly documented build behavior.

**Release** may retain only low-frequency operational diagnostics where enabled, such as:

- build identity;
- compatibility readiness;
- essential lifecycle/hook readiness;
- hot reload result;
- important errors.

**Debug** may additionally provide bounded:

- topology and parent/index/path snapshots;
- bounds and measured geometry;
- translation;
- hook lifecycle;
- state snapshots;
- ownership probes.

Prefer:

`event -> bounded snapshot -> report`

over polling or continuous sampling.

Diagnostics MUST NOT become a second runtime workload that materially changes the behavior being measured.

### 10.2 Runtime cost

Before adding a hook, observer, listener, coroutine, callback, reflection path, shell call, or View-tree probe, ask whether it wakes more often than the underlying state can meaningfully change.

Prefer:

- native callbacks;
- one-shot inspection;
- cached immutable metadata;
- shared helpers;
- scoped coroutines;
- bounded user-triggered Root work.

Root work SHOULD be user-triggered where practical and MUST be bounded by timeout/failure handling.

## 11. Application, UI, and text rules

### 11.1 App architecture

Persist only real user preferences.

Do not confuse UI preview state with module runtime state.

Prefer platform APIs over custom infrastructure when the platform already provides the required lifecycle and behavior.

### 11.2 MIUIX UI review

Whenever UI changes, contributors MUST review the change against the currently pinned MIUIX version and its supported components.

Prefer official MIUIX components and defaults for spacing, typography, shape, pressed state, disabled state, dialogs, and navigation.

Check:

- information hierarchy and section grouping;
- Card/list-item semantics;
- pressed, enabled, and disabled states;
- light/dark/dynamic themes;
- system/predictive back;
- transitions;
- accessibility labels;
- localized text expansion;
- dialog action hierarchy.

Do not hand-tune values merely to imitate MIUIX when an official component already provides the behavior.

### 11.3 Text review

Any user-facing text change MUST receive a copy review.

Fixed terminology:

- Chinese: **移动网络**
- English: **mobile network**
- Internal domain naming: `mobileNetwork` / `mobileSignal`

Keep exact upstream Android/HyperOS class, field, method, and resource identifiers unchanged.

Normal settings UI SHOULD NOT expose internal terms such as host, role, writer, hook chain, probe path, or implementation class name unless the screen is explicitly diagnostic.

Public-facing documentation SHOULD describe the project's current behavior, compatibility, architecture, and user-facing limitations. Development lineage, superseded implementation history, and unrelated projects SHOULD be omitted. Actual project dependencies, bundled third-party code or assets, and any required license or attribution notices MUST be documented accurately in the appropriate project metadata or third-party notices.

## 12. Branching, versioning, CI, and release discipline

### 12.1 Branches

- `main`: stable, installable, validated baseline.
- `dev`: ongoing module integration and the normal pull-request target.
- `feat/*`: larger isolated experiments that will return to `dev` after validation.

External and routine contributions SHOULD target `dev`. `main` is reserved for validated promotions and exceptional maintenance work.

Do not promote SystemUI work to `main` until the bounded change is complete, required CI is green, relevant diagnostics show no unresolved blocker, and required real-device validation has passed.

Pull-request CI MUST remain safe for untrusted forks:

- PR validation must not require repository signing secrets;
- tests, compatibility checks, metadata checks, and unsigned build validation may run on PRs;
- project-signed Debug/Canary artifacts remain a maintainer push responsibility;
- contributors must never request, expose, reproduce, or bypass project signing credentials.

### 12.2 Commits

Keep commits atomic and semantic.

Do not mix unrelated architecture migration, feature work, visual redesign, and cleanup into one change merely for convenience.

### 12.3 Versioning

The display version changes only when the formal external version is explicitly advanced.

Normal APK-affecting iterations advance the internal `versionCode` / `buildId`.

Documentation-only changes do not require an APK build-number bump.

GitHub Actions run numbers are CI execution metadata and MUST NOT be used as application version identifiers.

### 12.4 Changelog discipline

`CHANGELOG.md` records **net project changes at a release boundary**, not commit-by-commit development history.

Its purpose is to let a user or contributor answer:

- what capability exists now that did not exist before;
- what existing behavior now works differently;
- what defect is now fixed;
- what relevant mechanism has been removed;
- what engineering constraint materially affects future development.

A changelog entry SHOULD answer **what is now different**, not **how many attempts were made**.

#### 12.4.1 Release boundary

During normal development before the first formal release, `[Unreleased]` MUST describe the current net state intended for the initial release.

A dated/versioned section MUST NOT be created early merely to represent an intended future release. It is created only in the final release-preparation commit, immediately before the stable release workflow is run.

For a formal release:

1. freeze the applicable `[Unreleased]` net changes into `## [<version>] - YYYY-MM-DD` in the final release-preparation commit;
2. leave a fresh `## [Unreleased]` section for subsequent development;
3. run the stable release workflow from that prepared `main` commit;
4. do not continue unrelated development between the release-preparation commit and publication.

#### 12.4.2 Categories

Use the smallest category that accurately describes the final change:

- **Added** — a capability, supported behavior, user-facing option, diagnostic facility, build channel, or integration that did not previously exist and remains present.
- **Changed** — an existing capability now behaves, integrates, or presents differently.
- **Fixed** — a defect or incorrect behavior is now corrected.
- **Removed** — a previously present capability or a materially important experimental mechanism has been intentionally removed.
- **Engineering** — a non-user-facing architectural, lifecycle, compatibility, build, or contributor constraint that materially changes how future work must be implemented.

Do not use **Engineering** as a dumping ground for implementation details. If a change has no durable effect on users or future contributors, it belongs in Git history rather than the changelog.

#### 12.4.3 What to include

Contributors MUST include a changelog entry when a change materially affects at least one of:

- user-visible behavior or settings;
- supported runtime behavior;
- compatibility or fallback behavior;
- diagnostics that contributors rely on;
- build/release channels or signing behavior;
- persistent architecture or lifecycle ownership;
- contributor rules that change how future code must be written;
- removal of a mechanism whose absence is important to understanding the current architecture.

#### 12.4.4 What to omit

Contributors MUST NOT use the changelog as a development diary.

Normally omit:

- CI run/build numbers;
- temporary diagnostic probes;
- intermediate UI iterations;
- trial constants or offsets;
- failed hypotheses;
- one-off instrumentation;
- implementation paths that were later replaced;
- internal cleanup with no durable behavior or architecture effect;
- repeated entries for the same final outcome.

Keep detailed investigation history in commits, pull requests, diagnostics, issue discussions, or dedicated development documentation.

A superseded experiment SHOULD be collapsed into the final outcome. Mention its removal only when future contributors need to know that the approach was deliberately rejected.

#### 12.4.5 Entry style

Each bullet SHOULD describe one final change unit using:

`action + object + final effect/reason when needed`

Prefer concise, present-state wording:

- Good: `Fixed CombinedStatus disappearing during native Wi-Fi/mobile transitions.`
- Good: `Changed airplane-mode tracking to use the authoritative global setting.`
- Good: `Removed the experimental owned-slot padding mutation after it was shown to alter native geometry.`
- Avoid: `Build 93 added a probe, then Build 94 moved it, and later Build 95 removed it.`

Do not write chronological narratives inside a bullet.

Avoid internal class names, field names, CI identifiers, and low-level implementation detail unless they are necessary to understand compatibility or architecture.

One bullet SHOULD represent one durable outcome. If a sentence contains several unrelated changes joined by `and`, split it or keep only the release-relevant result.

#### 12.4.6 Final review

Before completing a functional change, ask:

1. If someone installs only the final APK and never reads the commit history, is this entry still true and useful?
2. Is the entry describing a result rather than the investigation process?
3. Has an earlier entry already been superseded by this result?
4. Is the selected category still correct?
5. Could the wording be shorter without losing the durable effect?

If the answer to the first two questions is no, the change normally does not belong in `CHANGELOG.md`.

### 12.5 Release publication

Formal stable releases MUST satisfy all of the following:

- publish from `main` only;
- the intended display version is already present in project version metadata;
- `CHANGELOG.md` contains a dated `## [<version>] - YYYY-MM-DD` section;
- the release notes describe that version's net changes rather than copying development history;
- README release-status wording is updated when the first stable release or another user-visible release state changes;
- the stable tag does not already exist;
- the Release workflow builds and verifies the signed APK from the selected `main` commit.

The Release workflow MUST fail closed when the branch or changelog release boundary is not ready.

Test/prerelease tags MAY include CI execution identity. Stable version tags and distributable APK filenames MUST use application release/build identity rather than GitHub Actions run numbers.

### 12.6 Local verification

Use the checked-in Gradle Wrapper as the canonical Gradle entry point.

Minimum local verification for ordinary code changes:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

When a change affects Release/Canary build behavior, signing-independent configuration, shrinking, resources, or Xposed metadata, also run the applicable non-secret build/check locally where possible. Maintainer signing credentials are never required for an external contributor to validate source changes.

Do not commit local SDK paths, signing material, generated APK/AAB files, or environment-specific Gradle configuration.

### 12.7 CI and device validation

CI verifies a source state; real-device testing verifies runtime behavior. Neither replaces the other.

Use CI by purpose:

- **Pull-request CI** runs secret-independent checks for tests, buildability, compatibility, metadata, and other safe validation.
- **Trusted pushes to `dev` or `main`** may additionally build and verify project-signed Debug/Canary artifacts.
- **Release CI** is reserved for deliberate test or stable publication, not ordinary development builds.
- CI and local builds MUST use the checked-in Gradle Wrapper.

For APK-affecting work, verify the checks relevant to the change, including compatibility profiles, tests, required build variants, Modern Xposed metadata, signing where applicable, diagnostics boundaries, and artifact generation.

A failed CI run MUST be understood before it is retried:

- fix deterministic code, configuration, dependency, metadata, or signing failures and validate the resulting commit with a new run;
- rerun the same commit only when there is reasonable evidence of a transient runner, network, package-hosting, or upstream-service failure;
- if the cause is unclear, inspect the logs or reproduce the failure before retrying;
- repeated reruns MUST NOT be used to obtain a green result from an unresolved deterministic failure.

A green CI result means only that the checks performed by that workflow passed. It does not prove SystemUI runtime correctness, lifecycle correctness, UI behavior, or device compatibility.

Before merging, required checks for the target branch MUST pass. Runtime-sensitive changes MUST also complete focused real-device validation for the affected lifecycle and scenes. Work still awaiting required device validation MUST remain marked as such and MUST NOT be promoted to `main`.

Create a new CI build when the source, configuration, diagnostics, or validation target has meaningfully changed. Do not create commits or builds solely to obtain another CI/run number.

### 12.8 Upstream dependency adoption

Dependency updates MUST be evaluated by relevance and maturity rather than adopted merely because a newer commit exists.

#### 12.8.1 Relevance classes

Classify a meaningful upstream change before adoption:

- **A — priority**: directly fixes a current or likely CombinedStatus problem, removes a project workaround, addresses a crash/lifecycle/state issue, or carries important maintainer guidance for an API or component the project uses.
- **B — canary candidate**: provides a clear interaction, stability, performance, compatibility, or maintainability improvement worth isolated validation.
- **C — normally ignore**: dependency-only churn, docs/example-only changes, unrelated platform changes, or changes to components CombinedStatus does not use.

For MIUIX, pay particular attention to pager/gesture handling, navigation, Preference, dialogs, horizontally draggable controls, floating navigation, Blur/backdrop, theme behavior, Android lifecycle/state restoration, performance, and maintainer warnings.

#### 12.8.2 Maturity levels

Treat upstream maturity explicitly:

1. **open/experimental PR** — investigation only; MUST NOT be adopted as a project dependency solely because its CI is green;
2. **merged main/canary** — MAY be considered when an exact revision is published and passes the project adoption gate;
3. **stable release** — preferred when it already contains the required fix or feature.

Maintainer warnings such as “do not use yet”, known regressions, incomplete follow-up fixes, or missing artifacts override apparent freshness and MUST block adoption until resolved.

#### 12.8.3 Exact-revision gate

For an upstream main snapshot or canary dependency, CombinedStatus MUST pin one exact revision and verify the evidence for that same revision.

For MIUIX main snapshots, all of the following MUST be true before adoption:

- upstream **Build All Tests** succeeds;
- upstream **Build Example App** succeeds;
- upstream **Publish to GitHub Packages** succeeds;
- the commit-specific SNAPSHOT for that exact revision is actually resolvable;
- all MIUIX modules used by the app resolve to the same revision;
- credentials remain outside the repository.

Equivalent test/build/publish evidence SHOULD be required for other dependencies when upstream provides it.

Do not substitute an unpublished head for the newest fully green published revision.

#### 12.8.4 Adoption workflow

Use a short-lived branch such as `feat/<dependency>-<revision>` and target `dev`.

The normal sequence is:

1. inspect the upstream PR/issue/maintainer discussion and classify the change A/B/C;
2. identify the exact merged revision and confirm its maturity/evidence;
3. centralize the dependency version/revision where practical;
4. update all related modules atomically so mixed revisions are not introduced;
5. update repository-facing dependency references and notices when applicable;
6. run CI and a dependency-only Canary first when the update can affect runtime/UI behavior;
7. if the new upstream version introduces a recommended API path, integrate that behavior as a separate bounded change or second Canary when practical;
8. perform focused real-device validation for runtime-sensitive behavior;
9. review the final diff for unrelated changes;
10. squash/merge the validated result into `dev`, verify post-merge CI, and delete the short-lived branch.

Do not combine a dependency update with unrelated UI redesign, architecture migration, or feature work merely because the newer dependency makes those changes possible.

A dependency-only validation and a behavior/API-integration validation SHOULD remain distinguishable when that separation materially improves fault isolation.

Once a dependency-update branch has been validated, later upstream revisions MUST be handled as a separate update rather than appended to that already-validated branch.

## 13. Post-change review and definition of done

Review only the checks that are relevant to the change. Runtime-sensitive work requires the deeper lifecycle and ownership checks; documentation-only or mechanical changes do not need artificial N/A-heavy reporting.

For applicable changes, verify:

- no duplicate hooks, observers, listeners, jobs, or equivalent state sources were introduced;
- live properties still have a clear writer;
- host/session references do not outlive their lifecycle;
- resource-owning sessions have cleanup or replacement paths;
- hot paths avoid repeated reflection, View-tree traversal, polling, unnecessary wakeups, or high-frequency logging;
- native geometry/animation ownership has not moved unintentionally;
- build-channel differences are intentional and documented;
- compatibility profiles and dependency notices remain accurate;
- user-facing copy and MIUIX behavior were reviewed when changed;
- CI/local checks passed where applicable;
- required real-device validation passed, or the change is explicitly marked **awaiting device validation**;
- `CHANGELOG.md` is updated when the final change is user-visible or materially affects future engineering constraints.

A successful build alone is not evidence of runtime correctness.

## 14. Change report

Every completed change SHOULD leave a concise record that answers:

1. **What changed and why?**
2. **What was intentionally left unchanged?**
3. **How was it validated?**
4. **What limitations or follow-up remain?**

For runtime-sensitive or architectural changes, also record the relevant owner/call chain, lifecycle or cleanup impact, fallback behavior, meaningful diagnostic evidence, and required real-device test scenarios.

The repository pull-request template is the preferred format when a pull request is used. Small documentation-only or mechanical changes may use a shorter commit/merge description as long as the applicable validation remains clear.
