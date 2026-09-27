package com.chaners.combinedstatus.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemUiNotificationShadeTargetProbeTest {
    @Test
    fun captureOnlyOccursWhenEnteringBoundaryBucket() {
        assertEquals(
            true,
            SystemUiNotificationShadeTargetProbe.shouldCaptureBoundary(null, 0),
        )
        assertEquals(
            false,
            SystemUiNotificationShadeTargetProbe.shouldCaptureBoundary(0, 0),
        )
        assertEquals(
            true,
            SystemUiNotificationShadeTargetProbe.shouldCaptureBoundary(0, 1),
        )
        assertEquals(
            false,
            SystemUiNotificationShadeTargetProbe.shouldCaptureBoundary(1, 2),
        )
        assertEquals(
            true,
            SystemUiNotificationShadeTargetProbe.shouldCaptureBoundary(6, 7),
        )
        assertEquals(
            true,
            SystemUiNotificationShadeTargetProbe.shouldCaptureBoundary(7, 8),
        )
    }

    @Test
    fun candidateSelectionIsNarrowAndSemantic() {
        assertEquals(
            true,
            SystemUiNotificationShadeTargetProbe.isCandidateField(
                "statusIconsController",
                "java.lang.Object",
            ),
        )
        assertEquals(
            true,
            SystemUiNotificationShadeTargetProbe.isCandidateField(
                "owner",
                "com.android.systemui.SomeHeaderController",
            ),
        )
        assertEquals(
            false,
            SystemUiNotificationShadeTargetProbe.isCandidateField(
                "configurationController",
                "com.android.systemui.statusbar.policy.ConfigurationController",
            ),
        )
    }
    @Test
    fun daggerLazyUsesGetAccessor() {
        assertEquals(
            "get",
            SystemUiNotificationShadeTargetProbe.lazyAccessorName(
                className = "dagger.internal.DoubleCheck",
                interfaceNames = listOf("dagger.Lazy", "javax.inject.Provider"),
                methodNames = setOf("get", "toString"),
            ),
        )
    }

    @Test
    fun kotlinLazyUsesGetValueAccessor() {
        assertEquals(
            "getValue",
            SystemUiNotificationShadeTargetProbe.lazyAccessorName(
                className = "kotlin.SynchronizedLazyImpl",
                interfaceNames = listOf("kotlin.Lazy"),
                methodNames = setOf("getValue", "isInitialized"),
            ),
        )
    }

    @Test
    fun unrelatedGetMethodIsNotInvokedAsLazy() {
        assertEquals(
            null,
            SystemUiNotificationShadeTargetProbe.lazyAccessorName(
                className = "com.android.systemui.SomeController",
                interfaceNames = emptyList(),
                methodNames = setOf("get"),
            ),
        )
    }
}
