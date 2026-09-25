package com.chaners.combinedstatus.xposed

import com.chaners.combinedstatus.settings.CombinedStatusVisualSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class CombinedStatusColorPolicyTest {
    @Test
    fun defaultUsesResolvedStatusIconTintAcrossNonChargingLayers() {
        val colors =
            CombinedStatusColorPolicy.resolve(
                model = model(charging = false),
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
    fun chargingUsesResolvedHyperOsTintWhenAvailable() {
        val colors =
            CombinedStatusColorPolicy.resolve(
                model =
                    model(
                        charging = true,
                        batteryModeTint = 0xff22aa55.toInt(),
                    ),
                tintState =
                    CombinedStatusTintState(
                        appliedTint = 0xffddeeff.toInt(),
                        statusIconTint = 0xff556677.toInt(),
                    ),
            )

        assertEquals(0xff556677.toInt(), colors.centerTint)
        assertEquals(0xff556677.toInt(), colors.mobileTint)
        assertEquals(0xff22aa55.toInt(), colors.batteryTint)
    }

    @Test
    fun mobileLinkUsesFinalBatteryTintForDotsAndUnavailableMarkLayer() {
        val colors =
            CombinedStatusColorPolicy.resolve(
                model =
                    model(
                        charging = true,
                        batteryModeTint = 0xff22aa55.toInt(),
                    ),
                tintState =
                    CombinedStatusTintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = 0xff445566.toInt(),
                    ),
                visualSettings =
                    CombinedStatusVisualSettings(
                        mobileFollowsBatteryColor = true,
                    ),
            )

        assertEquals(0xff445566.toInt(), colors.centerTint)
        assertEquals(0xff22aa55.toInt(), colors.mobileTint)
        assertEquals(0xff22aa55.toInt(), colors.batteryTint)
    }

    @Test
    fun centerLinkUsesFinalBatteryTintForEveryCenterFamily() {
        val colors =
            CombinedStatusColorPolicy.resolve(
                model = model(charging = true),
                tintState =
                    CombinedStatusTintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = 0xff445566.toInt(),
                    ),
                visualSettings =
                    CombinedStatusVisualSettings(
                        centerFollowsBatteryColor = true,
                    ),
            )

        assertEquals(0xff22aa55.toInt(), colors.centerTint)
        assertEquals(0xff445566.toInt(), colors.mobileTint)
        assertEquals(0xff22aa55.toInt(), colors.batteryTint)
    }

    @Test
    fun bothLinksUseTheSameResolvedBatteryTint() {
        val colors =
            CombinedStatusColorPolicy.resolve(
                model = model(charging = false),
                tintState =
                    CombinedStatusTintState(
                        appliedTint = 0xff102030.toInt(),
                        statusIconTint = 0xff405060.toInt(),
                    ),
                visualSettings =
                    CombinedStatusVisualSettings(
                        mobileFollowsBatteryColor = true,
                        centerFollowsBatteryColor = true,
                    ),
            )

        assertEquals(0xff405060.toInt(), colors.centerTint)
        assertEquals(0xff405060.toInt(), colors.mobileTint)
        assertEquals(0xff405060.toInt(), colors.batteryTint)
    }

    @Test
    fun powerSaveUsesRuntimeResolvedHyperOsTint() {
        val colors =
            CombinedStatusColorPolicy.resolve(
                model =
                    model(
                        charging = false,
                        batteryModeTint = 0xffaa7700.toInt(),
                    ),
                tintState =
                    CombinedStatusTintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = 0xff445566.toInt(),
                    ),
            )

        assertEquals(0xffaa7700.toInt(), colors.batteryTint)
        assertEquals(0xff445566.toInt(), colors.centerTint)
        assertEquals(0xff445566.toInt(), colors.mobileTint)
    }

    @Test
    fun missingNonChargingModeTintFailsNative() {
        val nativeTint = 0xff445566.toInt()
        val colors =
            CombinedStatusColorPolicy.resolve(
                model =
                    model(
                        charging = false,
                        batteryModeTint = null,
                    ),
                tintState =
                    CombinedStatusTintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = nativeTint,
                    ),
            )

        assertEquals(nativeTint, colors.batteryTint)
    }

    @Test
    fun invalidResolvedNetworkTintFallsBackToBatteryAnchor() {
        val colors =
            CombinedStatusColorPolicy.resolve(
                model = model(charging = false),
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
        charging: Boolean,
        batteryModeTint: Int? = null,
    ) =
        CombinedStatusRenderModel(
            batteryPercent = 80,
            charging = charging,
            batteryModeTint = batteryModeTint,
            centerIndicator =
                CenterIndicator.Wifi(
                    segments = 3,
                    internet = InternetState.VALIDATED,
                ),
            mobileLevel = 4,
            effectiveDataSubscriptionId = 1,
        )
}
