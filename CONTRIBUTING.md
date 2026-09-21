# Contributing to CombinedStatus

This file defines the engineering rules for CombinedStatus. These checks are part of every change, not optional cleanup.

## Project principles

Every change should improve the project in three directions:

- **Standardized**: follow Android, HyperOS, MIUIX, and Modern Xposed conventions; keep lifecycle, state, ownership, and compatibility boundaries explicit.
- **Lightweight**: avoid polling, duplicate listeners/state, unnecessary hooks, resident Root processes, background services, repeated View-tree scans, and high-frequency logs.
- **Modern**: prefer current Android APIs, Modern Xposed API 102, MIUIX 0.9.4, LocaleManager, predictive back/navigation APIs, and DataStore only where persistence is actually needed.

Package identity is always `com.chaners.combinedstatus`.

## Mandatory workflow for every functional change

### 1. Pre-change review

Before coding, inspect the full call chain and lifecycle.

Confirm:
- the owning process, class, host, and state source;
- who owns measurement, layout, translation, visibility, animation, and drawing;
- whether a new hook/listener is really necessary;
- whether the design is event-driven rather than polling;
- effects on SystemUI recreation, hot reload, lock screen, AOD, Control Center, charging, and configuration changes;
- Debug/Release differences;
- safe behavior when a target member is missing.

Do not patch a visual symptom before locating the real owner of the behavior.

### 2. Implementation rules

- Prefer exact verified hook points over broad reflection.
- Keep hook count as low as practical.
- Keep hook callbacks short and event-driven.
- Use weak references for SystemUI Views/hosts unless stronger ownership is clearly required.
- Never retain an Activity, Context, or View past its lifecycle.
- Avoid resident Root helpers and background services.
- Root work must be bounded, have timeouts, and be user-triggered where practical.
- Reuse native state instead of duplicating SystemUI state.
- Do not add redundant compatibility branches or hard-coded geometry without evidence.

### 3. Text review

Whenever user-facing text is added or changed, perform a copy pass before completion.

Chinese should use concise HyperOS/MIUIX-style titles and natural summaries. English should read as natural product English, not literal translation.

Fixed terminology:
- Chinese: **移动网络**
- English: **mobile network**
- Internal: `mobileNetwork` / `mobileSignal`

Keep exact upstream Android/HyperOS class, field, method, and resource identifiers unchanged.

Normal settings should not expose internal terms such as host, role, hook chain, writer, probe path, or implementation class names unless the screen is explicitly diagnostic.

### 4. UI review

Whenever UI changes, perform a MIUIX 0.9.4 review before completion.

Prefer official MIUIX components and defaults for spacing, typography, shape, pressed state, disabled state, dialogs, and navigation. Do not hand-tune values merely to imitate MIUIX when an official component already provides the behavior.

Check:
- page hierarchy and section grouping;
- Card/list-item semantics;
- pressed, enabled, and disabled states;
- light/dark/dynamic themes;
- system/predictive back;
- transitions;
- accessibility labels;
- localized text expansion;
- dialog primary/secondary action hierarchy.

UI polish is part of the feature.

### 5. Post-change review

Repeat the engineering review after implementation and check for:
- duplicate hooks/listeners/jobs/state;
- repeated reflection or View-tree traversal;
- per-frame/high-frequency logs;
- strong-reference leaks;
- unbounded main-thread work;
- unnecessary wakeups/background work;
- geometry/animation ownership accidentally moved from SystemUI to the module;
- Debug-only details leaking into Release;
- stale compatibility constants or target-profile drift.

Compiling successfully is not the definition of done.

## SystemUI integration rules

Preserve native HyperOS ownership wherever possible.

Do not modify native `measuredWidth`, layout width, `translationX`, `translationY`, or visibility as the first-choice solution. Prefer solving visual requirements inside the custom drawing layer.

Keep these three concepts separate:
1. native SystemUI layout slot;
2. CombinedStatus visual/drawing width;
3. transition/animation geometry.

Do not collapse them into a single hard-coded width/offset.

Any unavoidable native geometry change must be justified by verified runtime behavior, isolated to the narrowest state/lifecycle, documented, and real-device tested.

## Compatibility baseline

Hooks must be based on verified members from the exact target APKs. Keep compatibility facts in the pinned target profile and keep runtime probes aligned with it.

Current baseline:
- HyperOS SystemUI `17.03.260226.r`

