# Changelog

All notable changes to CombinedStatus are documented in this file.

The project follows a Keep a Changelog-style structure. Development changes remain under Unreleased until a display version is formally published.

## [Unreleased]

### Added
- Structured runtime diagnostics protocol with stable event/component/state fields and a report-time health snapshot for module loading, compatibility, status-host capture, network/airplane/tint sources, renderer, hot reload, and diagnostics transport without polling or resident collection.
- Canary build channel: non-debuggable and release-optimized like the production artifact, while retaining bounded runtime diagnostics controlled by the in-app General/Detailed preference.
- App-side diagnostics level preference with General/Detailed choices, defaulting to General and mirrored through libxposed API 102 RemotePreferences for later hook-side consumption.
- English and Simplified Chinese MIUIX application shell for HyperOS.
- Adaptive launcher icon with separate foreground/background resources and Android themed-icon support.
- Fixed CI debug signing so successive test APKs can update in place.
- MIUIX 0.9.4 navigation runtime with serializable routes, standard page transitions, predictive back, and direction-aware swipe-back gestures.
- Home, Features, and Settings top-level navigation with a MIUIX floating navigation bar.
- Official MIUIX 0.9.4 floating-navigation blur recipe with automatic runtime-shader fallback, 22 dp texture blur, 45% surface blend, and compact theme-aware GlassStrokeSmall highlight.
- Persistent appearance preferences for theme mode, floating/non-floating bottom navigation, Standard/Blur/Glass floating-navigation material style, and in-app swipe-back.
- Android 13+ per-app language selection for system default, English, and Simplified Chinese.
- Optional launcher icon hiding through a dedicated activity alias while retaining a non-launcher CATEGORY_INFO front door.
- Modern Xposed API 102 module baseline with a single Java entry point and a static `com.android.systemui` scope.
- One-shot SystemUI structural compatibility probe for the status bar, Control Center, keyguard header, and battery-view hosts.
- Pinned compatibility profile generated from the exact HyperOS SystemUI 17.03.260226.r APK.
- Read-only capture of the primary HyperOS status-bar host after `MiuiNotificationStatusContainer.onFinishInflate()`, stored as a weak reference for later rendering integration.
- Manual SystemUI scope restart from the module app with an explicit Root confirmation and no resident Root service.
- Modern Xposed API 102 hot reload lifecycle with automatic app-update reload metadata and hook migration for the status-host observer.
- One-shot native SystemUI status-view inventory after host layout, covering mobile network, Wi-Fi, and battery views without modifying geometry or drawing.
- Root-view topology inventory for native status icons and their verified SystemUI containers, with bounded one-shot traversal and ancestor/path diagnostics.
- Built-in export/share diagnostic report for feedback, containing app/build, basic device, and recent CombinedStatus runtime information without a resident collection service.
- Diagnostic report now reads LSPosed's own module log files first and falls back to logcat, matching the framework's actual log storage.
- Debug topology diagnostics now flag mobile-network and Wi-Fi candidate views by class/resource identity so third-party status-bar container changes remain observable.
- Verified HyperOS Home Wi-Fi/mobile collectors now provide the production event-driven network state source; detailed collector diagnostics remain Debug-only, with no polling or SystemUI geometry mutation.
- Event-driven normalized state storage now combines battery state, semantic Home Wi-Fi visibility/resource state, and per-subscription mobile signal/VoLTE/VoWiFi resources without drawing or changing native SystemUI geometry.
- Mobile SIGNAL events now sample the authoritative Android airplane-mode global state so the unavailable glyph can switch on the first SystemUI signal-null event instead of waiting for the default-data subscription.
- Airplane mode now uses an event-driven ContentObserver on Settings.Global.AIRPLANE_MODE_ON as the primary source because runtime validation showed the SystemUI repository command is not invoked by the device's actual toggle path; mobile-signal sampling remains a fallback.
- Build 94's compile-only anchorRect regression was corrected without restoring the retired battery-anchor island follower.
- Deterministic signal parsing maps SystemUI mobile `signal_0..4` / `signal_null` and Wi-Fi `wifi_signal_0..3` resources to semantic levels, covered by local unit tests.
- Debug builds now include a Home-only, non-layout visual probe that reproduces the P11BJ 120-unit ring/Wi-Fi/mobile-dot geometry inside the verified native 105×108 battery slot using ViewGroupOverlay; native icons remain visible and native geometry is untouched.
- A pure shared layout-policy layer now models visual size, adaptive neighbor gap, requested/applied slot width, end anchoring, render mode, and motion ownership for future multi-scene reuse; it is not wired to runtime layout yet.
- A pure scene-capability policy now classifies Home, notification-shade transition, Control Center, keyguard, and AOD without duplicating geometry rules; charging variants remain render state rather than a separate scene.
- Debug stable-status diagnostics now capture one-shot Home slot readiness metrics (padding, layout params, margins, adjacent status-icon boundary, clipping, RTL, and native translation) without mutating geometry.
- A shared event-driven SystemUI tint source now follows MiuiBatteryMeterView's native DarkIconDispatcher application path, with a reusable color policy for scene rendering.
- Debug Home rendering now records one bounded state-to-draw latency line per actually rendered state transition; the temporary eight-frame transition probe has been removed after it identified the native visibility owner.
- Runtime validation showed the native battery anchor does not move during the charging-island transition, so the ineffective battery-anchor follower was removed; Debug now maps the real Home right-side owner candidates (status container, end-side content, status-bar icons, battery container/view) with bounded pre-draw samples and zero geometry writes.
- The Home visual probe is now hosted by the real MiuiBatteryMeterView overlay instead of the broader MiuiStatusBatteryContainer overlay, so sibling Wi-Fi/mobile relayouts no longer own the probe's overlay lifecycle.
- Runtime transition diagnostics proved SystemUI temporarily sets MiuiStatusBatteryContainer to alpha=0 and INVISIBLE during Wi-Fi/mobile semantic changes; the Home probe is therefore lifted to MiuiNotificationStatusContainer's overlay while remaining anchored to the real battery bounds, so native container visibility no longer blanks CombinedStatus.
- Debug Home rendering now validates a real measured CombinedStatus slot without adding a fourth MiuiStatusBatteryContainer child: it preserves the native MiuiBatteryMeterView as the lifecycle/island-motion carrier, extends only its leading padding by one native square status unit, renders CombinedStatus into that leading region, and leaves native Battery/Wi-Fi/mobile visibility and translation untouched.
- Debug owned-slot validation now records one matched pre/post geometry pair around the existing paddingStart write, including measured-width expansion, stable battery end-anchor delta, adjacent status-icon boundary movement, and neighbor-gap delta without adding hooks or continuous sampling.

