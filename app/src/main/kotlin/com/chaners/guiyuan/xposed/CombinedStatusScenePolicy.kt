package com.chaners.guiyuan.xposed

import android.view.View

internal data class CombinedStatusTransitionSourceWitness(
    val renderView: View,
    val logicalLeftPx: Int,
    val logicalTopPx: Int,
    val logicalWidthPx: Int,
    val logicalHeightPx: Int,
    val positionHost: View,
    val motionCarrier: View,
    val representedSlots: Set<String>,
)

internal enum class CombinedStatusScene {
    HOME_STABLE,
    NOTIFICATION_SHADE_TRANSITION,
    CONTROL_CENTER,
    KEYGUARD,
    AOD,
}

internal enum class CombinedStatusSceneEvidence {
    RUNTIME_VERIFIED,
    STATIC_VERIFIED,
}

internal enum class CombinedStatusSourceScene {
    HOME,
    KEYGUARD,
    UNKNOWN,
}

internal data class CombinedStatusSceneCapability(
    val scene: CombinedStatusScene,
    val renderMode: CombinedStatusRenderMode,
    val motionOwnership: CombinedStatusMotionOwnership,
    val evidence: CombinedStatusSceneEvidence,
)

internal object CombinedStatusScenePolicy {
    private val capabilities =
        mapOf(
            CombinedStatusScene.HOME_STABLE to
                CombinedStatusSceneCapability(
                    scene = CombinedStatusScene.HOME_STABLE,
                    renderMode = CombinedStatusRenderMode.PROJECTED,
                    motionOwnership = CombinedStatusMotionOwnership.NONE,
                    evidence = CombinedStatusSceneEvidence.RUNTIME_VERIFIED,
                ),
            CombinedStatusScene.NOTIFICATION_SHADE_TRANSITION to
                CombinedStatusSceneCapability(
                    scene = CombinedStatusScene.NOTIFICATION_SHADE_TRANSITION,
                    renderMode = CombinedStatusRenderMode.NATIVE_ONLY,
                    motionOwnership = CombinedStatusMotionOwnership.SYSTEM_UI,
                    evidence = CombinedStatusSceneEvidence.STATIC_VERIFIED,
                ),
            CombinedStatusScene.CONTROL_CENTER to
                CombinedStatusSceneCapability(
                    scene = CombinedStatusScene.CONTROL_CENTER,
                    renderMode = CombinedStatusRenderMode.NATIVE_ONLY,
                    motionOwnership = CombinedStatusMotionOwnership.SYSTEM_UI,
                    evidence = CombinedStatusSceneEvidence.STATIC_VERIFIED,
                ),
            CombinedStatusScene.KEYGUARD to
                CombinedStatusSceneCapability(
                    scene = CombinedStatusScene.KEYGUARD,
                    renderMode = CombinedStatusRenderMode.PROJECTED,
                    motionOwnership = CombinedStatusMotionOwnership.SYSTEM_UI,
                    evidence = CombinedStatusSceneEvidence.STATIC_VERIFIED,
                ),
            CombinedStatusScene.AOD to
                CombinedStatusSceneCapability(
                    scene = CombinedStatusScene.AOD,
                    renderMode = CombinedStatusRenderMode.PROJECTED,
                    motionOwnership = CombinedStatusMotionOwnership.SYSTEM_UI,
                    evidence = CombinedStatusSceneEvidence.STATIC_VERIFIED,
                ),
        )

    fun capability(scene: CombinedStatusScene): CombinedStatusSceneCapability =
        requireNotNull(capabilities[scene]) {
            "Missing CombinedStatus scene capability: $scene"
        }

    fun all(): List<CombinedStatusSceneCapability> =
        CombinedStatusScene.entries.map(::capability)

    fun shouldAcquireKeyguardControlCenterLease(
        sourceScene: CombinedStatusSourceScene,
        keyguardRuntimeReady: Boolean,
        nativeFraction: Float,
    ): Boolean =
        sourceScene == CombinedStatusSourceScene.KEYGUARD &&
            keyguardRuntimeReady &&
            nativeFraction > 0f

    fun shouldRetainKeyguardControlCenterLease(
        leaseActive: Boolean,
        sourceScene: CombinedStatusSourceScene,
        featureEnabled: Boolean,
        keyguardEnabled: Boolean,
        hostAttached: Boolean,
        aodBlocked: Boolean,
        nativeFraction: Float,
    ): Boolean =
        leaseActive &&
            sourceScene == CombinedStatusSourceScene.KEYGUARD &&
            featureEnabled &&
            keyguardEnabled &&
            hostAttached &&
            !aodBlocked &&
            nativeFraction > 0f

