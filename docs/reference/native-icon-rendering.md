# Native status-icon resource rendering

## Scope

This note records reusable rendering evidence for native status-icon resources. It is reference evidence, not permission to change SystemUI-owned layout, tint, visibility, or animation.

## Exact-target Wi-Fi rendering contract

For the pinned HyperOS SystemUI `17.03.260226.r` target, the Home Wi-Fi path is now verified through exact APK inspection plus target-device runtime diagnostics.

Semantic/base resource flow:

`WifiIcon.Visible.icon (Icon.Resource)`
→ `MiuiWifiViewBinder`
→ `MiuiStatusBarIconViewHelper.transformResId(resId, useTint, light)`
→ `ImageView.setImageResource(transformedResId)`

`transformResId(...)` keeps the semantic/base resource ID separate from the resource actually displayed:
- `useTint=true` selects the mapped `*_tint` resource;
- otherwise the light/dark state selects the corresponding light or dark resource mapping.

The binder keeps the base resource ID in the native ImageView tag and reruns the transformation when UI mode changes.

Tint flow:
- when `useTint=true`, the native ImageView receives `setImageTintList(ColorStateList.valueOf(tint))`;
- when `useTint=false`, image tint is cleared and the selected light/dark drawable's own fill color is used.

For the verified `stat_sys_wifi_signal_3` family, normal, dark-mode, and tint resources use the same 20 × 20 vector geometry; their fill-color policy differs.

## Exact native final geometry

The Wi-Fi vector declares:
- intrinsic XML size: 20dp × 20dp;
- viewport: 20 × 20.

The native Home layout uses an `AlphaOptimizedImageView` with:
- width `WRAP_CONTENT`;
- height `MATCH_PARENT`;
- `adjustViewBounds=true`.

The owning status-icon `IconManager` resolves `status_bar_icon_height=20dp`. On the pinned target device, retained runtime diagnostics verify the final native Wi-Fi presentation as:
- `VectorDrawable`;
- intrinsic size: 75 × 75 px;
- drawable bounds: 0,0,75,75;
- ImageView measured size: 75 × 75 px;
- drawable alpha: 255;
- image alpha: 255;
- scale type: `FIT_CENTER`;
- image matrix: identity;
- padding: 0;
- runtime tint examples include `0xBF000000` on the light Home surface and `0xE6FFFFFF` on a dark surface.

Therefore the stable native path rasterizes the VectorDrawable once at its final 75 × 75 drawable bounds. It does not require an intermediate project bitmap or a second bitmap scaling pass.

## Build-404 correction

Build 404 removed the earlier percentile alpha normalization but retained this pipeline:

`native Drawable -> 96px probe Bitmap -> optical bounds -> filtered Bitmap rescale -> SRC_IN tint -> Combined Status canvas`

Preserving the alpha values inside that 96px bitmap does **not** preserve native final rendering. Antialias/coverage has already been generated at the 96px rasterization resolution and is then resampled into the smaller Combined Status center target.

A controlled rendering comparison of the same Wi-Fi vector shows the mechanism:
- direct vector rasterization at the representative 63px Combined Status target: 585 fully covered pixels and 275 partial-edge pixels;
- 96px rasterization followed by bilinear reduction to 63px: 474 fully covered pixels and 530 partial-edge pixels.

Total alpha mass remains close, but the two-stage path spreads coverage across substantially more semi-transparent edge pixels. That changes apparent edge sharpness / optical weight even when the final tint is identical.

This supersedes the earlier assumption that authored source alpha alone was the next sufficient parity boundary. Build 404 is still useful negative A/B evidence: removing percentile normalization did not remove the non-native rasterization boundary.

## Combined Status consequence

For verified native center resources, prefer:

`native semantic resource -> required native resource-state transformation -> module-owned Drawable clone -> resolved final bounds -> resolved native/user-policy tint -> direct Drawable draw`

A bitmap probe MAY still be used off the final presentation path when it is only needed for bounded optical measurement. Its rasterized pixels should not become the final drawable source unless a bitmap is itself the native resource contract.

Do not add grayscale multipliers, percentile alpha remaps, source-asset edits, or screenshot-fitted coverage compensation to counteract a resampling artifact.

## Confidence and limits

- Semantic/base resource and binder flow: exact-target DEX/runtime evidence.
- `transformResId` resource-state selection: exact-target DEX evidence.
- Wi-Fi vector dimensions/variants: exact target APK resource evidence.
- Final 75 × 75 ImageView/Drawable presentation: target-device runtime diagnostics.
- One-pass vs intermediate-bitmap difference: source inspection plus controlled rendering comparison.
- The same direct-draw principle is a strong candidate for other verified native center resources, but each resource family still needs its own state/resource contract checked before promotion.
