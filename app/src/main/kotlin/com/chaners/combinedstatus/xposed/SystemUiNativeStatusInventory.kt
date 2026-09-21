package com.chaners.combinedstatus.xposed

import android.view.View
import android.view.ViewGroup

internal object SystemUiNativeStatusInventory {
    const val MOBILE_NETWORK_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView"
    const val WIFI_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView"
    const val BATTERY_VIEW_CLASS_NAME =
        "com.android.systemui.statusbar.views.MiuiBatteryMeterView"

    fun schedule(
        host: Any,
        onSnapshot: (Snapshot) -> Unit,
    ) {
        val root = host as? View ?: return

        if (root.isLaidOut) {
            onSnapshot(inspect(root))
            return
        }

        root.addOnLayoutChangeListener(
            object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    view: View,
                    left: Int,
                    top: Int,
                    right: Int,
                    bottom: Int,
                    oldLeft: Int,
                    oldTop: Int,
                    oldRight: Int,
                    oldBottom: Int,
                ) {
                    view.removeOnLayoutChangeListener(this)
                    onSnapshot(inspect(root))
                }
            },
        )
    }

    private fun inspect(root: View): Snapshot {
        val entries = buildList {
            collect(
                view = root,
                depth = 0,
                path = "root",
                destination = this,
            )
        }

        val directChildren = (root as? ViewGroup)
            ?.let { group ->
                buildList {
                    for (index in 0 until group.childCount) {
                        add(group.getChildAt(index).javaClass.simpleName)
                    }
                }
            }
            .orEmpty()

        return Snapshot(
            hostClassName = root.javaClass.name,
            hostWidth = root.width,
            hostHeight = root.height,
            hostMeasuredWidth = root.measuredWidth,
            hostMeasuredHeight = root.measuredHeight,
            directChildren = directChildren,
            entries = entries,
        )
    }

    private fun collect(
        view: View,
        depth: Int,
        path: String,
        destination: MutableList<Entry>,
    ) {
        roleFor(view.javaClass.name)?.let { role ->
            val parent = view.parent as? ViewGroup
            destination += Entry(
                role = role,
                className = view.javaClass.name,
                resourceId = resourceId(view),
                parentClassName = parent?.javaClass?.name ?: "none",
                parentIndex = parent?.indexOfChild(view) ?: -1,
                depth = depth,
                path = path,
                visibility = visibilityName(view.visibility),
                width = view.width,
                height = view.height,
                measuredWidth = view.measuredWidth,
                measuredHeight = view.measuredHeight,
                left = view.left,
                top = view.top,
                right = view.right,
                bottom = view.bottom,
                translationX = view.translationX,
                translationY = view.translationY,
            )
        }

        val group = view as? ViewGroup ?: return
        for (index in 0 until group.childCount) {
            collect(
                view = group.getChildAt(index),
                depth = depth + 1,
                path = "$path/$index",
                destination = destination,
            )
        }
    }

    private fun roleFor(className: String): String? =
        when (className) {
            MOBILE_NETWORK_VIEW_CLASS_NAME -> "mobileNetwork"
            WIFI_VIEW_CLASS_NAME -> "wifi"
            BATTERY_VIEW_CLASS_NAME -> "battery"
            else -> null
        }

    private fun resourceId(view: View): String {
        if (view.id == View.NO_ID) {
            return "none"
        }

        return runCatching {
            view.resources.getResourceName(view.id)
        }.getOrElse {
            view.id.toString()
        }
    }

    private fun visibilityName(visibility: Int): String =
        when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> visibility.toString()
        }

    internal data class Snapshot(
        val hostClassName: String,
        val hostWidth: Int,
        val hostHeight: Int,
        val hostMeasuredWidth: Int,
        val hostMeasuredHeight: Int,
        val directChildren: List<String>,
        val entries: List<Entry>,
    ) {
        val summary: String
            get() {
                val mobileCount = entries.count { it.role == "mobileNetwork" }
                val wifiCount = entries.count { it.role == "wifi" }
                val batteryCount = entries.count { it.role == "battery" }
                val childSummary = directChildren.joinToString(",")

                return buildString {
                    append("nativeStatus inventory ")
                    append("host=")
                    append(hostClassName.substringAfterLast('.'))
                    append(" size=")
                    append(hostWidth)
                    append('x')
                    append(hostHeight)
                    append(" measured=")
                    append(hostMeasuredWidth)
                    append('x')
                    append(hostMeasuredHeight)
                    append(" mobileNetwork=")
                    append(mobileCount)
                    append(" wifi=")
                    append(wifiCount)
                    append(" battery=")
                    append(batteryCount)
                    append(" directChildren=[")
                    append(childSummary)
                    append(']')
                }
            }
    }

    internal data class Entry(
        val role: String,
        val className: String,
        val resourceId: String,
        val parentClassName: String,
        val parentIndex: Int,
        val depth: Int,
        val path: String,
        val visibility: String,
        val width: Int,
        val height: Int,
        val measuredWidth: Int,
        val measuredHeight: Int,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val translationX: Float,
        val translationY: Float,
    ) {
        val logLine: String
            get() =
                "nativeStatus role=$role " +
                    "class=${className.substringAfterLast('.')} " +
                    "id=$resourceId parent=${parentClassName.substringAfterLast('.')} " +
                    "index=$parentIndex depth=$depth path=$path " +
                    "visibility=$visibility " +
                    "bounds=$left,$top-$right,$bottom " +
                    "size=${width}x$height measured=${measuredWidth}x$measuredHeight " +
                    "translation=${translationX},${translationY}"
    }
}
