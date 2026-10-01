package com.chaners.guiyuan.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BATTERY_COLOR_SCHEME_CUSTOM_MAX
import com.chaners.guiyuan.settings.BATTERY_COLOR_SCHEME_HYPEROS_KEY
import com.chaners.guiyuan.settings.BatteryBuiltInColorScheme
import com.chaners.guiyuan.settings.BatteryColorSchemeEntry
import com.chaners.guiyuan.settings.BatteryColorSchemeLibrary
import com.chaners.guiyuan.settings.BatteryColorSchemeLibraryRepository
import com.chaners.guiyuan.settings.BatteryColorSchemeSource
import com.chaners.guiyuan.settings.BatteryCustomColorScheme
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorSlot
import com.chaners.guiyuan.settings.batteryBuiltInColor
import com.chaners.guiyuan.settings.batterySchemeEntryColor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HsvHueSlider
import top.yukonga.miuix.kmp.basic.HsvSaturationSlider
import top.yukonga.miuix.kmp.basic.HsvValueSlider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.drawCheckerboard
import top.yukonga.miuix.kmp.color.api.toHsv
import top.yukonga.miuix.kmp.color.space.Hsv
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Forward
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonLocation
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride
import top.yukonga.miuix.kmp.utils.springAnimateToPage

private val COMMON_BATTERY_COLORS =
    listOf(
        0xFFFF3B30.toInt(),
        0xFFFF9500.toInt(),
        0xFFFFCC00.toInt(),
        0xFF34C759.toInt(),
        0xFF32ADE6.toInt(),
        0xFF007AFF.toInt(),
        0xFF5856D6.toInt(),
        0xFFAF52DE.toInt(),
        0xFFFF2D55.toInt(),
        0xFF8E8E93.toInt(),
    )

private val BATTERY_COLOR_PREVIEW_SLOTS = CombinedStatusBatteryColorSlot.entries

private sealed interface BatterySchemePage {
    val key: String

    data class BuiltIn(
        val scheme: BatteryBuiltInColorScheme,
    ) : BatterySchemePage {
        override val key: String = scheme.key
    }

    data class Custom(
        val scheme: BatteryCustomColorScheme,
    ) : BatterySchemePage {
        override val key: String = scheme.key
    }

    data object Add : BatterySchemePage {
        override val key: String = "add"
    }
}

@Composable
internal fun BatteryColorPreference(
    library: BatteryColorSchemeLibrary,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ArrowPreference(
        title = stringResource(R.string.battery_colors),
        summary =
            stringResource(
                R.string.battery_color_scheme_summary,
                batterySchemeDisplayName(library, library.activeSchemeKey),
            ),
        enabled = enabled,
        onClick = onClick,
        endActions = {
            BatterySchemePreviewStrip(
                page = schemePageForKey(library, library.activeSchemeKey),
            )
        },
    )
}

