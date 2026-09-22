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
            return existing.verify()
        }

        if (existing != null) {
            when (val stopped = existing.stop()) {
                NativeParticipantShadowSession.DetachResult.Removed,
                NativeParticipantShadowSession.DetachResult.AlreadyDetached -> Unit

                is NativeParticipantShadowSession.DetachResult.Failure ->
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

        fun verify(): AttachResult =
            runCatching {
                runOnMainBlocking {
                    val root =
                        NativeParticipantRuntimeAccess.findSlotView(
                            handles.group,
                            SLOT,
                        )
                        ?: return@runOnMainBlocking AttachResult.Failure(
                            "shadow-root-missing",
                        )
                    if (root.visibility == View.VISIBLE) {
                        return@runOnMainBlocking AttachResult.Failure(
                            "shadow-root-visible",
                        )
                    }
                    val previousSnapshot =
                        snapshot
                            ?: return@runOnMainBlocking AttachResult.Failure(
                                "shadow-snapshot-missing",
                            )
                    val currentSnapshot =
                        previousSnapshot.copy(
                            rootIndex = handles.group.indexOfChild(root),
                            rootVisibility = visibilityName(root.visibility),
                            measuredWidth = root.measuredWidth,
                            measuredHeight = root.measuredHeight,
                        )
                    snapshot = currentSnapshot
                    AttachResult.Ready(currentSnapshot)
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
                    cleanupOnMain()
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

            cleanupOnMain()

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

            val root =
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

            if (root.visibility == View.VISIBLE) {
                cleanupOnMain()
                return AttachResult.Failure(
                    "shadow-hide-not-applied",
                )
            }

            val resolved =
                snapshotOf(
                    root = root,
                    childrenBefore = childrenBefore,
                    bootstrap = bootstrap,
                )
            snapshot = resolved

            onEvent(
                "nativeParticipantShadow attached " +
                    "slot=" + SLOT +
                    " root=" + resolved.rootClass +
                    " index=" + resolved.rootIndex +
                    " visibility=" + resolved.rootVisibility +
                    " children=" + resolved.childrenBefore +
                    "->" + resolved.childrenAfter +
                    " bootstrapRes=0x" +
                    resolved.bootstrapResourceId.toUInt().toString(16) +
                    " bootstrapSlot=" + (resolved.bootstrapSourceSlot ?: "unknown") +
                    " setter=" + resolved.creationMode +
                    " remover=" + resolved.removalMode +
                    " mainThread=true transientVisibleFrame=false " +
                    "nativeGeometryWrites=0",
            )

            return AttachResult.Ready(resolved)
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
        ): Snapshot =
            Snapshot(
                slot = SLOT,
                rootClass = root.javaClass.name,
                rootIndex = handles.group.indexOfChild(root),
                rootVisibility = visibilityName(root.visibility),
                measuredWidth = root.measuredWidth,
                measuredHeight = root.measuredHeight,
                childrenBefore = childrenBefore,
                childrenAfter = handles.group.childCount,
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
        val measuredWidth: Int,
        val measuredHeight: Int,
        val childrenBefore: Int,
        val childrenAfter: Int,
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
