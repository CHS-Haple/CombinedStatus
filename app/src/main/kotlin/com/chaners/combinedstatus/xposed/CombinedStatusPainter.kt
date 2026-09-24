package com.chaners.combinedstatus.xposed

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal class CombinedStatusPainter(
    context: Context,
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val batteryRing = RectF(10f, 8f, 110f, 108f)
    private val resources = context.resources
    private val theme = context.theme
    private val resourcePackage = context.packageName
    private var cachedWifiResId = 0
    private var cachedWifiDrawable: Drawable? = null

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        model: CombinedStatusRenderModel,
        colors: CombinedStatusColors,
        opacity: Float,
    ) {
        if (width <= 0 || height <= 0) {
            return
        }

        val scale = min(width / CANONICAL_SIZE, height / CANONICAL_SIZE)
        val visualWidth = CANONICAL_SIZE * scale
        val visualHeight = CANONICAL_SIZE * scale
        val save = canvas.save()
        canvas.translate(
            (width - visualWidth) / 2f,
            (height - visualHeight) / 2f,
        )
        canvas.scale(scale, scale)

        drawBattery(canvas, model, colors.batteryTint, opacity)
        drawCenter(canvas, model, colors.primaryTint, opacity)
        drawMobile(canvas, model, colors.primaryTint, opacity)
        canvas.restoreToCount(save)
    }

    private fun drawBattery(
        canvas: Canvas,
        model: CombinedStatusRenderModel,
        batteryTint: Int,
        opacity: Float,
    ) {
        stroke(batteryTint, 48, RING_STROKE, opacity)
        canvas.drawArc(batteryRing, BATTERY_START_DEGREES, BATTERY_MAX_SWEEP, false, paint)

        val sweep = model.batteryPercent * BATTERY_DEGREES_PER_PERCENT
        if (sweep > 0f) {
            stroke(batteryTint, 255, RING_STROKE, opacity)
            canvas.drawArc(batteryRing, BATTERY_START_DEGREES, sweep, false, paint)
        }
    }

    private fun drawCenter(
        canvas: Canvas,
        model: CombinedStatusRenderModel,
        tint: Int,
        opacity: Float,
    ) {
        when (val indicator = model.centerIndicator) {
            is CenterIndicator.Wifi -> {
                drawWifi(canvas, indicator, tint, opacity)
            }

            is CenterIndicator.MobileType -> {
                drawMobileType(canvas, indicator, tint, opacity)
                if (indicator.internet == InternetState.NO_INTERNET) {
                    drawSmallNoInternetMark(canvas, tint, opacity)
                }
            }

            CenterIndicator.Empty -> Unit
        }
    }

    private fun drawWifi(
        canvas: Canvas,
        indicator: CenterIndicator.Wifi,
        tint: Int,
        opacity: Float,
    ) {
        val drawable = resolveWifiDrawable(indicator) ?: return
        val intrinsicWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: WIFI_TARGET_WIDTH.roundToInt()
        val intrinsicHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: WIFI_TARGET_HEIGHT.roundToInt()
        val scale =
            min(
                WIFI_TARGET_WIDTH / intrinsicWidth.toFloat(),
                WIFI_TARGET_HEIGHT / intrinsicHeight.toFloat(),
            )
        val drawWidth = intrinsicWidth * scale
        val drawHeight = intrinsicHeight * scale
        val left = (WIFI_CENTER_X - drawWidth / 2f).roundToInt()
        val top = (WIFI_CENTER_Y - drawHeight / 2f).roundToInt()
        val right = (WIFI_CENTER_X + drawWidth / 2f).roundToInt()
        val bottom = (WIFI_CENTER_Y + drawHeight / 2f).roundToInt()

        drawable.setBounds(left, top, right, bottom)
        drawable.setTint(tint)
        drawable.alpha = effectiveAlpha(tint, 255, opacity)
        drawable.draw(canvas)
    }

    private fun resolveWifiDrawable(
        indicator: CenterIndicator.Wifi,
    ): Drawable? {
        val renderResId = resolveWifiRenderResId(indicator) ?: return null
        if (cachedWifiResId != renderResId || cachedWifiDrawable == null) {
            cachedWifiResId = renderResId
            cachedWifiDrawable =
                runCatching {
                    resources
                        .getDrawable(renderResId, theme)
                        .mutate()
                }.getOrNull()
        }
        return cachedWifiDrawable
    }

    private fun resolveWifiRenderResId(
        indicator: CenterIndicator.Wifi,
    ): Int? {
        val sourceResId = indicator.iconResId.takeIf { it != 0 } ?: return null
        if (indicator.internet != InternetState.NO_INTERNET) {
            return sourceResId
        }

        val entryName =
            runCatching {
                resources.getResourceEntryName(sourceResId)
            }.getOrNull() ?: return null
        val match = WIFI_RESOURCE_PATTERN.matchEntire(entryName) ?: return null
        if (match.groupValues[1].isNotEmpty()) {
            return sourceResId
        }

        val unavailableName =
            "stat_sys_wifi_signal_unavailable_" +
                match.groupValues[2] +
                match.groupValues[3]
        return resources
            .getIdentifier(
                unavailableName,
                "drawable",
                resourcePackage,
            )
            .takeIf { it != 0 }
    }

    private fun drawMobileType(
        canvas: Canvas,
        indicator: CenterIndicator.MobileType,
        tint: Int,
        opacity: Float,
    ) {
        val normalized = indicator.label.trim().uppercase()
        val split =
            when {
                normalized == "5GA" || normalized == "5G-A" || normalized == "5G_A" ->
                    "5G" to "A"
                normalized.startsWith("5G") && normalized.length > 2 ->
                    "5G" to
                        normalized
                            .removePrefix("5G")
                            .removePrefix("-")
                            .removePrefix("_")
                normalized.startsWith("4G") && normalized.length > 2 ->
                    "4G" to
                        normalized
                            .removePrefix("4G")
                            .removePrefix("-")
                            .removePrefix("_")
                indicator.enhanced && normalized == "5G" -> "5G" to "++"
                else -> normalized to ""
            }

        paint.style = Paint.Style.FILL
        paint.color = tint
        paint.alpha = effectiveAlpha(tint, 255, opacity)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = MOBILE_TYPE_TEXT_SIZE

        val mainWidth = paint.measureText(split.first)
        if (split.second.isEmpty()) {
            canvas.drawText(
                split.first,
                MOBILE_TYPE_CENTER_X - mainWidth / 2f,
                MOBILE_TYPE_BASELINE_Y,
                paint,
            )
            return
        }

        paint.textSize = MOBILE_TYPE_SUFFIX_SIZE
        val suffixWidth = paint.measureText(split.second)
        val totalWidth = mainWidth + MOBILE_TYPE_SUFFIX_GAP + suffixWidth
        val startX = MOBILE_TYPE_CENTER_X - totalWidth / 2f

        paint.textSize = MOBILE_TYPE_TEXT_SIZE
        canvas.drawText(split.first, startX, MOBILE_TYPE_BASELINE_Y, paint)
        paint.textSize = MOBILE_TYPE_SUFFIX_SIZE
        canvas.drawText(
            split.second,
            startX + mainWidth + MOBILE_TYPE_SUFFIX_GAP,
            MOBILE_TYPE_SUFFIX_BASELINE_Y,
            paint,
        )
    }

    private fun drawSmallNoInternetMark(
        canvas: Canvas,
        tint: Int,
        opacity: Float,
    ) {
        stroke(tint, 220, 3f, opacity)
        canvas.drawLine(76f, 66f, 84f, 74f, paint)
        canvas.drawLine(84f, 66f, 76f, 74f, paint)
    }

    private fun drawMobile(
        canvas: Canvas,
        model: CombinedStatusRenderModel,
        tint: Int,
        opacity: Float,
    ) {
        val level = model.mobileLevel

        for (index in 0 until MOBILE_DOT_COUNT) {
            fill(
                color = tint,
                alpha = if (level != null && level > index) 255 else 48,
                opacity = opacity,
            )
            val angle = bottomDotAngle(index)
            canvas.drawCircle(
                MOBILE_CENTER_X + cos(angle).toFloat() * MOBILE_ORBIT_RADIUS,
                MOBILE_CENTER_Y + sin(angle).toFloat() * MOBILE_ORBIT_RADIUS,
                MOBILE_DOT_RADIUS,
                paint,
            )
        }

        if (level == null) {
            stroke(tint, 210, 4f, opacity)
            canvas.drawLine(56f, 90f, 64f, 98f, paint)
            canvas.drawLine(64f, 90f, 56f, 98f, paint)
        }
    }

    private fun bottomDotAngle(index: Int): Double {
        val dotHalfAngle = asin((MOBILE_DOT_RADIUS / MOBILE_ORBIT_RADIUS).toDouble())
        val ringGapHalfAngle = asin((3.75f / MOBILE_ORBIT_RADIUS).toDouble())
        val step =
            (
                Math.PI * 2.0 / 3.0 -
                    8.0 * dotHalfAngle -
                    2.0 * ringGapHalfAngle
            ) / 5.3
        val start =
            Math.PI / 6.0 +
                ringGapHalfAngle +
                1.15 * step +
                dotHalfAngle
        return start + (3 - index) * (2.0 * dotHalfAngle + step)
    }

    private fun fill(
        color: Int,
        alpha: Int,
        opacity: Float,
    ) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = effectiveAlpha(color, alpha, opacity)
    }

    private fun stroke(
        color: Int,
        alpha: Int,
        width: Float,
        opacity: Float,
    ) {
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeWidth = width
        paint.color = color
        paint.alpha = effectiveAlpha(color, alpha, opacity)
    }

    private fun effectiveAlpha(
        color: Int,
        alpha: Int,
        opacity: Float,
    ): Int =
        (
            Color.alpha(color) *
                (alpha.coerceIn(0, 255) / 255f) *
                opacity.coerceIn(0f, 1f)
        ).toInt().coerceIn(0, 255)

    private companion object {
        const val CANONICAL_SIZE = 120f
        const val BATTERY_START_DEGREES = 150f
        const val BATTERY_MAX_SWEEP = 240f
        const val BATTERY_DEGREES_PER_PERCENT = 2.4f
        const val RING_STROKE = 7.5f
        const val MOBILE_DOT_COUNT = 4
        const val MOBILE_CENTER_X = 60f
        const val MOBILE_CENTER_Y = 58f
        const val MOBILE_ORBIT_RADIUS = 51f
        const val MOBILE_DOT_RADIUS = 4.9f
        const val MOBILE_TYPE_CENTER_X = 60f
        const val MOBILE_TYPE_BASELINE_Y = 66f
        const val MOBILE_TYPE_SUFFIX_BASELINE_Y = 58f
        const val MOBILE_TYPE_TEXT_SIZE = 28f
        const val MOBILE_TYPE_SUFFIX_SIZE = 17f
        const val MOBILE_TYPE_SUFFIX_GAP = 2f
        const val WIFI_CENTER_X = 60f
        const val WIFI_CENTER_Y = 57f
        const val WIFI_TARGET_WIDTH = 50f
        const val WIFI_TARGET_HEIGHT = 36f
        val WIFI_RESOURCE_PATTERN =
            Regex("^stat_sys_wifi_signal_(unavailable_)?([0-3])(_darkmode|_tint)?$")
    }
}
