# Contributing to CombinedStatus

This document defines the engineering rules for **developers and contributors** working on CombinedStatus. These rules are part of the implementation contract, not optional cleanup guidance.

## 1. Scope and normative language

These rules apply to changes involving application code, SystemUI hooks, runtime state, compatibility logic, diagnostics, UI, resources, build logic, CI, documentation that changes engineering behavior, and migration work.

Normative terms are used deliberately:

- **MUST / MUST NOT**: required. A change that violates the rule is not acceptable unless an exception is explicitly justified by verified evidence.
- **SHOULD / SHOULD NOT**: the default. A different approach is allowed only when the contributor can explain why it is safer or more appropriate.
- **MAY**: optional.

When an exception to a MUST-level architectural rule is genuinely required, the implementation report MUST state the evidence, affected lifecycle, rollback boundary, compatibility risk, and real-device validation required.

Package identity is always `com.chaners.combinedstatus`.

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

Current project baselines include:

- Modern Xposed API 102;
- MIUIX 0.9.4;
- Android LocaleManager for per-app language;
- DataStore only for real persistent preferences;
- current Android navigation and predictive-back APIs.

Modern does not mean adopting a newer mechanism merely because it exists. It MUST still satisfy lifecycle, stability, compatibility, and maintenance requirements.

## 3. Mandatory change workflow

### 3.1 Evaluate before modifying

A request, idea, reference implementation, or proposed fix is a **candidate direction**, not automatic permission to mutate the project.

Before changing project state, contributors MUST evaluate:

- whether the problem is real and sufficiently understood;
- whether the proposed mechanism addresses the verified cause rather than only the symptom;
- whether the change is necessary now;
- whether a simpler, more native, or lower-risk solution exists;
- whether lifecycle and ownership remain explicit;
- whether runtime cost is justified;
- whether the change creates duplicated state, special cases, or compatibility debt;
- whether the available evidence is sufficient.

Classify the direction as:

- **accept**;
- **accept with adjustments**;
- **defer for evidence**;
- **reject**.

Do not implement a weak or speculative direction merely because it was requested or previously attempted.

### 3.2 Define the change boundary

For an accepted functional change, the contributor MUST define a bounded plan before editing.

The plan MUST identify:

- the verified runtime owner and relevant call chain;
- the subsystem/layer expected to change;
- the subsystem/layer explicitly expected not to change;
- lifecycle and state dependencies;
- compatibility assumptions;
- Debug/Release impact;
- diagnostics required to validate uncertain runtime behavior;
- rollback boundary;
- CI checks;
- real-device scenarios required after implementation.

The plan SHOULD be the smallest change that can solve the verified problem.

### 3.3 Validate the plan

Before implementation, confirm that:

- no critical step depends on an unverified assumption;
- the order respects dependency and lifecycle boundaries;
- unrelated cleanup is not mixed into the change;
- diagnostics are bounded and event-driven;
- rollback leaves the last validated baseline intact;
- runtime-sensitive claims have a real-device verification path.

### 3.4 Stay inside the confirmed boundary

Implementation MUST NOT silently broaden into unrelated cleanup, speculative fixes, or additional feature work.

Read-only investigation such as code inspection, logs, CI status, APK/source analysis, and runtime evidence review may proceed without a new mutation plan.

### 3.5 Stop when evidence invalidates the plan

If implementation reveals that the real owner, call chain, required scope, or platform behavior differs materially from the confirmed plan, stop mutation at that boundary.

Do not stack another workaround.

The contributor MUST:

1. state which assumption was invalidated;
2. record the new evidence;
3. identify what remains untouched;
4. reassess the available solution families;
5. define a revised bounded plan before resuming mutation.

### 3.6 Reassess the solution space after meaningful diagnostics

Logs, recordings, crash traces, geometry snapshots, and other diagnostics are not only used to decide whether the current patch worked.

After meaningful new evidence, contributors MUST reconsider:

- native/platform mechanisms;
- a fix within the current architecture;
- an alternative integration point;
- compatibility or fallback handling;
- a bounded workaround;
- a justified redesign.

Previous engineering effort is not evidence that the current path remains correct.

Choose the best solution supported by current evidence, not the next patch on the existing path.

### 3.7 Preserve diagnostic isolation

When several hypotheses remain plausible, prefer single-variable A/B builds.

Multiple independently understood fixes MAY share one test build only when each retains:

- an explicit owner and affected layer;
- independent diagnostics where needed;
- separate acceptance criteria;
- a result that remains interpretable if another fix fails;
- an independent rollback path.

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

## 6. Ownership migration gate

Functional work may continue while the architecture remains within its intended boundaries. However, contributors MUST NOT indefinitely accumulate long-lived lifecycle responsibilities in `CombinedStatusModule`.

The migration gate is crossed when a new change would require adding another persistent host/state/observer/hook ownership responsibility to `CombinedStatusModule`, or when an existing ownership domain can no longer be understood or disposed independently.

At that point, complete the next bounded ownership migration before adding more persistent responsibility.

Current migration order:

1. **DefaultDataSubscription + Airplane + Connectivity ownership**
2. **Tint + Scene + MobileType ownership**
3. **Network hook ownership**
4. **Host + Hot Reload ownership**

Migration rules:

- migrate ownership, not unrelated behavior;
- keep one active owner for each responsibility;
- old and new owners MUST NOT run concurrently except in an explicit A/B diagnostic;
- do not combine ownership migration with unrelated visual redesign;
- each batch MUST remain independently testable and reversible;
- after a batch is complete, later code MUST use the new owner rather than reintroducing the old path.

