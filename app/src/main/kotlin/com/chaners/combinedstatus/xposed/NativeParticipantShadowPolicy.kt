package com.chaners.combinedstatus.xposed

internal object NativeParticipantShadowPolicy {
    fun isSemanticallyHidden(iconVisible: Boolean?): Boolean =
        iconVisible == false

    fun isLayoutHidden(
        rootVisible: Boolean,
        measuredWidth: Int,
    ): Boolean =
        !rootVisible || measuredWidth == 0
}