The module scope, compatibility profile, runtime markers, and hook verification must be based on SystemUI only. Do not add unrelated HyperOS component packages to the module baseline unless a future feature has a verified runtime dependency on them.

A class existing in an APK does not prove it owns the live behavior. Use runtime evidence when hierarchy, ownership, or transitions matter.

Other modules may alter the live SystemUI tree. Debug diagnostics should inspect the actual runtime structure instead of assuming stock parent/container relationships.

## Modern Xposed rules

Use Modern Xposed API 102. Keep one Java entry unless a verified API requirement changes that decision.

Hot reload must replace/migrate hook handles instead of stacking duplicates. CI success does not prove runtime hooking or hot reload; confirm runtime-sensitive behavior on a real device.

When LibXposed documentation is temporarily unavailable:
- Gradle repository root: `https://repo.maven.apache.org/maven2`
- Direct LibXposed artifact path: `https://repo.maven.apache.org/maven2/io/github/libxposed`

Use Maven Central for artifacts, POMs, source JARs, and Javadocs. Do not mechanically replace documentation URLs with Maven paths. Do not guess API signatures; verify them from the actual API artifact/source.

## Diagnostics policy

Debug and Release share the same core feature logic. Only diagnostics depth may differ.

**Release** keeps low-frequency operational diagnostics: build identity, compatibility, essential lifecycle/hook readiness, hot reload result, and important errors.

**Debug** may additionally include bounded event-driven topology, parent/index/path, bounds, measured size, translation, hook lifecycle, and state snapshots.

Detailed diagnostics must still be lightweight: prefer **event -> snapshot -> report** over polling or continuous sampling.

The built-in diagnostic report is the preferred feedback path. Collection must remain user-triggered and bounded.

## Performance and energy rules

Before adding a hook, observer, listener, coroutine, callback, or shell call, ask whether it wakes more often than the underlying state can meaningfully change.

Prefer native callbacks, one-shot inspection, cached immutable metadata, structured low-frequency events, scoped coroutines, and shared helpers.

"Lightweight" means reducing unnecessary runtime work and architectural redundancy, not merely shrinking APK size.

## App architecture

Persist only real user preferences. Do not confuse UI preview state with module runtime state.

Prefer platform solutions:
- LocaleManager for per-app language;
- DataStore for appropriate persistent preferences;
- predictive back/navigation APIs;
- normal Android component/manifest behavior for launcher and activity entries.

Avoid custom infrastructure when a maintained platform solution already exists.

## Branching and versioning

- `main`: stable/installable/validated baseline.
- `dev`: ongoing module integration.
- `feat/*`: only for larger isolated experiments; merge back into `dev`.

Do not promote SystemUI work to `main` until structurally complete, CI-green, diagnostics-clean, and required real-device validation is complete.

Keep commits atomic and semantic. For APK-affecting changes, code/resources, changelog, and internal build identity should remain one logical change.

The display version changes only when explicitly advancing the formal external version. Normal APK-affecting iterations advance internal versionCode/buildId. Documentation-only changes do not require an APK build-number bump.

## CI and real-device validation

CI is a gate, not a replacement for runtime testing.

For APK-affecting work verify the pinned profile, Debug build, Release build where applicable, Modern Xposed metadata, signing, Debug/Release diagnostics boundary, and artifact generation.

For runtime-sensitive work provide a focused real-device test list. Depending on impact this can include normal status bar, charging/non-charging, lock screen, AOD, Control Center open/close, SystemUI restart, hot reload, configuration changes, and coexistence with other status-bar modules.

## Definition of done

A change is complete only when all applicable checks pass:
- pre-change review;
- standardized/lightweight/modern implementation review;
- copy review when text changed;
- MIUIX UI review when UI changed;
- post-change review;
- appropriate Debug/Release diagnostics;
- CI;
- explicit real-device validation result or a clear "awaiting device validation" status;
- CHANGELOG update for user-visible or engineering-significant changes;
- no runtime claim based on CI alone.

## Required implementation report

Every implementation report should state:
1. what changed and why;
2. standardization/lightweight/modernization review result;
3. text-review result when text changed;
4. MIUIX UI-review result when UI changed;
5. CI result;
6. required real-device test scenarios;
7. known limitations or compatibility boundaries.

These rules should be re-read and applied for every future feature or fix.
