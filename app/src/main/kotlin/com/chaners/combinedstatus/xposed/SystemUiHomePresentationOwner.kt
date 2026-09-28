package com.chaners.combinedstatus.xposed

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedModule

internal object SystemUiHomePresentationOwner {
    private const val HOME_HOST =
        "com.android.systemui.statusbar.views.MiuiNotificationStatusContainer"
    private const val STATUS_ICON_CONTAINER =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
    private const val BATTERY_CONTAINER =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val BATTERY_VIEW =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val LEGACY_SLOT = "combined_status"

    private val representedSlots =
        linkedSetOf("wifi", "mobile", "stacked_mobile", "airplane", "no_sim")

    private var current: SystemUiCompactPresentationRegistry.Session? = null
    private var eventSink: ((String) -> Unit)? = null
    private var failNativeSink: ((String) -> Unit)? = null

    val installedHookCount: Int
        get() = SystemUiCompactPresentationRegistry.installedHookCount

    @Synchronized
    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onEvent: ((String) -> Unit)? = null,
        onFailNative: ((String) -> Unit)? = null,
    ): InstallResult {
        eventSink = onEvent
        failNativeSink = onFailNative
        return when (
            val result =
                SystemUiCompactPresentationRegistry.install(
                    module = module,
                    classLoader = classLoader,
                )
        ) {
            SystemUiCompactPresentationRegistry.InstallResult.Installed ->
                InstallResult.Installed
            SystemUiCompactPresentationRegistry.InstallResult.AlreadyInstalled ->
                InstallResult.AlreadyInstalled
            is SystemUiCompactPresentationRegistry.InstallResult.Failure ->
                InstallResult.Failure(result.reason)
        }
    }

    @Synchronized
    fun activate(host: Any): StateResult {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return StateResult.Failure("main-thread-required")
        }
        if (installedHookCount != 3) {
            return StateResult.Failure("hooks-not-ready")
        }
        val hostView =
            host as? ViewGroup
                ?: return StateResult.Failure("host-not-view-group")
        if (hostView.javaClass.name != HOME_HOST) {
            return StateResult.Failure("home-host-mismatch")
        }
        val statusIcons =
            NativeParticipantRuntimeAccess.groupFor(host)
                ?: return StateResult.Failure("status-icon-group-missing")
        if (statusIcons.javaClass.name != STATUS_ICON_CONTAINER) {
            return StateResult.Failure("status-icon-group-type-mismatch")
        }
        val batteryContainer =
            hostView.directChild(BATTERY_CONTAINER) as? ViewGroup
                ?: return StateResult.Failure("battery-container-missing")
        val battery =
            batteryContainer.directChild(BATTERY_VIEW)
                ?: return StateResult.Failure("battery-view-missing")
        val batteryCarrier =
            SystemUiHomeCarrierMetrics.resolveCarrierView(battery)
                ?: return StateResult.Failure("battery-core-carrier-missing")

        val existing = current
        if (
            existing?.matches(
                host = hostView,
                statusIcons = statusIcons,
                batteryContainer = batteryContainer,
                battery = battery,
                batteryCarrier = batteryCarrier,
            ) == true
        ) {
            existing.syncEndReservation()
            val masked = existing.refreshClipMasks()
            if (current !== existing) {
                return StateResult.Failure("session-reuse-failed-native-restored")
            }
            batteryContainer.requestLayout()
            return StateResult.Active(representedSlots.size, masked, true)
        }

        existing?.let { session ->
            SystemUiCompactPresentationRegistry.release(
                session = session,
                source = "host-replaced",
            )
        }

        val created =
            SystemUiCompactPresentationRegistry.createSession(
                host = hostView,
                statusIcons = statusIcons,
                batteryContainer = batteryContainer,
                battery = battery,
                batteryCarrier = batteryCarrier,
                representedSlots = representedSlots,
                surfaceName = "home",
                eventPrefix = "homePresentation",
                onEvent = { event -> eventSink?.invoke(event) },
                onFailNative = ::onSessionFailure,
            )
        val session =
            when (created) {
                is SystemUiCompactPresentationRegistry.CreateResult.Ready ->
                    created.session
                is SystemUiCompactPresentationRegistry.CreateResult.Failure ->
                    return StateResult.Failure(created.reason)
            }

        current = session
        val masked = SystemUiCompactPresentationRegistry.activate(session)
        if (current !== session) {
            return StateResult.Failure("session-activation-failed-native-restored")
        }
        batteryContainer.requestLayout()
        eventSink?.invoke(
            "homePresentation active carrier=MiuiStatusBatteryContainer.overlay " +
                "representedSlots=" + representedSlots.joinToString(",") +
                " maskedViews=" + masked +
                " slotExclusion=scoped-native-measure-layout " +
                    "carrierReservation=status-icons-end-padding " +
                    "carrierAuthority=battery_icon_container visualMask=clipBounds " +
                "nativeLayoutReservationWrites=1 nativeTranslationWrites=0 " +
                    "nativeAlphaWrites=0 nativeVisibilityWrites=0",
        )
        return StateResult.Active(representedSlots.size, masked, false)
    }

    @Synchronized
    fun ownsBatteryContainer(candidate: ViewGroup): Boolean =
        current?.ownsBatteryContainer(candidate) == true

    @Synchronized
    fun deactivate(source: String): StateResult {
        val session = current ?: return StateResult.Inactive(0)
        current = null
        val restored =
            SystemUiCompactPresentationRegistry.release(
                session = session,
                source = source,
            )
        eventSink?.invoke(
            "homePresentation inactive source=" + source +
                " restoredViews=" + restored +
                " nativeTranslationWrites=0 nativeAlphaWrites=0 nativeVisibilityWrites=0",
        )
        return StateResult.Inactive(restored)
    }

    @Synchronized
    fun releaseGenerationForHotReload(): Int {
        val session = current ?: return 0
        current = null
        val restored =
            SystemUiCompactPresentationRegistry.release(
                session = session,
                source = "hotReload-oldGeneration",
            )
        eventSink = null
        failNativeSink = null
        return restored
    }

    @Synchronized
    fun resetRuntimeState(source: String) {
        deactivate(source)
        SystemUiCompactPresentationRegistry.resetRuntimeState(source)
        eventSink = null
        failNativeSink = null
    }

    @Synchronized
    fun cleanupLegacyParticipant(host: Any): LegacyCleanupResult {
        val group =
            NativeParticipantRuntimeAccess.groupFor(host)
                ?: return LegacyCleanupResult.Failure("status-icon-group-missing")
        val legacyView = NativeParticipantRuntimeAccess.findSlotView(group, LEGACY_SLOT)
        val handles =
            when (val resolution = NativeParticipantRuntimeAccess.resolve(host)) {
                is NativeParticipantRuntimeAccess.ResolveResult.Ready ->
                    resolution.handles
                is NativeParticipantRuntimeAccess.ResolveResult.Failure -> {
                    return if (legacyView == null) {
                        LegacyCleanupResult.NotPresent
                    } else {
                        LegacyCleanupResult.Failure(resolution.reason)
                    }
                }
            }
        val holder = NativeParticipantRuntimeAccess.iconHolder(handles, LEGACY_SLOT)
        if (legacyView == null && holder == null) {
            return LegacyCleanupResult.NotPresent
        }
        val removal =
            NativeParticipantRuntimeAccess.removal(handles.controller.javaClass)
                ?: return LegacyCleanupResult.Failure("legacy-removal-contract-missing")
        val removed =
            runCatching {
                NativeParticipantRuntimeAccess.invokeRemoval(handles, removal, LEGACY_SLOT)
                NativeParticipantRuntimeAccess.clearBindableEntries(handles, LEGACY_SLOT)
                NativeParticipantRuntimeAccess.findSlotView(group, LEGACY_SLOT) == null &&
                    NativeParticipantRuntimeAccess.iconHolder(handles, LEGACY_SLOT) == null
            }.getOrDefault(false)
        return if (removed) {
            LegacyCleanupResult.Removed
        } else {
            LegacyCleanupResult.Failure("legacy-removal-verification-failed")
        }
    }

    @Synchronized
    private fun onSessionFailure(reason: String) {
        val session = current ?: return
        current = null
        SystemUiCompactPresentationRegistry.release(
            session = session,
            source = "fail-native:" + reason,
        )
        eventSink?.invoke(
            "homePresentation failNative reason=" + reason + " restoredNative=true",
        )
        failNativeSink?.invoke(reason)
    }

    internal object EndReservationPolicy {
        fun resolvePaddingEndDelta(
            nativeHide: Boolean,
            actualBatteryWidthPx: Int,
            requestedSlotWidthPx: Int,
        ): Int =
            SystemUiCompactPresentationRegistry.EndReservationPolicy
                .resolvePaddingEndDelta(
                    nativeHide = nativeHide,
                    actualBatteryWidthPx = actualBatteryWidthPx,
                    requestedSlotWidthPx = requestedSlotWidthPx,
                )
    }

    internal object OwnedListEntries {
        fun <T> addOwnedEntries(
            target: MutableList<T>,
            entries: Collection<T>,
        ): List<T> =
            SystemUiCompactPresentationRegistry.OwnedListEntries
                .addOwnedEntries(
                    target = target,
                    entries = entries,
                )

        fun <T> restoreOwnedEntries(
            target: MutableList<T>,
            ownedEntries: List<T>,
        ) =
            SystemUiCompactPresentationRegistry.OwnedListEntries
                .restoreOwnedEntries(
                    target = target,
                    ownedEntries = ownedEntries,
                )

        fun <T, R> withTemporaryEntries(
            target: MutableList<T>,
            entries: Collection<T>,
            block: () -> R,
        ): R =
            SystemUiCompactPresentationRegistry.OwnedListEntries
                .withTemporaryEntries(
                    target = target,
                    entries = entries,
                    block = block,
                )
    }

    private fun ViewGroup.directChild(className: String): View? {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.javaClass.name == className) {
                return child
            }
        }
        return null
    }

    internal sealed interface InstallResult {
        data object Installed : InstallResult
        data object AlreadyInstalled : InstallResult
        data class Failure(val reason: String) : InstallResult
    }

    internal sealed interface StateResult {
        data class Active(
            val representedSlots: Int,
            val maskedViews: Int,
            val reused: Boolean,
        ) : StateResult
        data class Inactive(val restoredViews: Int) : StateResult
        data class Failure(val reason: String) : StateResult
    }

    internal sealed interface LegacyCleanupResult {
        data object NotPresent : LegacyCleanupResult
        data object Removed : LegacyCleanupResult
        data class Failure(val reason: String) : LegacyCleanupResult
    }
}
