package com.chaners.combinedstatus.xposed

import android.os.Handler
import android.os.Looper
import android.view.View
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

internal object NativeParticipantShadowSession {
    private const val SLOT = "combined_status_shadow"
    private const val CONTENT_DESCRIPTION = "CombinedStatus native shadow"
    private const val MAIN_THREAD_TIMEOUT_MS = 1_000L
    private const val POST_LAYOUT_VALIDATION_DELAY_MS = 48L

    private var current: Session? = null

    @Synchronized
    fun attach(
        host: Any,
        onEvent: (String) -> Unit,
    ): AttachResult {
        val resolution = NativeParticipantRuntimeAccess.resolve(host)
        val handles =
            when (resolution) {
                is NativeParticipantRuntimeAccess.ResolveResult.Ready ->
                    resolution.handles

                is NativeParticipantRuntimeAccess.ResolveResult.Failure ->
                    return AttachResult.Failure(resolution.reason)
            }

        val existing = current
        if (existing?.matches(handles) == true) {
            val verified = existing.verify(requireLayoutHidden = true)
            if (verified is AttachResult.Ready) {
                return verified
            }
            current = null
            when (val stopped = existing.stop()) {
                DetachResult.Removed,
                DetachResult.AlreadyDetached -> Unit

                is DetachResult.Failure ->
                    return AttachResult.Failure(
                        "existing-shadow-cleanup-" + stopped.reason,
                    )
            }
        } else if (existing != null) {
            current = null
            when (val stopped = existing.stop()) {
                DetachResult.Removed,
                DetachResult.AlreadyDetached -> Unit

                is DetachResult.Failure ->
                    return AttachResult.Failure(
                        "previous-shadow-cleanup-" + stopped.reason,
                    )
            }
        }

        val session = Session(handles, onEvent)
        val result = session.start()
        current =
            if (result is AttachResult.Ready) {
                session
            } else {
                null
            }
        return result
    }

    @Synchronized
    fun detach(): DetachResult {
        val session = current ?: return DetachResult.AlreadyDetached
        current = null
        return session.stop()
    }

    @Synchronized
    private fun invalidate(session: Session) {
        if (current === session) {
            current = null
        }
    }

