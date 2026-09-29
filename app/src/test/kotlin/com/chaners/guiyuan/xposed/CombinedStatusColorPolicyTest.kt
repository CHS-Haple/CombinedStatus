package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.CombinedStatusVisualSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class CombinedStatusColorPolicyTest {
    @Test
    fun normalUsesResolvedStatusIconTintAcrossLayers() {
        val colors =
            CombinedStatusColorPolicy.resolve(
                model = model(),
                tintState =
                    CombinedStatusTintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = 0xff445566.toInt(),
                    ),
            )
        assertEquals(0xff445566.toInt(), colors.centerTint)
        assertEquals(0xff445566.toInt(), colors.mobileTint)
        assertEquals(0xff445566.toInt(), colors.batteryTint)
    }

    @Test
    fun chargingUsesNativeSystemSemanticColorByDefault() {
        val semanticColor = 0xff1dcd3a.toInt()
        val colors =
            CombinedStatusColorPolicy.resolve(
                model =
                    model(
                        state = CombinedStatusBatterySemanticState.CHARGING,
                        systemColor = semanticColor,
                    ),
                tintState =
                    CombinedStatusTintState(
                        appliedTint = 0xffddeeff.toInt(),
                        statusIconTint = 0xff556677.toInt(),
                    ),
            )
        assertEquals(0xff556677.toInt(), colors.centerTint)
        assertEquals(0xff556677.toInt(), colors.mobileTint)
        assertEquals(semanticColor, colors.batteryTint)
    }

    @Test
    fun modeColorOnlyChangesBatteryByDefault() {
        val semanticColor = 0xff3482ff.toInt()
        val colors =
            CombinedStatusColorPolicy.resolve(
                model =
                    model(
                        state = CombinedStatusBatterySemanticState.PERFORMANCE,
                        systemColor = semanticColor,
                    ),
                tintState =
                    CombinedStatusTintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = 0xff445566.toInt(),
                    ),
            )
        assertEquals(0xff445566.toInt(), colors.centerTint)
        assertEquals(0xff445566.toInt(), colors.mobileTint)
        assertEquals(semanticColor, colors.batteryTint)
    }

    @Test
    fun optionalLinksConsumeFinalBatteryColor() {
        val semanticColor = 0xffff9f05.toInt()
        val colors =
            CombinedStatusColorPolicy.resolve(
                model =
                    model(
                        state = CombinedStatusBatterySemanticState.POWER_SAVE,
                        systemColor = semanticColor,
                    ),
                tintState =
                    CombinedStatusTintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = 0xff445566.toInt(),
                    ),
                visualSettings =
                    CombinedStatusVisualSettings(
                        mobileFollowsBatteryColor = true,
                        centerFollowsBatteryColor = true,
                    ),
            )
        assertEquals(semanticColor, colors.centerTint)
        assertEquals(semanticColor, colors.mobileTint)
        assertEquals(semanticColor, colors.batteryTint)
    }

    @Test
    fun invalidStatusIconTintFallsBackToBatteryAnchorTint() {
        val colors =
            CombinedStatusColorPolicy.resolve(
                model = model(),
                tintState =
                    CombinedStatusTintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = 0x00112233,
                    ),
            )
        assertEquals(0xff112233.toInt(), colors.centerTint)
        assertEquals(0xff112233.toInt(), colors.mobileTint)
        assertEquals(0xff112233.toInt(), colors.batteryTint)
    }

    private fun model(
        state: CombinedStatusBatterySemanticState =
            CombinedStatusBatterySemanticState.NORMAL,
        systemColor: Int? = null,
    ) =
        CombinedStatusRenderModel(
            batteryPercent = 80,
            charging = state == CombinedStatusBatterySemanticState.CHARGING,
            centerIndicator =
                CenterIndicator.Wifi(
                    segments = 3,
                    internet = InternetState.VALIDATED,
                ),
            mobileLevel = 4,
            effectiveDataSubscriptionId = 1,
            batterySemanticState = state,
            batterySystemSemanticColor = systemColor,
        )
}
