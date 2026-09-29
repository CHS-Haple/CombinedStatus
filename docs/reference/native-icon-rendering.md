# Native status-icon resource rendering

## Scope

This note records reusable rendering evidence for native status-icon resources on the pinned HyperOS target. It is reference evidence, not permission to change SystemUI-owned layout, tint, visibility, or animation.

## Exact target

- SystemUI: `17.03.260226.r`
- APK SHA-256: `a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d`
- Device density/runtime evidence: `status_bar_icon_height = 20dp` resolves to the verified 75px Home icon height on the current target device.

## Home Wi-Fi semantic source

The Home Wi-Fi pipeline is:

`LocationBasedWifiViewModel -> WifiIcon.Visible / Hidden -> MiuiWifiViewBinder -> ModernStatusBarWifiView`

For a visible state:

- `WifiIcon.Visible.icon` is an `Icon.Resource`;
- the raw resource ID is the semantic icon identity;
- `WifiIcon.Hidden` remains authoritative even if the previously bound ImageView still retains a visible drawable/tag.

Guiyuan may therefore use the raw model resource for Wi-Fi semantics, but that raw semantic ID is **not yet the final rendered resource ID**.

## Native View and geometry path

`ModernStatusBarWifiView.constructAndBind(...)` inflates `new_status_bar_wifi_group.xml` and binds it through `MiuiWifiViewBinder.bind(...)`.

The exact layouts establish:

- `ModernStatusBarWifiView`: `WRAP_CONTENT x MATCH_PARENT`, vertically centered;
- inner Wi-Fi `AlphaOptimizedImageView`: `WRAP_CONTENT x MATCH_PARENT`, `adjustViewBounds=true`;
- `AlphaOptimizedImageView` does not override ImageView drawing or measurement behavior beyond overlapping-rendering reporting;
- `IconManager` supplies a modern status-icon slot with `WRAP_CONTENT x mIconSize`;
- `mIconSize = R.dimen.status_bar_icon_height = 20dp`.

The Wi-Fi vector resources are themselves `20dp x 20dp` with a `20 x 20` viewport. Native steady-state rendering therefore does not require an intermediate bitmap merely to bridge the vector intrinsic size to the Home status-icon slot.

## HyperOS Light / Dark / Tint resource variants

The exact target contains three corresponding Wi-Fi resource variants. Example for level 3:

- Light/base: `stat_sys_wifi_signal_3`;
- Dark: `stat_sys_wifi_signal_3_darkmode`;
- Tint: `stat_sys_wifi_signal_3_tint`.

The geometry/path data is identical for this verified family, but color/alpha ownership differs:

- Light/base path fill resolves to `#FFFFFFFF`;
- Dark path fill resolves to `#BF000000`;
- Tint path fill is `#FF000000` and receives the current ImageView tint.

`com.miui.systemui.statusbar.Icons` initializes Light / Dark / Tint conversion maps. For `stat_sys_wifi_signal_3`, the exact IDs are:

- Light/base: `0x7f081b3e`;
- Dark: `0x7f081b3f`;
- Tint: `0x7f081b40`.

`MiuiStatusBarIconViewHelper.transformResId(rawResId, useTint, isLight)` selects:

- `useTint=true` -> Tint map;
- otherwise `isLight=true` -> Light map;
- otherwise -> Dark map.

This transformation is part of the native presentation contract. The raw semantic resource is therefore intentionally separated from the final native presentation resource.

## Tint-state path

`ModernStatusBarView.onLightDarkTintChanged(...)` resolves local dark/tint state and forwards it to the binding.

The Wi-Fi binding stores:

`Triple(useTint, isLight, color)`

in its tint/light-color flow.

The native signal collector then:

1. reads the raw `WifiIcon.Visible.icon` resource;
2. stores the raw resource ID on the ImageView tag;
3. calls `transformResId(rawResId, useTint, isLight)`;
4. calls `ImageView.setImageResource(transformedResId)`.

The tint flow separately:

- calls `setImageTintList(ColorStateList.valueOf(color))` when `useTint=true`;
- clears `imageTintList` when `useTint=false`;
- re-runs `transformResId(...)` and `setImageResource(...)` when the presentation mode changes.

Therefore the verified native steady paths are:

- **Tint mode:** Tint VectorDrawable + ImageTintList;
- **Light non-tint mode:** Light/base VectorDrawable + no ImageTintList;
- **Dark non-tint mode:** Dark VectorDrawable + no ImageTintList.

## Final draw path

No Wi-Fi-specific bitmap rendering layer was found between the bound ImageView and framework drawing.

The verified path is:

`semantic Icon.Resource`
-> `transformResId`
-> `ImageView.setImageResource`
-> native VectorDrawable
-> final ImageView bounds / framework draw

The traditional `StatusBarIconView` path used by neighboring status icons follows the same broader principle: HyperOS transforms SystemUI resource variants, assigns a Drawable to the ImageView, and `StatusBarIconView.onDraw()` delegates to ImageView after only its normal icon/appearance scale. It does not create a large cached bitmap and resample it back into the slot.

## Build 403 / 404 historical correction

Build 403 rendered verified center resources through a project-owned bitmap path:

`raw resource -> 96px-max raster probe -> percentile alpha remap -> optical sizing -> bitmap resample -> SRC_IN tint`.

Build 404 removed only the percentile alpha remap:

`raw resource -> 96px-max raster probe -> authored alpha retained -> optical sizing -> bitmap resample -> SRC_IN tint`.

Target-device feedback reports Build 404 as visually worse than Build 403. That result does **not** establish percentile alpha remapping as native or correct. Instead, exact-target reverse engineering shows that both builds still diverge earlier from HyperOS by using the raster probe as the final rendered asset.

The stronger current root-cause candidate is therefore the intermediate rasterization/resampling boundary itself. Build 403's alpha normalization may have partially compensated for losses introduced by that non-native path without making the path correct.

## Guiyuan consequence

For verified native center resources, the preferred rendering order is now:

`semantic native resource`
-> native-compatible presentation selection
-> cloned Drawable / VectorDrawable
-> final resolved Guiyuan bounds
-> direct Drawable draw

Optical measurement may still require a bounded probe because Guiyuan places a native resource inside a different compact composition. If retained, that probe should be **measurement-only** and must not become the bitmap subsequently drawn to screen.

Keep these concerns separate:

1. semantic resource identity;
2. HyperOS state-dependent resource transformation;
3. Guiyuan optical measurement / placement;
4. final Drawable rasterization;
5. tint ownership.

Do not use per-resource gray multipliers, percentile/coverage remaps, source-asset edits, or screenshot-fitted constants as a substitute for matching the verified native rendering contract.

## Validation strategy

Because two native-path differences are now verified, validate them independently where practical:

1. first remove the bitmap as the rendered asset while keeping current resource/tint/geometry policy unchanged;
2. if state-dependent parity still differs, evaluate the HyperOS Light / Dark / Tint resource transformation as its own boundary.

This preserves attribution and avoids concluding that a multi-variable visual change proves the cause.

## Confidence and limits

- Semantic Wi-Fi model and collector timing: exact-target static plus prior runtime evidence.
- View/layout/resource dimensions: exact target APK resources and DEX.
- Light / Dark / Tint mapping and Binder resource/tint path: exact target DEX/resource inspection.
- Absence of an equivalent 96px raster-cache/resample step in the verified native Wi-Fi/status-icon path: exact target directed inspection.
- Guiyuan still owns a different compact composition, so native 20dp geometry is evidence for rendering semantics rather than a requirement to copy the native slot size into the Guiyuan center.