@Composable
internal fun BatteryColorBottomSheet(
    show: Boolean,
    library: BatteryColorSchemeLibrary,
    repository: BatteryColorSchemeLibraryRepository,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val navPager = rememberPagerState(initialPage = 0, pageCount = { 2 })
    var selectedCustomId by remember { mutableStateOf<Int?>(null) }
    var selectedSlot by remember { mutableStateOf<CombinedStatusBatteryColorSlot?>(null) }
    var sourceExpanded by remember { mutableStateOf(false) }
    var createFromKey by remember { mutableStateOf(BATTERY_COLOR_SCHEME_HYPEROS_KEY) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var manageCustomId by remember { mutableStateOf<Int?>(null) }
    var renameCustomId by remember { mutableStateOf<Int?>(null) }
    var deleteCustomId by remember { mutableStateOf<Int?>(null) }
    val inDetail = navPager.currentPage == 1

    OverlayBottomSheet(
        show = show,
        title = stringResource(R.string.battery_colors),
        startAction =
            if (inDetail) {
                {
                    IconButton(
                        onClick = {
                            sourceExpanded = false
                            scope.launch { navPager.springAnimateToPage(0) }
                        },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                }
            } else {
                null
            },
        onDismissRequest = {
            if (inDetail) {
                sourceExpanded = false
                scope.launch { navPager.springAnimateToPage(0) }
            } else {
                onDismiss()
            }
        },
    ) {
        HorizontalPager(
            state = navPager,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 520.dp, max = 650.dp),
            userScrollEnabled = false,
        ) { page ->
            if (page == 0) {
                BatterySchemeOverview(
                    library = library,
                    managedCustomId = manageCustomId,
                    onSettledScheme = repository::activateScheme,
                    onOpenCustomSlot = { id, slot ->
                        selectedCustomId = id
                        selectedSlot = slot
                        sourceExpanded = false
                        scope.launch { navPager.springAnimateToPage(1) }
                    },
                    onAdd = { fromKey ->
                        createFromKey = fromKey
                        showCreateDialog = true
                    },
                    onManageCustom = { manageCustomId = it },
                )
            } else {
                val custom = selectedCustomId?.let(library::customById)
                val slot = selectedSlot
                if (custom != null && slot != null) {
                    BatteryCustomModeEditor(
                        custom = custom,
                        slot = slot,
                        sourceExpanded = sourceExpanded,
                        onSourceExpandedChange = { sourceExpanded = it },
                        onSourceChange = { source ->
                            repository.setCustomSource(custom.id, slot, source)
                            sourceExpanded = false
                        },
                        onColorChange = { color ->
                            repository.setCustomColor(custom.id, slot, color)
                        },
                        onRestore = {
                            repository.restoreCustomSlot(custom.id, slot)
                        },
                    )
                }
            }
        }
    }

    BatteryCreateSchemeDialog(
        show = showCreateDialog,
        nextId = repository.nextAvailableCustomId(),
        onDismiss = { showCreateDialog = false },
        onCreate = { name ->
            showCreateDialog = false
            repository.createCustom(name, createFromKey)
        },
    )

    val managed = manageCustomId?.let(library::customById)
    val nextCopyId = repository.nextAvailableCustomId()
    val nextCopyName =
        nextCopyId?.let {
            stringResource(R.string.battery_custom_scheme_default_name, it)
        }
    OverlayDialog(
        title = managed?.let { customSchemeName(it) } ?: "",
        show = managed != null,
        onDismissRequest = { manageCustomId = null },
    ) {
        if (managed != null) {
            Column {
                BasicComponent(
                    title = stringResource(R.string.battery_custom_scheme_rename),
                    onClick = {
                        manageCustomId = null
                        renameCustomId = managed.id
                    },
                )
                BasicComponent(
                    title = stringResource(R.string.battery_custom_scheme_copy),
                    enabled = nextCopyId != null,
                    onClick = {
                        nextCopyName?.let { repository.createCustom(it, managed.key) }
                        manageCustomId = null
                    },
                )
                BasicComponent(
                    title = stringResource(R.string.battery_custom_scheme_delete),
                    titleColor =
                        BasicComponentColors(
                            color = MiuixTheme.colorScheme.error,
                            disabledColor = MiuixTheme.colorScheme.error.copy(alpha = 0.4f),
                        ),
                    onClick = {
                        manageCustomId = null
                        deleteCustomId = managed.id
                    },
                )
            }
        }
    }

    BatteryRenameSchemeDialog(
        scheme = renameCustomId?.let(library::customById),
        onDismiss = { renameCustomId = null },
        onRename = { id, name ->
            renameCustomId = null
            repository.renameCustom(id, name)
        },
    )

    val deleting = deleteCustomId?.let(library::customById)
    OverlayDialog(
        title = stringResource(R.string.battery_custom_scheme_delete),
        summary =
            deleting?.let {
                stringResource(
                    R.string.battery_custom_scheme_delete_summary,
                    customSchemeName(it),
                )
            },
        show = deleting != null,
        onDismissRequest = { deleteCustomId = null },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                text = stringResource(R.string.cancel),
                modifier = Modifier.weight(1f),
                onClick = { deleteCustomId = null },
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                text = stringResource(R.string.battery_custom_scheme_delete),
                modifier = Modifier.weight(1f),
                colors =
                    ButtonDefaults.textButtonColors(
                        textColor = MiuixTheme.colorScheme.error,
                    ),
                onClick = {
                    deleting?.let { repository.deleteCustom(it.id) }
                    deleteCustomId = null
                },
            )
        }
    }
}

