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
        keyguardStatusIconsAlpha: Float? = null,
        nativeToLockScreenTarget: Boolean? = null,
        fullAodTargetSourceReady: Boolean = false,
    ): KeyguardAodProjection {
        if (!featureEnabled) return KeyguardAodProjection.NATIVE
        if (isAodAnimate) {
            return resolveAnimatingKeyguardAodProjection(
                keyguardEnabled = keyguardEnabled,
                aodEnabled = aodEnabled,
                steadySourceScene = steadySourceScene,
                lastStableFamilyScene = lastStableFamilyScene,
                homePresentationOwned = homePresentationOwned,
                keyguardStatusIconsAlpha = keyguardStatusIconsAlpha,
                nativeToLockScreenTarget = nativeToLockScreenTarget,
                fullAodTargetSourceReady = fullAodTargetSourceReady,
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
        lastStableFamilyScene: StableKeyguardAodScene,
        homePresentationOwned: Boolean,
        keyguardStatusIconsAlpha: Float? = null,
        nativeToLockScreenTarget: Boolean? = null,
        fullAodTargetSourceReady: Boolean = false,
    ): KeyguardAodProjection {
        if (steadySourceScene == CombinedStatusSourceScene.HOME) {
            return if (
                lastStableFamilyScene == StableKeyguardAodScene.UNKNOWN &&
                homePresentationOwned &&
                aodEnabled
            ) {
                // Home -> AOD is the only transition without a prior stable
                // Keyguard/AOD child. A latched AOD/Keyguard origin instead
                // means we are leaving that family for Home and must not
                // reverse-prearm AOD during the outgoing animation.
                KeyguardAodProjection.AOD
            } else {
                KeyguardAodProjection.NATIVE
            }
        }

        if (
            steadySourceScene == CombinedStatusSourceScene.KEYGUARD &&
            lastStableFamilyScene == StableKeyguardAodScene.UNKNOWN &&
            homePresentationOwned &&
            aodEnabled
        ) {
            // Home -> AOD can briefly expose KEYGUARD ancestry before the AOD
            // callback catches up. Only an UNKNOWN family origin may use Home
            // ownership as prearm evidence. A latched AOD/Keyguard origin is
            // stronger and must not be overridden by stale Home ownership.
            return KeyguardAodProjection.AOD
        }

        // On the pinned HyperOS target, animateFullAod commits
        // MiuiKeyguardStatusBarView.mToLockScreen before driving the native
        // Battery/status-icon animation. Once that exact-target event source is
        // available, a single enabled family child follows the persistent
        // native target directly. This prevents the earlier isAodAnimate edge
        // from cutting over too soon and avoids waiting for animation-end
        // callbacks on the reverse direction.
        if (
            fullAodTargetSourceReady &&
            nativeToLockScreenTarget != null &&
            steadySourceScene == CombinedStatusSourceScene.KEYGUARD &&
            lastStableFamilyScene != StableKeyguardAodScene.UNKNOWN &&
            keyguardEnabled != aodEnabled
        ) {
            return if (nativeToLockScreenTarget) {
                if (keyguardEnabled) {
                    KeyguardAodProjection.KEYGUARD
                } else {
                    KeyguardAodProjection.NATIVE
                }
            } else {
                if (aodEnabled) {
                    KeyguardAodProjection.AOD
                } else {
                    KeyguardAodProjection.NATIVE
                }
            }
        }

        // Single-child handoff follows the native Keyguard status-icons
        // layer in both directions. Alpha 1 belongs to the Keyguard endpoint;
        // alpha 0 belongs to the AOD endpoint. Battery AOD alpha is a separate
        // native animation and is not a whole-combined-scene lifetime signal.
        if (
            keyguardEnabled &&
            !aodEnabled &&
            steadySourceScene == CombinedStatusSourceScene.KEYGUARD
        ) {
            val alpha = keyguardStatusIconsAlpha
            if (alpha != null) {
                when (lastStableFamilyScene) {
                    StableKeyguardAodScene.KEYGUARD ->
                        if (alpha < 1f) return KeyguardAodProjection.NATIVE

                    StableKeyguardAodScene.AOD ->
                        return if (alpha > 0f) {
                            KeyguardAodProjection.KEYGUARD
                        } else {
                            KeyguardAodProjection.NATIVE
                        }

                    StableKeyguardAodScene.UNKNOWN -> Unit
                }
            }
        }
        if (
            !keyguardEnabled &&
            aodEnabled &&
            steadySourceScene == CombinedStatusSourceScene.KEYGUARD
        ) {
            val alpha = keyguardStatusIconsAlpha
            if (alpha != null) {
                when (lastStableFamilyScene) {
                    StableKeyguardAodScene.AOD ->
                        if (alpha > 0f) return KeyguardAodProjection.NATIVE

                    StableKeyguardAodScene.KEYGUARD ->
                        return if (alpha < 1f) {
                            KeyguardAodProjection.AOD
                        } else {
                            KeyguardAodProjection.NATIVE
                        }

                    StableKeyguardAodScene.UNKNOWN -> Unit
                }
            }
        }

        // Do not derive AOD animation direction from current presentation
        // ownership. Attach/cleanup mutates ownership itself and Build 645
        // proved that doing so creates KEYGUARD -> NATIVE -> KEYGUARD
        // oscillation on repeated callbacks. Direction is instead derived from
        // the last non-animating, runtime-observed family scene and remains
        // frozen for the whole animation.
        return when (lastStableFamilyScene) {
            StableKeyguardAodScene.KEYGUARD ->
                when {
                    aodEnabled -> KeyguardAodProjection.AOD
                    keyguardEnabled -> KeyguardAodProjection.KEYGUARD
                    else -> KeyguardAodProjection.NATIVE
                }

            StableKeyguardAodScene.AOD ->
                when {
                    keyguardEnabled -> KeyguardAodProjection.KEYGUARD
                    aodEnabled -> KeyguardAodProjection.AOD
                    else -> KeyguardAodProjection.NATIVE
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

    fun resolveControlCenterSourceScene(
        panelSourceScene: CombinedStatusSourceScene,
        steadySourceScene: CombinedStatusSourceScene,
        lastStableFamilyScene: StableKeyguardAodScene = StableKeyguardAodScene.UNKNOWN,
    ): CombinedStatusSourceScene {
        if (panelSourceScene == steadySourceScene) return panelSourceScene
        if (panelSourceScene == CombinedStatusSourceScene.UNKNOWN) return steadySourceScene
        if (steadySourceScene == CombinedStatusSourceScene.UNKNOWN) return panelSourceScene

        // A HOME/KEYGUARD disagreement is a lifecycle-boundary race between two
        // native witnesses. Family history provides direction without borrowing
        // mutable presentation ownership:
        // - a latched family child means Keyguard/AOD is the outgoing side, so
        //   HOME is the unlock target;
        // - UNKNOWN means stable Home was the prior family state, so KEYGUARD
        //   is the lock/AOD target.
        return if (lastStableFamilyScene == StableKeyguardAodScene.UNKNOWN) {
            CombinedStatusSourceScene.KEYGUARD
        } else {
            CombinedStatusSourceScene.HOME
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
