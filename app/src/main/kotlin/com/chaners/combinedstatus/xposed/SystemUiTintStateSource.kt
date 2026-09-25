package com.chaners.combinedstatus.xposed

import android.view.View
import android.widget.TextView
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.util.ArrayList
import java.util.WeakHashMap

internal object SystemUiTintStateSource {
    const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    const val UPDATE_TINT_METHOD_NAME = "updateLightDarkTint"
    const val HOOK_COUNT = 1

    private const val HOOK_ID = "combinedstatus.tint.battery.update"
    private val lastStates = WeakHashMap<View, CombinedStatusTintState>()
    private val firstEventLogged = WeakHashMap<View, Unit>()

    @Volatile
    private var batteryPercentViewField: Field? = null

    @Volatile
    private var lastSourceView: WeakReference<View>? = null

    fun install(
        module: XposedModule,
        classLoader: ClassLoader,
        onTintState: (TintUpdate) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): List<HookHandle> {
        val batteryClass =
            Class.forName(BATTERY_VIEW_CLASS_NAME, false, classLoader)
        val percentField =
            batteryClass.getDeclaredField("mBatteryPercentView")
                .apply { isAccessible = true }
        val updateMethod =
            batteryClass.getDeclaredMethod(
                UPDATE_TINT_METHOD_NAME,
                ArrayList::class.java,
                Float::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
            ).apply { isAccessible = true }

        batteryPercentViewField = percentField

        val handle =
            module
                .hook(updateMethod)
                .setId(HOOK_ID)
                .intercept(
                    Hooker { chain ->
                        val result = chain.proceed()
                        val sourceView = chain.thisObject as? View
                            ?: return@Hooker result
                        val state = readAppliedState(sourceView, percentField)
                            ?: return@Hooker result

                        synchronized(this) {
                            lastStates[sourceView] = state
                            lastSourceView = WeakReference(sourceView)
                        }
                        onTintState(TintUpdate(sourceView, state))

                        val shouldLog =
                            synchronized(this) {
                                firstEventLogged.put(sourceView, Unit) == null
                            }
                        if (shouldLog) {
                            val darkIntensity =
                                (chain.getArg(1) as? Number)?.toFloat()
                                    ?: Float.NaN
                            val lightColor =
                                (chain.getArg(3) as? Number)?.toInt() ?: 0
                            val darkColor =
                                (chain.getArg(4) as? Number)?.toInt() ?: 0
                            val useTint =
                                chain.getArg(5) as? Boolean ?: false
                            onEvent?.invoke(
                                "tintSource receiver=" +
                                    sourceView.javaClass.simpleName +
                                    " batteryApplied=" + colorHex(state.appliedTint) +
                                    " networkFallback=" + colorHex(state.appliedTint) +
                                    " primaryAuthority=battery-anchor-fallback" +
                                    " intensity=" + darkIntensity +
                                    " light=" + colorHex(lightColor) +
                                    " dark=" + colorHex(darkColor) +
                                    " useTint=" + useTint,
                            )
                        }

                        result
                    },
                )

        return listOf(handle)
    }

    fun matches(handle: HookHandle): Boolean = handle.id == HOOK_ID

    @Synchronized
    fun resetRuntimeState() {
        lastStates.clear()
        firstEventLogged.clear()
        batteryPercentViewField = null
        lastSourceView = null
    }

    @Synchronized
    fun currentSourceView(): View? = lastSourceView?.get()

    @Synchronized
    fun currentState(sourceView: View): CombinedStatusTintState? {
        val cached =
            lastStates[sourceView]
                ?.takeIf(CombinedStatusPresentationPolicy::isValidTint)
        val field = batteryPercentViewField
        val refreshed =
            field?.let { percentField ->
                readAppliedState(sourceView, percentField)
            }
        if (
            refreshed != null &&
            CombinedStatusPresentationPolicy.isValidTint(refreshed)
        ) {
            lastStates[sourceView] = refreshed
            return refreshed
        }
        return cached
    }

    private fun readAppliedState(
        sourceView: View,
        percentField: Field,
    ): CombinedStatusTintState? {
        val percentView =
            runCatching {
                percentField.get(sourceView) as? TextView
            }.getOrNull() ?: return null
        return CombinedStatusTintState(
            appliedTint = percentView.currentTextColor,
        )
    }

    private fun colorHex(color: Int): String =
        "#" + color.toUInt().toString(16).padStart(8, '0')

    internal data class TintUpdate(
        val sourceView: View,
        val state: CombinedStatusTintState,
    )
}
