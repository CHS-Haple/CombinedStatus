package com.chaners.combinedstatus.xposed

import android.os.Looper
import android.view.View
import android.view.ViewGroup

/**
 * QS_FAKE-specific adapter for the shared compact-presentation registry.
 *
 * HyperOS owns source selection plus fake/real transition motion. This adapter
 * owns only reversible fake-carrier slot exclusion, visual masks and local end
 * reservation while the transition surface is active.
 */
internal object SystemUiControlCenterPresentationOwner {
    private const val BATTERY_CONTAINER =
        "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val STATUS_ICON_CONTAINER =
        "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
    private const val BATTERY_VIEW =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"

    private val representedSlots =
        linkedSetOf("wifi", "mobile", "stacked_mobile", "airplane", "no_sim")

    private var current: SystemUiCompactPresentationRegistry.Session? = null
    private var eventSink: ((String) -> Unit)? = null
    private var failNativeSink: ((String) -> Unit)? = null
    private var readySink: ((StateResult.Active) -> Unit)? = null

    @Synchronized
    fun activate(
        host: ViewGroup,
        sourceCarrier: ViewGroup,
        onEvent: ((String) -> Unit)? = null,
        onFailNative: ((String) -> Unit)? = null,
        onReady: ((StateResult.Active) -> Unit)? = null,
    ): StateResult {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            return StateResult.Failure("main-thread-required")
        }
        if (host.javaClass.name != BATTERY_CONTAINER) {
            return StateResult.Failure("qs-fake-carrier-type-mismatch")
        }
        if (!SystemUiHomePresentationOwner.ownsBatteryContainer(sourceCarrier)) {
            return StateResult.Failure("source-not-home-compact-owner")
        }

        val statusIcons =
            host.directChild(STATUS_ICON_CONTAINER) as? ViewGroup
                ?: return StateResult.Failure("status-icons-missing")
        val battery =
            host.directChild(BATTERY_VIEW)
                ?: return StateResult.Failure("battery-view-missing")
        val batteryCarrier =
            SystemUiHomeCarrierMetrics.resolveCarrierView(battery)
                ?: return StateResult.Failure("battery-core-carrier-missing")

        eventSink = onEvent
        failNativeSink = onFailNative
        readySink = onReady

        val existing = current
        if (
            existing?.matches(
                host = host,
                statusIcons = statusIcons,
                batteryContainer = host,
                battery = battery,
                batteryCarrier = batteryCarrier,
            ) == true
        ) {
            existing.syncEndReservation()
            val masked =
                SystemUiCompactPresentationRegistry.activate(
                    session = existing,
                    deferVisualMaskUntilLayout = true,
                    onLayoutReady = { maskedViews ->
                        onSessionLayoutReady(
                            session = existing,
                            maskedViews = maskedViews,
                            reused = true,
                        )
                    },
                )
            if (current !== existing) {
                return StateResult.Failure("session-reuse-failed-native-restored")
            }
            host.requestLayout()
            return if (existing.isLayoutCutoverReady()) {
                activeResult(maskedViews = masked, reused = true)
            } else {
                StateResult.Prepared(
                    representedSlots = representedSlots.size,
                    reused = true,
                )
            }
        }

        existing?.let { session ->
            SystemUiCompactPresentationRegistry.release(
                session = session,
                source = "host-replaced",
            )
        }

        val created =
            SystemUiCompactPresentationRegistry.createSession(
                host = host,
                statusIcons = statusIcons,
                batteryContainer = host,
                battery = battery,
                batteryCarrier = batteryCarrier,
                representedSlots = representedSlots,
                surfaceName = "control-center-fake",
                eventPrefix = "controlCenterPresentation",
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
        val masked =
            SystemUiCompactPresentationRegistry.activate(
                session = session,
                deferVisualMaskUntilLayout = true,
                onLayoutReady = { maskedViews ->
                    onSessionLayoutReady(
                        session = session,
                        maskedViews = maskedViews,
                        reused = false,
                    )
                },
            )
        if (current !== session) {
            return StateResult.Failure("session-activation-failed-native-restored")
        }
        host.requestLayout()
        return if (session.isLayoutCutoverReady()) {
            activeResult(maskedViews = masked, reused = false)
        } else {
            StateResult.Prepared(
                representedSlots = representedSlots.size,
                reused = false,
            )
        }
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
            "controlCenterPresentation inactive source=" + source +
                " restoredViews=" + restored +
                " nativeTranslationWrites=0 nativeAlphaWrites=0 nativeVisibilityWrites=0",
        )
        eventSink = null
        failNativeSink = null
        readySink = null
        return StateResult.Inactive(restored)
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
            "controlCenterPresentation failNative reason=" + reason +
                " restoredNative=true",
        )
        failNativeSink?.invoke(reason)
        eventSink = null
        failNativeSink = null
        readySink = null
    }

    @Synchronized
    private fun onSessionLayoutReady(
        session: SystemUiCompactPresentationRegistry.Session,
        maskedViews: Int,
        reused: Boolean,
    ) {
        if (current !== session) {
            return
        }
        val active = activeResult(maskedViews = maskedViews, reused = reused)
        readySink?.invoke(active)
    }

    private fun activeResult(
        maskedViews: Int,
        reused: Boolean,
    ): StateResult.Active {
        eventSink?.invoke(
            "controlCenterPresentation active carrier=QS_FAKE.system_icon_area " +
                "representedSlots=" + representedSlots.joinToString(",") +
                " maskedViews=" + maskedViews +
                " slotExclusion=scoped-native-measure-layout " +
                "carrierReservation=status-icons-end-padding " +
                "carrierAuthority=battery_icon_container visualMask=clipBounds " +
                "cutover=native-layout-ready " +
                "nativeTranslationWrites=0 nativeAlphaWrites=0 nativeVisibilityWrites=0",
        )
        return StateResult.Active(
            representedSlots = representedSlots.size,
            maskedViews = maskedViews,
            reused = reused,
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

    internal sealed interface StateResult {
        data class Active(
            val representedSlots: Int,
            val maskedViews: Int,
            val reused: Boolean,
        ) : StateResult

        data class Prepared(
            val representedSlots: Int,
            val reused: Boolean,
        ) : StateResult

        data class Inactive(
            val restoredViews: Int,
        ) : StateResult

        data class Failure(
            val reason: String,
        ) : StateResult
    }
}
