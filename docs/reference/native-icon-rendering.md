# Native status-icon resource rendering

## Scope

This note records reusable rendering evidence for native status-icon resources. It is reference evidence, not permission to change SystemUI-owned layout, tint, visibility, or animation.

## Verified target evidence

For the pinned HyperOS SystemUI target:

- the Wi-Fi pipeline exposes native presentation as `Icon.Resource`;
- the native status bar binds that presentation through an ImageView-based icon pipeline;
- Combined Status already consumes the native resource identity and resolved status-icon tint rather than maintaining a replacement Wi-Fi palette.

The retained target evidence does not establish a native step that rescales each drawable's internal alpha mask to a percentile-derived ceiling before rendering.

## Android rendering contract

Android `ImageView` applies image tint to the drawable. Under SRC_IN semantics, tint is masked by the drawable alpha. The drawable's authored alpha therefore remains part of the visual asset unless a separate owner explicitly changes it.

This is distinct from whole-image/view opacity during a transition: overall opacity may change while the drawable's internal alpha relationships remain authored by the resource.

## Combined Status consequence

For verified HyperOS center resources, prefer:

`native resource alpha mask -> native/resolved tint -> Combined Status placement/scale -> canvas`

Combined Status may measure optical bounds and align its own final drawing pixels because those are composition responsibilities. It should not rewrite the resource's internal alpha distribution merely to force parity unless exact-target evidence demonstrates the same native transformation.

A project-side percentile alpha normalization is therefore an A/B candidate to remove, not an upstream-native requirement.

## Confidence and limits

- Resource identity / Wi-Fi pipeline: exact-target static/runtime evidence in SystemUI-Reference.
- Android tint/alpha-mask semantics: platform contract.
- Absence of a HyperOS percentile-normalization step: supported by the inspected target/reference path, but device A/B validation remains required to determine whether removing the project-side transform fully resolves the observed optical mismatch.
