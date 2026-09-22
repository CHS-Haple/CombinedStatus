package com.chaners.combinedstatus.xposed

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

internal object NativePresentationResolver {
    fun resolve(
        state: CombinedStatusStateStore.Snapshot,
        defaultDataSubscriptionId: Int =
            SystemUiDefaultDataSubscriptionSource.currentSubscriptionId(),
    ): Snapshot {
        val bindings =
            SystemUiNetworkStateSource.mobilePresentationBindings()
                .filter { binding -> binding.root.isAttachedToWindow }
        val activeBindingSubIds =
            bindings
                .map { binding -> binding.subscriptionId }
                .distinct()
                .filter { subscriptionId ->
                    state.mobile[subscriptionId]
                        ?.signal
                        ?.let { signal -> signal !is SignalStrength.Unknown }
                        ?: false
                }
        val visible =
            bindings.filter { binding ->
                binding.root.visibility == View.VISIBLE
            }

        val mode =
            classify(
                boundRoots = bindings.size,
                visibleRoots = visible.size,
                activeSubscriptions = activeBindingSubIds.size,
            )

        val effectiveDataSubscriptionId =
            defaultDataSubscriptionId
                .takeIf { subscriptionId -> subscriptionId >= 0 }
                ?: activeBindingSubIds.firstOrNull()

        val target =
            when (mode) {
                Mode.DUAL_AGGREGATED,
                Mode.SINGLE,
                -> visible.firstOrNull()

                Mode.DUAL_SEPARATE ->
                    visible.firstOrNull { binding ->
                        binding.subscriptionId == effectiveDataSubscriptionId
                    } ?: visible.firstOrNull()

                Mode.UNKNOWN -> visible.firstOrNull()
            }

        val networkTypeSubscriptionId =
            selectNetworkTypeSubscriptionId(
                effectiveDataSubscriptionId = effectiveDataSubscriptionId,
                presentationRootSubscriptionId = target?.subscriptionId,
                boundSubscriptionIds = bindings.map { binding -> binding.subscriptionId },
            )
        val networkTypeTarget =
            networkTypeSubscriptionId?.let { subscriptionId ->
                bindings.firstOrNull { binding ->
                    binding.subscriptionId == subscriptionId
                }
            }
        val networkType = networkTypeTarget?.let(::resolveNetworkType)

        return Snapshot(
            mode = mode,
            boundRoots = bindings.size,
            visibleRoots = visible.size,
            activeSubscriptionIds = activeBindingSubIds.sorted(),
            presentationRootSubscriptionId = target?.subscriptionId,
            effectiveDataSubscriptionId = effectiveDataSubscriptionId,
            networkTypeSubscriptionId = networkTypeSubscriptionId,
            networkType = networkType,
        )
    }

    internal fun selectNetworkTypeSubscriptionId(
        effectiveDataSubscriptionId: Int?,
        presentationRootSubscriptionId: Int?,
        boundSubscriptionIds: List<Int>,
    ): Int? =
        effectiveDataSubscriptionId
            ?.takeIf { subscriptionId -> subscriptionId in boundSubscriptionIds }
            ?: presentationRootSubscriptionId
                ?.takeIf { subscriptionId -> subscriptionId in boundSubscriptionIds }
            ?: boundSubscriptionIds.firstOrNull()

    internal fun classify(
        boundRoots: Int,
        visibleRoots: Int,
        activeSubscriptions: Int,
    ): Mode =
        when {
            boundRoots <= 0 -> Mode.UNKNOWN
            activeSubscriptions <= 1 && visibleRoots <= 1 -> Mode.SINGLE
            activeSubscriptions >= 2 && visibleRoots >= 2 -> Mode.DUAL_SEPARATE
            activeSubscriptions >= 2 && visibleRoots == 1 -> Mode.DUAL_AGGREGATED
            else -> Mode.UNKNOWN
        }