### Changed
- Appearance preview header is now explicitly a style-preview summary instead of duplicating theme/color state already shown inside the preview scene. The floating navigation sample trims MIUIX's preview-irrelevant 36 dp system-navigation tail to a compact shadow-safe margin, reducing card height without changing the real MainHub component.
- Floating navigation material is now a single three-state setting: Standard uses the native MIUIX surfaceContainer capsule, Blur uses the shared 22 dp / 45% MIUIX texture blur without highlight, and Glass adds the matching GlassStrokeSmallLight/Dark preset. Existing blur preferences migrate without changing their previous visual meaning.
- Appearance preview now scales its complete MIUIX component scene uniformly to 82% instead of clipping individual controls, keeps a fixed navigation scene height to prevent layout jumps, uses MIUIX typography sizes to drive rounded text-skeleton hierarchy, and mirrors Standard/Blur/Glass material changes from the real MainHub.
- Real MainHub floating navigation and Appearance preview now share one MIUIX 0.9.4 glass-material specification: 22 dp texture blur, 45% surfaceContainer blend, GlassStrokeSmallDark in dark mode and GlassStrokeSmallLight in light mode. The preview therefore follows future material tuning automatically instead of carrying a duplicated approximation.
- Appearance navigation preview now reuses the same MIUIX 0.9.4 textureBlur/BlurDefaults/BlendColorEntry/GlassStroke highlight path as the real MainHub when floating blur is enabled, and both navigation styles render inside a fixed 65 dp preview slot so switching styles cannot remeasure the page; the standard bar retains its native 64 dp item height + divider and the floating capsule retains its native 52 dp visual body while only preview-irrelevant system navigation inset space is clipped.
- Appearance preview now delegates both bottom-navigation modes to MIUIX 0.9.4 itself: floating uses the real FloatingNavigationBar/FloatingNavigationBarItem implementation (including the library squircle, shadow, spacing, selection alpha, and icon geometry), standard mode uses NavigationBar/NavigationBarItem (including library typography and divider), and the switch/slider sample rows now use real MIUIX Card containers rather than custom rounded surfaces.
- Appearance preview removes the dialog and now uses MIUIX 0.9.4 control geometry directly: the switch is the real 49x28dp checked Switch, the slider is the real 28dp Slider with enabled-state semantic colors while remaining read-only, non-floating navigation uses the actual NavigationBar/NavigationBarItem components, and floating navigation mirrors MIUIX's 50dp-radius capsule, 52dp minimum height, 28dp icons, 10dp icon padding, 12dp spacing, and 10dp/20%-black drop shadow.
- Appearance preview now gives the MIUIX dialog its own bounded mini-scene so it no longer covers the switch, slider, or navigation samples; non-floating navigation now renders as a true full-width bottom dock with a top divider instead of a rounded full-width pill, while floating navigation keeps the centered capsule form.
- Appearance preview now mirrors the observed MIUIX control grammar more closely: the switch and slider are separate setting surfaces, the slider uses a long rounded filled track with a bordered thumb, the dialog becomes a wide lower action sheet-style overlay with centered skeleton copy and equal-width secondary/primary buttons, and the miniature navigation uses the real Home/Features/Settings icon-plus-label hierarchy.
- The miniature MIUIX dialog is now rendered as a centered overlay over a deliberately subdued page skeleton instead of occupying a normal layout row, preserving real dialog semantics while keeping the switch, slider, palette, and navigation context visible behind it.
- Appearance preview now behaves as an abstract MIUIX component board: a miniature dialog, switch, slider, theme swatches, skeleton text bars, and the real Home/Tune/Settings navigation icons replace literal preview copy; floating versus standard navigation follows the actual appearance setting while the preview remains read-only and content-sized.
- Appearance preview is now a compact full-width miniature MIUIX scene instead of a left-right color-swatch control: the card previews typography, semantic surfaces, theme colors, switch state, and floating/standard navigation while remaining read-only and density-conscious.
- Appearance preview tray now uses a restrained translucent MIUIX tonal surface instead of a solid high-container fill, with clearer semantic sample outlines to avoid a disabled, greyed-out appearance in light mode.
- Appearance palette preview now uses a compact read-only inset tray with MIUIX surface hierarchy, a restrained outline, and smaller color samples so it reads as a preview rather than another interactive preference while preserving the settings-page density.
- Core Home rendering no longer depends on `BuildConfig.DEBUG`; build-channel flags now isolate development probes from runtime rendering and bounded Canary diagnostics.
- Appearance palette preview no longer uses a separate section title; its secondary "Current colors" label now sits directly beneath the three theme swatches using MIUIX body2 and onSurfaceContainerVariant styling.
- Diagnostics level is now a persistent runtime preference rather than being presented as a direct alias of the APK build type; build type remains a separate capability boundary.
- Diagnostics now uses an MIUIX device-information card hierarchy inspired by established HyperOS settings patterns: app details, live device/system values, module runtime, and report actions are separated clearly.
- Device diagnostics now resolve the market device name, Android/API level, HyperOS incremental version with software-update suffix when available, and the installed SystemUI package version at runtime instead of showing a fixed platform label.
- Appearance settings now model light/dark mode and dynamic color as independent preferences. The page uses a compact MIUIX theme-mode dropdown, a separate dynamic-color switch, and a concise live palette preview; legacy `theme_mode=Dynamic` is interpreted as System + dynamic color without losing the previous choice.
- Swipe-back behavior is now configured from the main Settings page instead of Appearance, keeping Appearance limited to theme and visual effects.
- Appearance copy now labels the read-only palette card as current colors and shortens the theme-mode description to reflect its actual light/dark responsibility.
- The color preview uses a compact horizontal read-only layout with primary, secondary, and surfaceContainerHigh samples and a uniform 1 dp MIUIX outline at 30% opacity in both light and dark themes.
- Bottom navigation can now switch between MIUIX 0.9.4 FloatingNavigationBar and the standard full-width NavigationBar. Floating style remains the default; its blur option follows the parent style with MIUIX AnimatedVisibility and stays hidden when the standard bar is selected.
- Renamed swipe-back copy to describe a page-level horizontal return gesture, avoiding confusion with the system-wide edge-back gesture.
- Minimum Android version is Android 13 / API 33 to match the current MIUIX blur baseline.
- Android compile and target SDK baseline is API 37.
- Application JVM target is 21 to align with the MIUIX 0.9.4 navigation runtime.
- Launcher icon resources now use the Android adaptive-icon resource model.
- The original single-screen settings index is split into top-level Home, Features, and Settings areas while deeper settings remain on the navigation stack.
- The Settings hub now groups controls into Appearance & interaction, App, and Diagnostics & maintenance cards, keeping related actions together without changing their behavior.
- Appearance settings now drive the root theme and navigation behavior instead of temporary screen-local preview state.
- Aligned system bar icon appearance with the selected theme mode.
- Aligned top-level pager fling and back-to-home behavior with the MIUIX 0.9.4 example patterns.
- Renamed the optional floating-bar effect to floating navigation blur and migrated its saved preference key.
- Simplified in-app wording and removed development-oriented placeholder phrasing.
- Aligned page spacing and typography with MIUIX 0.9.4 defaults, including standard cards, section spacing, top-level scroll behavior, and bottom padding handling.
- Standardized user-facing settings copy and hardened platform language and launcher-entry state handling.
- Polished settings summaries to use shorter, more natural system-style phrasing without unnecessary semicolons.
- Added CI validation for modern Xposed metadata while keeping the initial module entry point hook-free.
- Standardized module terminology on mobile network / 移动网络 and clarified Xiaomi HyperOS as the target platform in module descriptions.
- Diagnostics now report the active Modern Xposed API 102 runtime and hot-reload capability instead of the earlier pre-hook placeholder state.
- Diagnostics UI now uses MIUIX read-only information rows for module framework, compatibility baseline, and the native status probe; preview copy no longer claims SystemUI is disconnected.
- Native status probe wording now describes the status-bar topology check without exposing host-lifecycle implementation details.
- Build channel and diagnostics level are now separate capability axes: Debug retains development-only probes, Canary keeps bounded runtime diagnostics switchable from the app, and Release keeps only low-frequency operational diagnostics.
- SystemUI restart and on-demand diagnostic collection now share one bounded Root shell executor instead of duplicating process lifecycle code.
- Push CI for `main`/`dev` now produces signed Debug and non-debuggable Canary artifacts; signed Release remains a separate formal build path.
- LibXposed artifacts are resolved explicitly from Maven Central at `repo.maven.apache.org`, restricted to the `io.github.libxposed` group.
- Restart confirmation now follows the MIUIX two-action dialog layout with equal-width actions and user-facing SystemUI wording.
- Xposed lifecycle diagnostics now include the internal build ID to make hot-reload generation changes directly visible in LSPosed logs.
- Cold-start initialization now configures edge-to-edge before Compose content and prepares one-time platform state before the first composition; later theme changes update only system-bar icon appearance.
- Debug diagnostics now record a one-shot, bounded three-level inventory of the verified MIUI status-icon container subtree to identify live icon ownership without adding hooks or reading user-facing text.
- Diagnostic reports now default to the latest SystemUI process session instead of accumulating historical sessions across builds.
- Diagnostic report export continues to use Android's system document picker with an editable default text-file name.
- Diagnostic report sharing now uses a bounded MediaStore Downloads transport with a system-managed content URI, standard text/plain semantics, EXTRA_STREAM, ClipData, and temporary read permission.
- Temporary managed share reports are kept under Download/CombinedStatus and pruned to at most three recent files with a 24-hour age bound.
- Stable status geometry capture now waits for the first valid battery-view layout before recording the anchor, while keeping native SystemUI geometry untouched.
- Compatibility metadata, diagnostics copy, and CI verification now target SystemUI only, matching the module's actual `com.android.systemui` scope.
- Verified network collectors now feed the typed CombinedStatus state snapshot in all build types, while detailed change-only diagnostics remain Debug-only.
- Wi-Fi and mobile semantic state is now committed before the verified SystemUI emitter proceeds, and main-thread render invalidation is requested immediately so CombinedStatus and the native icon transition can enter the same UI frame.
- The Home visual probe now renders at full opacity and repaints from the native battery receiver's applied tint instead of recursively guessing colors from arbitrary child views.