This gate exists to prevent a gradual return to a monolithic module while allowing normal feature work to continue between migration points.

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

Before porting a historical fix, determine:

- what real behavior it compensated for;
- which runtime owner produced that behavior;
- whether that owner still exists;
- whether the new architecture already addresses the cause;
- whether the old fix would violate current lifecycle or geometry ownership.

Port the verified requirement, not the historical implementation.

### 8.3 Reference projects provide patterns, not authority

KeiMi and other SystemUI modules MAY be studied for:

- per-host state;
- lifecycle/session ownership;
- cleanup design;
- fallback behavior;
- sizing abstractions;
- integration points.

A mechanism MUST NOT be copied solely because a mature reference project uses it.

In particular, another module's `onMeasure`, `onLayout`, translation, visibility, or native-geometry hooks do not justify introducing the same ownership into CombinedStatus.

Adopt the architectural benefit while preserving CombinedStatus's more conservative native-geometry contract whenever possible.

## 9. SystemUI and compatibility rules

### 9.1 Verified integration points

Prefer exact verified hook points over broad reflection.

A class or member existing in an APK does not prove that it owns live behavior. Use runtime evidence when hierarchy, ownership, or transitions matter.

Hooks MUST be based on verified members from the exact target APKs and represented in the pinned compatibility profile where appropriate.

Current baseline:

- HyperOS SystemUI `17.03.260226.r`

Do not expand module scope to unrelated packages without a verified runtime dependency.

### 9.2 References and host retention

Use weak references for SystemUI hosts/Views unless a stronger reference is required by an explicit session ownership contract.

Never retain an Activity, Context, or View beyond its valid lifecycle.

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

Debug and Release MUST share core feature behavior. Diagnostics depth may differ.

**Release** may retain low-frequency operational diagnostics such as:

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

Whenever UI changes, contributors MUST perform a MIUIX 0.9.4 review.

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

## 12. Branching, versioning, CI, and release discipline

### 12.1 Branches

- `main`: stable, installable, validated baseline.
- `dev`: ongoing module integration.
- `feat/*`: larger isolated experiments that will return to `dev` after validation.

Do not promote SystemUI work to `main` until it is structurally complete, CI-green, diagnostics-clean, and has completed required real-device validation.

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

Until the first formal release, `[Unreleased]` MUST describe the current net state intended for the initial release.

Do not create a dated/versioned release section before that version is actually published.

After a formal release:

1. move the applicable net changes into `## [<version>] - YYYY-MM-DD`;
2. create a fresh `## [Unreleased]`;
3. record only changes made after that release boundary.

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
- refactors with no durable behavior or architecture effect;
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

### 12.5 CI and device validation

CI is a gate, not a replacement for runtime testing.

For APK-affecting work, verify as applicable:

- pinned compatibility profile;
- Debug build;
- Release build;
- Modern Xposed metadata;
- signing;
- Debug/Release diagnostics boundary;
- artifact generation.

For runtime-sensitive work, provide focused real-device scenarios based on the affected owner/lifecycle. These may include:

- normal status bar;
- charging/non-charging;
- lock screen;
- AOD;
- Control Center open/close;
- island transitions;
- SystemUI restart;
- hot reload;
- configuration changes;
- coexistence with other status-bar modules.

Do not claim runtime correctness from CI alone.

## 13. Post-change review

After implementation, contributors MUST review for:

- duplicate hooks, observers, listeners, jobs, or state;
- multiple writers for the same runtime property;
- stale host/session references;
- missing cleanup paths;
- repeated reflection or View-tree traversal;
- polling or unnecessary wakeups;
- high-frequency logs;
- unbounded main-thread work;
- geometry/animation ownership accidentally moved from SystemUI to the module;
- Debug-only behavior leaking into Release;
- compatibility profile drift;
- new long-lived responsibilities added to `CombinedStatusModule` without checking the ownership migration gate.

A successful build is not the definition of done.

## 14. Definition of done

A change is complete only when all applicable requirements are satisfied:

- direction evaluated before mutation;
- bounded plan defined and validated;
- verified owner/call chain identified for runtime-sensitive work;
- implementation stayed within the plan or stopped for re-evaluation;
- meaningful diagnostics were used to reassess the full solution space;
- lifecycle and ownership are explicit;
- resource cleanup/fallback is defined;
- no accidental multi-writer geometry/state ownership was introduced;
- standardized/lightweight/modern review completed;
- copy review completed when text changed;
- MIUIX review completed when UI changed;
- appropriate Debug/Release diagnostics are present;
- CI passed where applicable;
- required real-device validation passed or is explicitly marked **awaiting device validation**;
- CHANGELOG is updated for user-visible or engineering-significant changes;
- runtime claims are not based on CI alone.

## 15. Required implementation report

Every completed implementation report MUST state:

1. how the direction was evaluated and why it was accepted, adjusted, deferred, or rejected;
2. the confirmed change boundary and whether execution stayed inside it;
3. the verified owner/call chain for runtime-sensitive work;
4. any invalidated assumption or plan deviation and the evidence that caused it;
5. alternative solution families considered after meaningful diagnostics;
6. what changed and why;
7. ownership/lifecycle impact, including cleanup and fallback behavior where applicable;
8. whether the ownership migration gate was evaluated or crossed;
9. standardization/lightweight/modernization review result;
10. text-review result when text changed;
11. MIUIX UI-review result when UI changed;
12. CI result;
13. required real-device test scenarios and current result;
14. known limitations and compatibility boundaries.

These rules MUST be re-read and applied by developers and contributors for every future feature, fix, migration, and runtime-sensitive refactor.