@Composable
private fun BatterySchemeOverview(
    library: BatteryColorSchemeLibrary,
    managedCustomId: Int?,
    onSettledScheme: (String) -> Unit,
    onOpenCustomSlot: (Int, CombinedStatusBatteryColorSlot) -> Unit,
    onAdd: (String) -> Unit,
    onManageCustom: (Int) -> Unit,
) {
    val pages =
        buildList {
            add(BatterySchemePage.BuiltIn(BatteryBuiltInColorScheme.HYPEROS))
            add(BatterySchemePage.BuiltIn(BatteryBuiltInColorScheme.IOS))
            add(BatterySchemePage.BuiltIn(BatteryBuiltInColorScheme.LOW_SATURATION))
            library.customSchemes.forEach { add(BatterySchemePage.Custom(it)) }
            add(BatterySchemePage.Add)
        }
    val initial =
        pages.indexOfFirst { it.key == library.activeSchemeKey }
            .takeIf { it >= 0 }
            ?: 0
    val pagerState =
        rememberPagerState(
            initialPage = initial,
            pageCount = { pages.size },
        )
    val flingBehavior =
        PagerDefaults.flingBehavior(
            state = pagerState,
            snapAnimationSpec = PagerNavigationSpringSpec,
        )
    var lastSchemeKey by remember { mutableStateOf(library.activeSchemeKey) }

    LaunchedEffect(pagerState, pages.map { it.key }) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { index ->
                val page = pages.getOrNull(index)
                if (page != null && page !is BatterySchemePage.Add) {
                    lastSchemeKey = page.key
                    onSettledScheme(page.key)
                }
            }
    }

    LaunchedEffect(library.activeSchemeKey, pages.size) {
        val target = pages.indexOfFirst { it.key == library.activeSchemeKey }
        if (target >= 0 && target != pagerState.currentPage) {
            pagerState.springAnimateToPage(target)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(455.dp)
                    .pagerGestureOverride(
                        pagerState = pagerState,
                        flingBehavior = flingBehavior,
                    ),
            userScrollEnabled = false,
            flingBehavior = flingBehavior,
            pageNestedScrollConnection = PagerGestureNestedScrollConnection,
        ) { index ->
            when (val page = pages[index]) {
                is BatterySchemePage.BuiltIn ->
                    BatterySchemePageContent(
                        name = batteryBuiltInName(page.scheme),
                        builtIn = page.scheme,
                        custom = null,
                        onSlotClick = null,
                        onManage = null,
                        manageHeldDown = false,
                    )
                is BatterySchemePage.Custom ->
                    BatterySchemePageContent(
                        name = customSchemeName(page.scheme),
                        builtIn = null,
                        custom = page.scheme,
                        onSlotClick = { slot ->
                            onOpenCustomSlot(page.scheme.id, slot)
                        },
                        onManage = { onManageCustom(page.scheme.id) },
                        manageHeldDown = managedCustomId == page.scheme.id,
                    )
                BatterySchemePage.Add ->
                    BatteryAddSchemePage(
                        enabled = library.customSchemes.size < BATTERY_COLOR_SCHEME_CUSTOM_MAX,
                        onClick = { onAdd(lastSchemeKey) },
                    )
            }
        }

        Spacer(Modifier.height(8.dp))
        BatteryPagerIndicator(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BatterySchemePageContent(
    name: String,
    builtIn: BatteryBuiltInColorScheme?,
    custom: BatteryCustomColorScheme?,
    onSlotClick: ((CombinedStatusBatteryColorSlot) -> Unit)?,
    onManage: (() -> Unit)?,
    manageHeldDown: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Card(
            insideMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(name)
                    Spacer(Modifier.height(10.dp))
                    BatterySchemePreviewStrip(
                        page =
                            if (builtIn != null) {
                                BatterySchemePage.BuiltIn(builtIn)
                            } else {
                                custom?.let(BatterySchemePage::Custom)
                            },
                    )
                }
                if (onManage != null) {
                    IconButton(
                        modifier = Modifier.align(Alignment.TopEnd),
                        onClick = onManage,
                        holdDownState = manageHeldDown,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription =
                                stringResource(R.string.battery_custom_scheme_manage),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Card(
            insideMargin = PaddingValues(vertical = 2.dp),
        ) {
            CombinedStatusBatteryColorSlot.entries.forEach { slot ->
                val color =
                    when {
                        builtIn != null -> batteryBuiltInColor(builtIn, slot)
                        custom != null ->
                            batterySchemeEntryColor(custom.entries.entryFor(slot), slot)
                        else -> null
                    }
                val followsSystem =
                    when {
                        builtIn != null -> color == null
                        custom != null ->
                            custom.entries.entryFor(slot).source ==
                                BatteryColorSchemeSource.FOLLOW_SYSTEM ||
                                color == null
                        else -> true
                    }
                BatteryModeRow(
                    slot = slot,
                    color = color,
                    followsSystem = followsSystem,
                    editable = onSlotClick != null,
                    onClick = onSlotClick?.let { callback -> { callback(slot) } },
                )
            }
        }
    }
}

@Composable
private fun BatteryModeRow(
    slot: CombinedStatusBatteryColorSlot,
    color: Int?,
    followsSystem: Boolean,
    editable: Boolean,
    onClick: (() -> Unit)?,
) {
    BasicComponent(
        title = stringResource(batteryColorSlotLabel(slot)),
        onClick = onClick,
        endActions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.width(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (followsSystem || color == null) {
                        BatteryColorMosaic()
                    } else {
                        BatteryColorDot(color)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text =
                        if (followsSystem || color == null) {
                            stringResource(R.string.battery_color_follow_inversion)
                        } else {
                            batteryColorHex(color)
                        },
                    modifier = Modifier.width(92.dp),
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    fontFamily =
                        if (followsSystem || color == null) {
                            FontFamily.Default
                        } else {
                            FontFamily.Monospace
                        },
                )
                Box(
                    modifier = Modifier.width(22.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    if (editable) {
                        Icon(
                            imageVector = MiuixIcons.Forward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun BatteryAddSchemePage(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            insideMargin = PaddingValues(vertical = 46.dp),
            showIndication = enabled,
            onClick = if (enabled) onClick else null,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = MiuixIcons.Add,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint =
                        if (enabled) {
                            MiuixTheme.colorScheme.onSurface
                        } else {
                            MiuixTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        },
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text =
                        if (enabled) {
                            stringResource(R.string.battery_custom_scheme_new)
                        } else {
                            stringResource(
                                R.string.battery_custom_scheme_limit,
                                BATTERY_COLOR_SCHEME_CUSTOM_MAX,
                            )
                        },
                    color =
                        if (enabled) {
                            MiuixTheme.colorScheme.onSurface
                        } else {
                            MiuixTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        },
                )
            }
        }
    }
}

@Composable
private fun BatteryPagerIndicator(
    pageCount: Int,
    currentPage: Int,
) {
    Card(
        insideMargin = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pageCount) { index ->
                val width by
                    animateDpAsState(
                        targetValue = if (index == currentPage) 16.dp else 6.dp,
                        label = "battery-scheme-indicator",
                    )
                Surface(
                    modifier =
                        Modifier
                            .width(width)
                            .height(6.dp),
                    shape = CircleShape,
                    color =
                        if (index == currentPage) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurface.copy(alpha = 0.22f)
                        },
                ) {}
            }
        }
    }
}

@Composable
private fun BatteryCustomModeEditor(
    custom: BatteryCustomColorScheme,
    slot: CombinedStatusBatteryColorSlot,
    sourceExpanded: Boolean,
    onSourceExpandedChange: (Boolean) -> Unit,
    onSourceChange: (BatteryColorSchemeSource) -> Unit,
    onColorChange: (Int) -> Unit,
    onRestore: () -> Unit,
) {
    val entry = custom.entries.entryFor(slot)
    val resolved = batterySchemeEntryColor(entry, slot)
    val fallback =
        entry.customColor
            ?: batteryBuiltInColor(custom.baseTemplate, slot)
            ?: MiuixTheme.colorScheme.onSurface.toArgb()
    var editingColor by remember(custom.id, slot, entry.source, resolved) {
        mutableIntStateOf((resolved ?: fallback).or(0xFF000000.toInt()))
    }
    var hexText by remember(custom.id, slot, editingColor) {
        mutableStateOf(batteryColorHex(editingColor).removePrefix("#"))
    }
    val rgb = batteryColorRgb(editingColor)
    var redText by remember(custom.id, slot, editingColor) {
        mutableStateOf(rgb.first.toString())
    }
    var greenText by remember(custom.id, slot, editingColor) {
        mutableStateOf(rgb.second.toString())
    }
    var blueText by remember(custom.id, slot, editingColor) {
        mutableStateOf(rgb.third.toString())
    }

    fun applyColor(color: Int) {
        val opaque = color or 0xFF000000.toInt()
        editingColor = opaque
        hexText = batteryColorHex(opaque).removePrefix("#")
        val value = batteryColorRgb(opaque)
        redText = value.first.toString()
        greenText = value.second.toString()
        blueText = value.third.toString()
        onColorChange(opaque)
    }

    val hsv = Color(editingColor).toHsv()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, bottom = 24.dp),
    ) {
        SmallTitle(
            stringResource(
                R.string.battery_custom_mode_title,
                customSchemeName(custom),
                stringResource(batteryColorSlotLabel(slot)),
            ),
        )

        SmallTitle(stringResource(R.string.battery_color_source))
        Card {
            BasicComponent(
                title = batterySourceLabel(entry.source),
                summary = batterySourceValue(entry, slot),
                onClick = { onSourceExpandedChange(!sourceExpanded) },
                endActions = {
                    if (resolved == null) {
                        BatteryColorMosaic()
                    } else {
                        BatteryColorDot(resolved)
                    }
                },
            )
            if (sourceExpanded) {
                BatteryColorSchemeSource.entries.forEach { source ->
                    val candidate = entry.copy(source = source)
                    RadioButtonPreference(
                        title = batterySourceLabel(source),
                        summary = batterySourceValue(candidate, slot),
                        selected = entry.source == source,
                        onClick = {
                            if (
                                source == BatteryColorSchemeSource.CUSTOM &&
                                entry.customColor == null
                            ) {
                                applyColor(editingColor)
                                onSourceExpandedChange(false)
                            } else {
                                onSourceChange(source)
                            }
                        },
                        radioButtonLocation = RadioButtonLocation.End,
                    )
                }
            }
        }

        SmallTitle(stringResource(R.string.battery_color_settings))
        Card {
            BasicComponent(
                title =
                    if (resolved == null) {
                        stringResource(R.string.battery_color_follow_inversion)
                    } else {
                        batteryColorHex(resolved)
                    },
                summary = stringResource(R.string.battery_color_edit_auto_custom),
                endActions = {
                    if (resolved == null) {
                        BatteryColorMosaic(size = 28.dp)
                    } else {
                        BatteryColorDot(resolved, size = 28.dp)
                    }
                },
            )
        }

        SmallTitle(stringResource(R.string.battery_color_common))
        Card(
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            COMMON_BATTERY_COLORS.chunked(5).forEachIndexed { index, colors ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (index == 0) 12.dp else 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    colors.forEach { color ->
                        BatteryCommonColorButton(
                            color = color,
                            selected = editingColor == color,
                            onClick = { applyColor(color) },
                        )
                    }
                }
            }
        }

        SmallTitle(stringResource(R.string.battery_color_full_adjustment))
        Card {
            BasicComponent(
                title = stringResource(R.string.battery_color_hue),
                bottomAction = {
                    HsvHueSlider(
                        currentHue = hsv.h,
                        onHueChanged = { fraction ->
                            applyColor(
                                Hsv(fraction * 360f, hsv.s, hsv.v).toColor().toArgb(),
                            )
                        },
                    )
                },
            )
            BasicComponent(
                title = stringResource(R.string.battery_color_saturation),
                bottomAction = {
                    HsvSaturationSlider(
                        currentHue = hsv.h,
                        currentSaturation = hsv.s / 100f,
                        onSaturationChanged = { saturation ->
                            applyColor(
                                Hsv(hsv.h, saturation * 100f, hsv.v).toColor().toArgb(),
                            )
                        },
                    )
                },
            )
            BasicComponent(
                title = stringResource(R.string.battery_color_brightness),
                bottomAction = {
                    HsvValueSlider(
                        currentHue = hsv.h,
                        currentSaturation = hsv.s / 100f,
                        currentValue = hsv.v / 100f,
                        onValueChanged = { value ->
                            applyColor(
                                Hsv(hsv.h, hsv.s, value * 100f).toColor().toArgb(),
                            )
                        },
                    )
                },
            )
        }

        SmallTitle(stringResource(R.string.battery_color_precise_input))
        Card(
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            TextField(
                value = hexText,
                onValueChange = { raw ->
                    val normalized =
                        raw.removePrefix("#")
                            .uppercase()
                            .filter { it.isDigit() || it in 'A'..'F' }
                    if (normalized.length <= 6) {
                        hexText = normalized
                        batteryColorFromHex(normalized)?.let(::applyColor)
                    }
                },
                label = stringResource(R.string.battery_color_hex),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = redText,
                    onValueChange = { raw ->
                        if (raw.length <= 3 && raw.all(Char::isDigit)) {
                            redText = raw
                            batteryColorFromRgb(redText, greenText, blueText)?.let(::applyColor)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "R",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                TextField(
                    value = greenText,
                    onValueChange = { raw ->
                        if (raw.length <= 3 && raw.all(Char::isDigit)) {
                            greenText = raw
                            batteryColorFromRgb(redText, greenText, blueText)?.let(::applyColor)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "G",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                TextField(
                    value = blueText,
                    onValueChange = { raw ->
                        if (raw.length <= 3 && raw.all(Char::isDigit)) {
                            blueText = raw
                            batteryColorFromRgb(redText, greenText, blueText)?.let(::applyColor)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "B",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        }

        SmallTitle(stringResource(R.string.section_management))
        Card {
            BasicComponent(
                title = stringResource(R.string.battery_color_restore_mode),
                summary =
                    stringResource(
                        R.string.battery_color_restore_from_scheme,
                        batteryBuiltInName(custom.baseTemplate),
                    ),
                onClick = onRestore,
            )
        }
    }
}

@Composable
private fun BatteryCreateSchemeDialog(
    show: Boolean,
    nextId: Int?,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    val defaultName =
        nextId?.let {
            stringResource(R.string.battery_custom_scheme_default_name, it)
        }.orEmpty()
    var name by remember(show, nextId) { mutableStateOf(defaultName) }

    OverlayDialog(
        title = stringResource(R.string.battery_custom_scheme_new),
        show = show && nextId != null,
        onDismissRequest = onDismiss,
    ) {
        Column {
            TextField(
                value = name,
                onValueChange = { name = it.take(28) },
                label = stringResource(R.string.battery_custom_scheme_name),
                singleLine = true,
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = stringResource(R.string.battery_custom_scheme_create),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        if (name.isNotBlank()) onCreate(name.trim())
                    },
                )
            }
        }
    }
}

@Composable
private fun BatteryRenameSchemeDialog(
    scheme: BatteryCustomColorScheme?,
    onDismiss: () -> Unit,
    onRename: (Int, String) -> Unit,
) {
    var name by remember(scheme?.id) { mutableStateOf(scheme?.name.orEmpty()) }
    OverlayDialog(
        title = stringResource(R.string.battery_custom_scheme_rename),
        show = scheme != null,
        onDismissRequest = onDismiss,
    ) {
        if (scheme != null) {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it.take(28) },
                    label = stringResource(R.string.battery_custom_scheme_name),
                    singleLine = true,
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                    )
                    Spacer(Modifier.width(20.dp))
                    TextButton(
                        text = stringResource(R.string.confirm),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            if (name.isNotBlank()) onRename(scheme.id, name.trim())
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BatterySchemePreviewStrip(page: BatterySchemePage?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BATTERY_COLOR_PREVIEW_SLOTS.forEach { slot ->
            val color =
                when (page) {
                    is BatterySchemePage.BuiltIn ->
                        batteryBuiltInColor(page.scheme, slot)
                    is BatterySchemePage.Custom ->
                        batterySchemeEntryColor(page.scheme.entries.entryFor(slot), slot)
                    else -> null
                }
            if (color == null) {
                BatteryColorMosaic(size = 11.dp)
            } else {
                BatteryColorDot(color, size = 11.dp)
            }
        }
    }
}

@Composable
private fun BatteryCommonColorButton(
    color: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color =
            if (selected) {
                Color.White
            } else {
                MiuixTheme.colorScheme.surface
            },
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(if (selected) 4.dp else 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = Color(color),
                border =
                    BorderStroke(
                        1.dp,
                        MiuixTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    ),
            ) {}
        }
    }
}

@Composable
private fun BatteryColorDot(
    color: Int,
    size: Dp = 20.dp,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = Color(color),
        border =
            BorderStroke(
                1.dp,
                MiuixTheme.colorScheme.onSurface.copy(alpha = 0.10f),
            ),
    ) {}
}

@Composable
private fun BatteryColorMosaic(
    size: Dp = 20.dp,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = Color.Transparent,
        border =
            BorderStroke(
                1.dp,
                MiuixTheme.colorScheme.onSurface.copy(alpha = 0.10f),
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .drawCheckerboard(
                        cellSizeDp = 3.dp,
                        lightColor = MiuixTheme.colorScheme.surfaceContainer,
                        darkColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                    ),
        )
    }
}

@Composable
private fun batterySchemeDisplayName(
    library: BatteryColorSchemeLibrary,
    key: String,
): String =
    BatteryBuiltInColorScheme.fromKey(key)?.let { batteryBuiltInName(it) }
        ?: library.customByKey(key)?.let { customSchemeName(it) }
        ?: batteryBuiltInName(BatteryBuiltInColorScheme.HYPEROS)

@Composable
private fun batteryBuiltInName(scheme: BatteryBuiltInColorScheme): String =
    when (scheme) {
        BatteryBuiltInColorScheme.HYPEROS ->
            stringResource(R.string.battery_color_preset_hyperos)
        BatteryBuiltInColorScheme.IOS ->
            stringResource(R.string.battery_color_preset_ios)
        BatteryBuiltInColorScheme.LOW_SATURATION ->
            stringResource(R.string.battery_color_preset_recommended)
    }

@Composable
private fun customSchemeName(scheme: BatteryCustomColorScheme): String =
    scheme.name.ifBlank {
        stringResource(R.string.battery_custom_scheme_default_name, scheme.id)
    }

@Composable
private fun batterySourceLabel(source: BatteryColorSchemeSource): String =
    when (source) {
        BatteryColorSchemeSource.HYPEROS ->
            stringResource(R.string.battery_color_preset_hyperos)
        BatteryColorSchemeSource.IOS ->
            stringResource(R.string.battery_color_preset_ios)
        BatteryColorSchemeSource.LOW_SATURATION ->
            stringResource(R.string.battery_color_preset_recommended)
        BatteryColorSchemeSource.FOLLOW_SYSTEM ->
            stringResource(R.string.battery_color_follow_inversion)
        BatteryColorSchemeSource.CUSTOM ->
            stringResource(R.string.battery_color_source_custom)
    }

@Composable
private fun batterySourceValue(
    entry: BatteryColorSchemeEntry,
    slot: CombinedStatusBatteryColorSlot,
): String =
    batterySchemeEntryColor(entry, slot)?.let(::batteryColorHex)
        ?: stringResource(R.string.battery_color_follow_inversion)

@StringRes
private fun batteryColorSlotLabel(slot: CombinedStatusBatteryColorSlot): Int =
    when (slot) {
        CombinedStatusBatteryColorSlot.NORMAL -> R.string.battery_mode_normal
        CombinedStatusBatteryColorSlot.POWER_SAVE -> R.string.battery_mode_power_save
        CombinedStatusBatteryColorSlot.PERFORMANCE -> R.string.battery_mode_performance
        CombinedStatusBatteryColorSlot.SUPER_POWER_SAVE -> R.string.battery_mode_super_power_save
        CombinedStatusBatteryColorSlot.CHARGING -> R.string.battery_mode_charging
        CombinedStatusBatteryColorSlot.LOW -> R.string.battery_mode_low
    }

private fun schemePageForKey(
    library: BatteryColorSchemeLibrary,
    key: String,
): BatterySchemePage? =
    BatteryBuiltInColorScheme.fromKey(key)?.let { BatterySchemePage.BuiltIn(it) }
        ?: library.customByKey(key)?.let { BatterySchemePage.Custom(it) }

internal fun batteryColorHex(color: Int): String =
    "#%06X".format(color and 0x00FFFFFF)

internal fun batteryColorFromHex(value: String): Int? {
    val normalized = value.removePrefix("#")
    if (normalized.length != 6) return null
    return normalized.toLongOrNull(16)
        ?.toInt()
        ?.or(0xFF000000.toInt())
}

internal fun batteryColorRgb(color: Int): Triple<Int, Int, Int> =
    Triple(
        (color shr 16) and 0xFF,
        (color shr 8) and 0xFF,
        color and 0xFF,
    )

internal fun batteryColorFromRgb(
    red: String,
    green: String,
    blue: String,
): Int? {
    val r = red.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    val g = green.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    val b = blue.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
}