### Fixed
- Network emitter hooks no longer resolve `kotlin.coroutines.Continuation` by name through the SystemUI ClassLoader; the verified `emit(Object, continuation)` overload is selected directly from each loaded emitter class, preventing Canary/optimized builds from losing all four Wi-Fi/mobile hooks before rendering can initialize.
- Network-state installation is now fail-soft per source: Wi-Fi and mobile resolve/install independently, a failure in one branch no longer unhooks the other, and diagnostics record the exact failing stage plus full exception type. Wi-Fi resource sampling returns to the Build 77/80 network-collector-validated post-emitter ImageView tag path, removing the unverified Icon.Resource.resId reflection that could abort the entire network source before any hook was installed.
- Retired the experimental Home owned-slot padding mutation after runtime validation showed a 105 px request expanding MiuiBatteryMeterView by 135 px, moving its end anchor and leaking layout effects into lock-screen/status scenes. Home debug rendering is restored to the read-only host overlay with zero native geometry writes.
- Home Wi-Fi roots are registered before the native binder proceeds; Wi-Fi icon tracing now follows the verified `getWifiIcon()` collector (`classId=1`) instead of the unrelated `setImageViewResId()` helper.
- CI certificate verification now accepts the current Android Build Tools signer output while still pinning the expected certificate SHA-256 digest.
- Per-app language selection now keeps the explicit System/English/Simplified Chinese choice visible even when the chosen language matches the current system locale.
- Normalized state diagnostics now include the battery plugged source consistently for both charging and discharging states.
- The Home visual probe now matches P11BJ unavailable/mobile-airplane rendering: all four mobile dots remain as low-alpha filled dots and the X marker is drawn on top.
- Render-state transitions now commit atomically: incomplete/unknown candidates retain the last stable CombinedStatus frame, while explicit Hidden/Unavailable states still update immediately; transparent uninitialized tint samples are ignored instead of blanking the icon.