    private class Session(
        private val handles: NativeParticipantRuntimeAccess.Handles,
        private val onEvent: (String) -> Unit,
    ) {
        private val setter =
            NativeParticipantRuntimeAccess.resourceSetter(
                handles.controller.javaClass,
            )
        private val visibility =
            NativeParticipantRuntimeAccess.visibilityMethod(
                handles.controller.javaClass,
            )
        private val removal =
            NativeParticipantRuntimeAccess.removal(
                handles.controller.javaClass,
            )

        private var snapshot: Snapshot? = null
        private var root: View? = null
        private var stopped = false

        private val postLayoutValidation =
            Runnable {
                if (stopped) {
                    return@Runnable
                }

                runCatching {
                    when (val result = verifyOnMain(requireLayoutHidden = true)) {
                        is AttachResult.Ready -> {
                            val verified = result.snapshot
                            onEvent(
                                "nativeParticipantShadow postLayout " +
                                    "slot=" + SLOT +
                                    " state=ready" +
                                    " visibility=" + verified.rootVisibility +
                                    " iconVisible=" + verified.iconVisible +
                                    " measured=" + verified.measuredWidth +
                                    "x" + verified.measuredHeight +
                                    " layoutHidden=" + verified.layoutHidden +
                                    " nativeGeometryWrites=0",
                            )
                        }

                        is AttachResult.Failure -> {
                            cleanupAfterValidationFailure(result.reason)
                        }
                    }
                }.onFailure { error ->
                    cleanupAfterValidationFailure(
                        "shadow-post-layout-exception-" +
                            (error.message ?: error.javaClass.simpleName),
                    )
                }
            }

        fun matches(
            candidate: NativeParticipantRuntimeAccess.Handles,
        ): Boolean =
            handles.controller === candidate.controller &&
                handles.group === candidate.group

        fun start(): AttachResult =
            runCatching {
                runOnMainBlocking {
                    startOnMain()
                }
            }.getOrElse { error ->
                runCatching {
                    runOnMainBlocking {
                        cleanupOnMain()
                    }
                }
                AttachResult.Failure(
                    "shadow-attach-" +
                        (error.message ?: error.javaClass.simpleName),
                )
            }

        fun verify(requireLayoutHidden: Boolean): AttachResult =
            runCatching {
                runOnMainBlocking {
                    verifyOnMain(requireLayoutHidden)
                }
            }.getOrElse { error ->
                AttachResult.Failure(
                    "shadow-verify-" +
                        (error.message ?: error.javaClass.simpleName),
                )
            }

        fun stop(): DetachResult =
            runCatching {
                runOnMainBlocking {
                    stopped = true
                    root?.removeCallbacks(postLayoutValidation)
                    cleanupOnMain()
                    root = null
                    val remaining =
                        NativeParticipantRuntimeAccess.findSlotView(
                            handles.group,
                            SLOT,
                        )
                    if (remaining == null) {
                        onEvent(
                            "nativeParticipantShadow detached " +
                                "slot=" + SLOT +
                                " cleanup=verified nativeGeometryWrites=0",
                        )
                        DetachResult.Removed
                    } else {
                        DetachResult.Failure(
                            "shadow-root-remains index=" +
                                handles.group.indexOfChild(remaining),
                        )
                    }
                }
            }.getOrElse { error ->
                DetachResult.Failure(
                    "shadow-detach-" +
                        (error.message ?: error.javaClass.simpleName),
                )
            }

        private fun startOnMain(): AttachResult {
            val activeSetter =
                setter
                    ?: return AttachResult.Failure(
                        "resource-setter-missing",
                    )
            val activeVisibility =
                visibility
                    ?: return AttachResult.Failure(
                        "visibility-method-missing",
                    )
            removal
                ?: return AttachResult.Failure(
                    "removal-method-missing",
                )

            val preExistingSlot =
                NativeParticipantRuntimeAccess.findSlotView(
                    handles.group,
                    SLOT,
                ) != null
            cleanupOnMain()
            val residualAfterCleanup =
                NativeParticipantRuntimeAccess.findSlotView(
                    handles.group,
                    SLOT,
                )
            if (residualAfterCleanup != null) {
                return AttachResult.Failure(
                    "shadow-pre-attach-cleanup-failed index=" +
                        handles.group.indexOfChild(residualAfterCleanup),
                )
            }

            val bootstrap =
                NativeParticipantRuntimeAccess.findBootstrapResource(
                    handles.group,
                )
                    ?: return AttachResult.Failure(
                        "bootstrap-systemui-resource-missing",
                    )

            val childrenBefore = handles.group.childCount

            NativeParticipantRuntimeAccess.invokeCreate(
                handles = handles,
                setter = activeSetter,
                slot = SLOT,
                resourceId = bootstrap.resourceId,
                contentDescription = CONTENT_DESCRIPTION,
            )
            NativeParticipantRuntimeAccess.invokeVisibility(
                handles = handles,
                method = activeVisibility,
                slot = SLOT,
                visible = false,
            )

            val createdRoot =
                NativeParticipantRuntimeAccess.findSlotView(
                    handles.group,
                    SLOT,
                )
                    ?: run {
                        cleanupOnMain()
                        return AttachResult.Failure(
                            "shadow-root-not-created",
                        )
                    }

            val iconVisible =
                NativeParticipantRuntimeAccess.iconVisible(createdRoot)
            if (!NativeParticipantShadowPolicy.isSemanticallyHidden(iconVisible)) {
                cleanupOnMain()
                return AttachResult.Failure(
                    "shadow-semantic-hide-not-applied iconVisible=" +
                        (iconVisible?.toString() ?: "unknown"),
                )
            }

            root = createdRoot
            stopped = false

            val resolved =
                snapshotOf(
                    root = createdRoot,
                    childrenBefore = childrenBefore,
                    bootstrap = bootstrap,
                    iconVisible = iconVisible,
                    preExistingSlot = preExistingSlot,
                )
            snapshot = resolved

            createdRoot.removeCallbacks(postLayoutValidation)
            check(
                createdRoot.postDelayed(
                    postLayoutValidation,
                    POST_LAYOUT_VALIDATION_DELAY_MS,
                ),
            ) {
                "shadow-post-layout-validation-rejected"
            }

            onEvent(
                "nativeParticipantShadow attached " +
                    "slot=" + SLOT +
                    " root=" + resolved.rootClass +
                    " index=" + resolved.rootIndex +
                    " visibility=" + resolved.rootVisibility +
                    " iconVisible=" + resolved.iconVisible +
                    " measured=" + resolved.measuredWidth +
                    "x" + resolved.measuredHeight +
                    " layoutHidden=" + resolved.layoutHidden +
                    " children=" + resolved.childrenBefore +
                    "->" + resolved.childrenAfter +
                    " preExistingSlot=" + resolved.preExistingSlot +
                    " preAttachCleanup=verified" +
                    " bootstrapRes=0x" +
                    resolved.bootstrapResourceId.toUInt().toString(16) +
                    " bootstrapSlot=" + (resolved.bootstrapSourceSlot ?: "unknown") +
                    " setter=" + resolved.creationMode +
                    " remover=" + resolved.removalMode +
                    " mainThread=true sameLooperTurnHide=true " +
                    "nativeGeometryWrites=0",
            )

            return AttachResult.Ready(resolved)
        }

        private fun verifyOnMain(
            requireLayoutHidden: Boolean,
        ): AttachResult {
            val currentRoot =
                NativeParticipantRuntimeAccess.findSlotView(
                    handles.group,
                    SLOT,
                )
                    ?: return AttachResult.Failure(
                        "shadow-root-missing",
                    )

            val iconVisible =
                NativeParticipantRuntimeAccess.iconVisible(currentRoot)
            if (!NativeParticipantShadowPolicy.isSemanticallyHidden(iconVisible)) {
                return AttachResult.Failure(
                    "shadow-semantic-visible iconVisible=" +
                        (iconVisible?.toString() ?: "unknown"),
                )
            }

            val layoutHidden =
                NativeParticipantShadowPolicy.isLayoutHidden(
                    rootVisible = currentRoot.visibility == View.VISIBLE,
                    measuredWidth = currentRoot.measuredWidth,
                )
            if (requireLayoutHidden && !layoutHidden) {
                return AttachResult.Failure(
                    "shadow-layout-visible visibility=" +
                        visibilityName(currentRoot.visibility) +
                        " measuredWidth=" + currentRoot.measuredWidth,
                )
            }

            val previousSnapshot =
                snapshot
                    ?: return AttachResult.Failure(
                        "shadow-snapshot-missing",
                    )
            val currentSnapshot =
                previousSnapshot.copy(
                    rootIndex = handles.group.indexOfChild(currentRoot),
                    rootVisibility = visibilityName(currentRoot.visibility),
                    iconVisible = false,
                    measuredWidth = currentRoot.measuredWidth,
                    measuredHeight = currentRoot.measuredHeight,
                    layoutHidden = layoutHidden,
                    childrenAfter = handles.group.childCount,
                )
            root = currentRoot
            snapshot = currentSnapshot
            return AttachResult.Ready(currentSnapshot)
        }

        private fun cleanupAfterValidationFailure(reason: String) {
            val cleanupResult =
                runCatching {
                    cleanupOnMain()
                    NativeParticipantRuntimeAccess.findSlotView(
                        handles.group,
                        SLOT,
                    ) == null
                }.getOrDefault(false)

            if (cleanupResult) {
                invalidate(this)
                root = null
            }

            onEvent(
                "nativeParticipantShadow postLayout " +
                    "slot=" + SLOT +
                    " state=error reason=" + reason +
                    " cleanup=" + if (cleanupResult) "verified" else "failed" +
                    " nativeGeometryWrites=0",
            )
        }

        private fun cleanupOnMain() {
            val activeRemoval = removal ?: return
            NativeParticipantRuntimeAccess.invokeRemoval(
                handles = handles,
                removal = activeRemoval,
                slot = SLOT,
            )
        }

        private fun snapshotOf(
            root: View,
            childrenBefore: Int,
            bootstrap: NativeParticipantRuntimeAccess.BootstrapResource,
            iconVisible: Boolean?,
            preExistingSlot: Boolean,
        ): Snapshot =
            Snapshot(
                slot = SLOT,
                rootClass = root.javaClass.name,
                rootIndex = handles.group.indexOfChild(root),
                rootVisibility = visibilityName(root.visibility),
                iconVisible = iconVisible == true,
                measuredWidth = root.measuredWidth,
                measuredHeight = root.measuredHeight,
                layoutHidden =
                    NativeParticipantShadowPolicy.isLayoutHidden(
                        rootVisible = root.visibility == View.VISIBLE,
                        measuredWidth = root.measuredWidth,
                    ),
                childrenBefore = childrenBefore,
                childrenAfter = handles.group.childCount,
                preExistingSlot = preExistingSlot,
                preAttachCleanupVerified = true,
                bootstrapResourceId = bootstrap.resourceId,
                bootstrapSourceSlot = bootstrap.sourceSlot,
                bootstrapSourceIndex = bootstrap.sourceIndex,
                creationMode = setter?.mode?.name ?: "NONE",
                removalMode = removal?.mode?.name ?: "NONE",
            )
    }

