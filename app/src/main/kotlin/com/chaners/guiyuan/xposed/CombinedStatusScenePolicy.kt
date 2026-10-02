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

    fun shouldReconcileControlCenterForKeyguardLifecycle(
        controlCenterVisible: Boolean,
        nativeFraction: Float,
        leaseActive: Boolean,
    ): Boolean =
        controlCenterVisible ||
            nativeFraction > 0f ||
            leaseActive

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

    enum class StableKeyguardAodScene {
        UNKNOWN,
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
        lastStableFamilyScene: StableKeyguardAodScene = StableKeyguardAodScene.UNKNOWN,
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
                lastStableFamilyScene = lastStableFamilyScene,
                homePresentationOwned = homePresentationOwned,
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
            steadySourceScene == CombinedStatusSourceScene.KEYGUARD &&
            !keyguardEnabled &&
            aodEnabled &&
            homePresentationOwned
        ) {
            // Home -> AOD crosses a short native KEYGUARD source interval on the
            // pinned HyperOS target. When Keyguard projection is intentionally
            // disabled, keep the still-owned Home presentation continuous by
            // prearming the enabled AOD family owner instead of exposing native
            // represented icons until the AOD animation callback catches up.
            return KeyguardAodProjection.AOD
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
        lastStableFamilyScene: StableKeyguardAodScene,
        homePresentationOwned: Boolean,
    ): KeyguardAodProjection {
        if (steadySourceScene == CombinedStatusSourceScene.HOME) {
            return if (homePresentationOwned && aodEnabled) {
                // Home -> AOD is the only transition without a prior stable
                // Keyguard/AOD child. Keep the verified Home presentation as
                // the source while prearming the enabled AOD child.
                KeyguardAodProjection.AOD
            } else {
                KeyguardAodProjection.NATIVE
            }
        }

        if (
            steadySourceScene == CombinedStatusSourceScene.KEYGUARD &&
            homePresentationOwned &&
            aodEnabled
        ) {
            // HyperOS briefly exposes KEYGUARD ancestry while Home still owns
            // the visible source on Home -> AOD. Home ownership is stronger
            // evidence than family history for this one boundary.
            return KeyguardAodProjection.AOD
        }

        // Do not derive AOD animation direction from current presentation
        // ownership. Attach/cleanup mutates ownership itself and Build 645
        // proved that doing so creates KEYGUARD -> NATIVE -> KEYGUARD
        // oscillation on repeated callbacks. Direction is instead derived from
        // the last non-animating, runtime-observed family scene and remains
        // frozen for the whole animation.
        return when (lastStableFamilyScene) {
            StableKeyguardAodScene.KEYGUARD ->
                if (aodEnabled) {
                    KeyguardAodProjection.AOD
                } else {
                    KeyguardAodProjection.NATIVE
                }

            StableKeyguardAodScene.AOD ->
                if (keyguardEnabled) {
                    KeyguardAodProjection.KEYGUARD
                } else {
                    KeyguardAodProjection.NATIVE
                }

            StableKeyguardAodScene.UNKNOWN ->
                if (
                    steadySourceScene == CombinedStatusSourceScene.KEYGUARD &&
                    keyguardEnabled
                ) {
                    // Cold-start / late-install fallback: an authoritative
                    // Keyguard source may acquire only the enabled Keyguard
                    // child. No cross-child inference is made.
                    KeyguardAodProjection.KEYGUARD
                } else {
                    KeyguardAodProjection.NATIVE
                }
        }
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
