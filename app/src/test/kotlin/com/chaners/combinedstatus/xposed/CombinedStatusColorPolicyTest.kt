package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class CombinedStatusColorPolicyTest {
    @Test
    fun nonChargingUsesSystemTintEverywhere() {
        val colors =
            CombinedStatusColorPolicy.resolve(
                model = model(charging = false),
                tintState = CombinedStatusTintState(appliedTint = 0xff112233.toInt()),
            )

        assertEquals(0xff112233.toInt(), colors.primaryTint)
        assertEquals(0xff112233.toInt(), colors.batteryTint)
    }

    @Test
    fun chargingOnlyOverridesBatteryTint() {
        val colors =
            CombinedStatusColorPolicy.resolve(
                model = model(charging = true),
                tintState = CombinedStatusTintState(appliedTint = 0xffddeeff.toInt()),
            )

        assertEquals(0xffddeeff.toInt(), colors.primaryTint)
        assertEquals(CombinedStatusColorPolicy.CHARGING_TINT, colors.batteryTint)
    }

    private fun model(charging: Boolean) =
        CombinedStatusRenderModel(
            batteryPercent = 80,
            charging = charging,
            wifiSegments = 3,
            mobileLevel = 4,
            mobileSubscriptionId = 1,
        )
}