    private fun <T> runOnMainBlocking(block: () -> T): T {
        if (Looper.myLooper() === Looper.getMainLooper()) {
            return block()
        }

        val task = FutureTask<T> { block() }
        val posted = Handler(Looper.getMainLooper()).post(task)
        check(posted) { "main-thread-post-rejected" }
        return task.get(
            MAIN_THREAD_TIMEOUT_MS,
            TimeUnit.MILLISECONDS,
        )
    }

    private fun visibilityName(visibility: Int): String =
        when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> visibility.toString()
        }

    internal data class Snapshot(
        val slot: String,
        val rootClass: String,
        val rootIndex: Int,
        val rootVisibility: String,
        val iconVisible: Boolean,
        val measuredWidth: Int,
        val measuredHeight: Int,
        val layoutHidden: Boolean,
        val childrenBefore: Int,
        val childrenAfter: Int,
        val preExistingSlot: Boolean,
        val preAttachCleanupVerified: Boolean,
        val bootstrapResourceId: Int,
        val bootstrapSourceSlot: String?,
        val bootstrapSourceIndex: Int,
        val creationMode: String,
        val removalMode: String,
    )

    internal sealed interface AttachResult {
        data class Ready(
            val snapshot: Snapshot,
        ) : AttachResult

        data class Failure(
            val reason: String,
        ) : AttachResult
    }

    internal sealed interface DetachResult {
        data object Removed : DetachResult

        data object AlreadyDetached : DetachResult

        data class Failure(
            val reason: String,
        ) : DetachResult
    }
}