    fun retainedTransitionSourceWitnessAvailable(
        widthPx: Int,
        heightPx: Int,
        hostAttached: Boolean,
    ): Boolean =
        widthPx > 0 &&
            heightPx > 0 &&
            hostAttached

    fun aodProjectionEligible(
        featureEnabled: Boolean,
        aodEnabled: Boolean,
        stableAod: Boolean,
        homeTransitionPrearm: Boolean = false,
    ): Boolean =
        featureEnabled &&
            aodEnabled &&
            (stableAod || homeTransitionPrearm) &&
            capability(CombinedStatusScene.AOD).renderMode ==
                CombinedStatusRenderMode.PROJECTED

    enum class KeyguardAodProjection {
        NATIVE,
        KEYGUARD,
        AOD,
    }

    fun resolveKeyguardAodProjection(
        featureEnabled: Boolean,
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
        toAod: Boolean,
        isAodAnimate: Boolean,
        steadySourceScene: CombinedStatusSourceScene = CombinedStatusSourceScene.UNKNOWN,
        homePresentationOwned: Boolean = false,
        keyguardPresentationOwned: Boolean = false,
        aodPresentationOwned: Boolean = false,
    ): KeyguardAodProjection {
        if (!featureEnabled) return KeyguardAodProjection.NATIVE
        if (isAodAnimate) {
            return resolveAnimatingKeyguardAodProjection(
                keyguardEnabled = keyguardEnabled,
                aodEnabled = aodEnabled,
                steadySourceScene = steadySourceScene,
                homePresentationOwned = homePresentationOwned,
                keyguardPresentationOwned = keyguardPresentationOwned,
                aodPresentationOwned = aodPresentationOwned,
            )
        }
        if (
            SystemUiKeyguardAodStateSource.isStableAod(
                toAod = toAod,
                isAodAnimate = isAodAnimate,
            )
        ) {
            return if (aodEnabled) {
                KeyguardAodProjection.AOD
            } else {
                KeyguardAodProjection.NATIVE
            }
        }
        if (
            SystemUiKeyguardAodStateSource.blocksKeyguardProjection(
                toAod = toAod,
                isAodAnimate = isAodAnimate,
                animToAod = null,
            )
        ) {
            return KeyguardAodProjection.NATIVE
        }
        if (steadySourceScene == CombinedStatusSourceScene.HOME) {
            return KeyguardAodProjection.NATIVE
        }
        return if (
            steadySourceScene == CombinedStatusSourceScene.KEYGUARD &&
            keyguardEnabled
        ) {
            KeyguardAodProjection.KEYGUARD
        } else {
            KeyguardAodProjection.NATIVE
        }
    }

    internal fun resolveAnimatingKeyguardAodProjection(
        keyguardEnabled: Boolean,
        aodEnabled: Boolean,
        steadySourceScene: CombinedStatusSourceScene,
        homePresentationOwned: Boolean,
        keyguardPresentationOwned: Boolean,
        aodPresentationOwned: Boolean,
    ): KeyguardAodProjection {
        if (aodPresentationOwned) {
            return if (aodEnabled) {
                KeyguardAodProjection.AOD
            } else {
                KeyguardAodProjection.NATIVE
            }
        }
        if (
            steadySourceScene == CombinedStatusSourceScene.KEYGUARD &&
            keyguardPresentationOwned
        ) {
            return if (aodEnabled) {
                KeyguardAodProjection.KEYGUARD
            } else {
                KeyguardAodProjection.NATIVE
            }
        }
        if (
            steadySourceScene == CombinedStatusSourceScene.HOME &&
            homePresentationOwned &&
            aodEnabled
        ) {
            return KeyguardAodProjection.AOD
        }
        return KeyguardAodProjection.NATIVE
    }

    fun controlCenterProjectionEligible(
        featureEnabled: Boolean,
        sourceScene: CombinedStatusSourceScene,
        keyguardEnabled: Boolean,
    ): Boolean =
        featureEnabled &&
            when (sourceScene) {
            CombinedStatusSourceScene.HOME ->
                capability(CombinedStatusScene.HOME_STABLE).renderMode ==
                    CombinedStatusRenderMode.PROJECTED

            CombinedStatusSourceScene.KEYGUARD ->
                keyguardEnabled &&
                    capability(CombinedStatusScene.KEYGUARD).renderMode ==
                    CombinedStatusRenderMode.PROJECTED

            CombinedStatusSourceScene.UNKNOWN -> false
        }
}