    private fun resolveNetworkType(
        binding: SystemUiNetworkStateSource.MobilePresentationBinding,
    ): NetworkType? {
        val root = binding.root

        findViewByResourceEntry(root, MOBILE_TYPE_RESOURCE_ENTRY)
            ?.let { view ->
                val image = view as? ImageView
                val drawable = image?.drawable
                val label = drawable?.readStringField(MOBILE_TYPE_FIELD)?.trim().orEmpty()
                if (label.isNotEmpty()) {
                    return NetworkType(
                        label = label,
                        enhanced = drawable?.readBooleanField(MOBILE_TYPE_ENHANCED_FIELD) == true,
                        source = NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
                    )
                }
            }

        findViewByResourceEntry(root, MOBILE_TYPE_SINGLE_RESOURCE_ENTRY)
            ?.let { view ->
                val text = (view as? TextView)?.text?.toString()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    return NetworkType(
                        label = text,
                        enhanced = false,
                        source = NetworkTypeSource.MOBILE_TYPE_SINGLE,
                    )
                }
            }

        return null
    }

    private fun findViewByResourceEntry(
        root: View,
        entryName: String,
    ): View? {
        if (resourceEntryName(root) == entryName) {
            return root
        }
        val group = root as? ViewGroup ?: return null
        for (index in 0 until group.childCount) {
            findViewByResourceEntry(group.getChildAt(index), entryName)?.let {
                return it
            }
        }
        return null
    }

    private fun resourceEntryName(view: View): String? {
        if (view.id == View.NO_ID) {
            return null
        }
        return runCatching {
            view.resources.getResourceEntryName(view.id)
        }.getOrNull()
    }

    private fun Drawable.readStringField(name: String): String? =
        runCatching {
            javaClass.getDeclaredField(name)
                .apply { isAccessible = true }
                .get(this) as? String
        }.getOrNull()

    private fun Drawable.readBooleanField(name: String): Boolean? =
        runCatching {
            javaClass.getDeclaredField(name)
                .apply { isAccessible = true }
                .getBoolean(this)
        }.getOrNull()

    internal enum class Mode {
        SINGLE,
        DUAL_SEPARATE,
        DUAL_AGGREGATED,
        UNKNOWN,
    }

    internal enum class NetworkTypeSource {
        MOBILE_TYPE_DRAWABLE,
        MOBILE_TYPE_SINGLE,
    }

    internal data class NetworkType(
        val label: String,
        val enhanced: Boolean,
        val source: NetworkTypeSource,
    )

    internal data class Snapshot(
        val mode: Mode,
        val boundRoots: Int,
        val visibleRoots: Int,
        val activeSubscriptionIds: List<Int>,
        val presentationRootSubscriptionId: Int?,
        val effectiveDataSubscriptionId: Int?,
        val networkTypeSubscriptionId: Int?,
        val networkType: NetworkType?,
    ) {
        val logLine: String
            get() =
                "mobilePresentation mode=" + mode.name +
                    " boundRoots=" + boundRoots +
                    " visibleRoots=" + visibleRoots +
                    " activeSubIds=" + activeSubscriptionIds.joinToString(",", prefix = "[", postfix = "]") +
                    " presentationRootSubId=" + (presentationRootSubscriptionId ?: -1) +
                    " effectiveDataSubId=" + (effectiveDataSubscriptionId ?: -1) +
                    " networkTypeSubId=" + (networkTypeSubscriptionId ?: -1) +
                    " networkType=" + (networkType?.label ?: "unknown") +
                    " enhanced=" + (networkType?.enhanced ?: false) +
                    " typeSource=" + (networkType?.source?.name ?: "none") +
                    " geometryWrites=0"
    }

    private const val MOBILE_TYPE_RESOURCE_ENTRY = "mobile_type"
    private const val MOBILE_TYPE_SINGLE_RESOURCE_ENTRY = "mobile_type_single"
    private const val MOBILE_TYPE_FIELD = "mMobileType"
    private const val MOBILE_TYPE_ENHANCED_FIELD = "mShowMobileTypeDoublePlus"
}
