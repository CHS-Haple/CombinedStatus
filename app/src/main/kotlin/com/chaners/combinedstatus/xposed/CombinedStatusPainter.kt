package com.chaners.combinedstatus.xposed

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal class CombinedStatusPainter {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val batteryRing = RectF(10f, 8f, 110f, 108f)
    private val wifiPaths = arrayOf(
        wifiPathLow(),
        wifiPathMid(),
        wifiPathHigh(),
    )

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
        drawCenter(canvas, model, colors.primaryTint, opacity, scale)
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
        scale: Float,
    ) {
        when (val indicator = model.centerIndicator) {
            is CenterIndicator.Wifi -> {
                drawWifi(canvas, indicator.segments, tint, opacity)
                if (indicator.internet == InternetState.NO_INTERNET) {
                    drawSmallNoInternetMark(canvas, tint, opacity)
                }
            }

            is CenterIndicator.MobileType -> {
                drawMobileType(canvas, indicator, tint, opacity, scale)
                if (indicator.internet == InternetState.NO_INTERNET) {
                    drawSmallNoInternetMark(canvas, tint, opacity)
                }
            }

            CenterIndicator.Empty -> Unit
        }
    }

    private fun drawWifi(
        canvas: Canvas,
        segments: Int,
        tint: Int,
        opacity: Float,
    ) {
        val save = canvas.save()
        canvas.translate(30f, 27f)
        canvas.scale(3f, 3f)

        wifiPaths.forEachIndexed { index, path ->
            fill(
                color = tint,
                alpha = if (index < segments) 255 else 102,
                opacity = opacity,
            )
            canvas.drawPath(path, paint)
        }
        canvas.restoreToCount(save)
    }

    private fun drawMobileType(
        canvas: Canvas,
        indicator: CenterIndicator.MobileType,
        tint: Int,
        opacity: Float,
        scale: Float,
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
        val typography = indicator.typography
        val nativeMainTypeface =
            typography?.mainTypeface
                ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val mainTypeface =
            nativeMainTypeface.withMinimumWeight(MOBILE_TYPE_MIN_WEIGHT)
        val mainTextSize =
            (
                typography?.mainTextSizePx
                    ?.takeIf { it > 0f && scale > 0f }
                    ?.div(scale)
                    ?: MOBILE_TYPE_TEXT_SIZE
            ).coerceAtLeast(MOBILE_TYPE_TEXT_SIZE)
        val nativeSuffixTypeface =
            typography?.suffixTypeface
                ?: nativeMainTypeface
        val suffixTypeface =
            nativeSuffixTypeface.withMinimumWeight(MOBILE_TYPE_MIN_WEIGHT)
        val suffixTextSize =
            (
                typography?.suffixTextSizePx
                    ?.takeIf { it > 0f && scale > 0f }
                    ?.div(scale)
                    ?: MOBILE_TYPE_SUFFIX_SIZE
            ).coerceAtLeast(MOBILE_TYPE_SUFFIX_SIZE)
        paint.typeface = mainTypeface
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = mainTextSize

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

        paint.typeface = suffixTypeface
        paint.textSize = suffixTextSize
        val suffixWidth = paint.measureText(split.second)
        val totalWidth = mainWidth + MOBILE_TYPE_SUFFIX_GAP + suffixWidth
        val startX = MOBILE_TYPE_CENTER_X - totalWidth / 2f

        paint.typeface = mainTypeface
        paint.textSize = mainTextSize
        canvas.drawText(split.first, startX, MOBILE_TYPE_BASELINE_Y, paint)
        paint.typeface = suffixTypeface
        paint.textSize = suffixTextSize
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

    private fun Typeface.withMinimumWeight(minWeight: Int): Typeface =
        runCatching {
            Typeface.create(
                this,
                weight.coerceAtLeast(minWeight),
                isItalic,
            )
        }.getOrDefault(this)

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

    private fun wifiPathLow(): Path =
        Path().apply {
            moveTo(9.778f, 15.752f)
            cubicTo(9.63f, 15.704f, 9.503f, 15.572f, 9.249f, 15.31f)
            lineTo(7.905f, 13.923f)
            cubicTo(7.729f, 13.742f, 7.641f, 13.652f, 7.61f, 13.528f)
            cubicTo(7.586f, 13.432f, 7.601f, 13.293f, 7.644f, 13.204f)
            cubicTo(7.7f, 13.089f, 7.786f, 13.031f, 7.958f, 12.915f)
            cubicTo(8.548f, 12.516f, 9.259f, 12.283f, 10.025f, 12.283f)
            cubicTo(10.762f, 12.283f, 11.448f, 12.499f, 12.025f, 12.87f)
            cubicTo(12.202f, 12.984f, 12.29f, 13.041f, 12.349f, 13.156f)
            cubicTo(12.394f, 13.246f, 12.41f, 13.387f, 12.386f, 13.484f)
            cubicTo(12.355f, 13.61f, 12.266f, 13.701f, 12.088f, 13.885f)
            lineTo(10.707f, 15.31f)
            cubicTo(10.453f, 15.572f, 10.325f, 15.704f, 10.178f, 15.752f)
            cubicTo(10.048f, 15.796f, 9.908f, 15.796f, 9.778f, 15.752f)
            close()
        }

    private fun wifiPathMid(): Path =
        Path().apply {
            moveTo(5.678f, 11.626f)
            cubicTo(5.865f, 11.82f, 5.959f, 11.917f, 6.057f, 11.954f)
            cubicTo(6.152f, 11.991f, 6.228f, 11.997f, 6.327f, 11.976f)
            cubicTo(6.43f, 11.954f, 6.554f, 11.861f, 6.801f, 11.675f)
            cubicTo(7.7f, 11.001f, 8.816f, 10.602f, 10.025f, 10.602f)
            cubicTo(11.214f, 10.602f, 12.312f, 10.988f, 13.202f, 11.64f)
            cubicTo(13.449f, 11.821f, 13.572f, 11.912f, 13.674f, 11.933f)
            cubicTo(13.773f, 11.953f, 13.849f, 11.947f, 13.943f, 11.91f)
            cubicTo(14.04f, 11.872f, 14.132f, 11.777f, 14.318f, 11.586f)
            lineTo(14.894f, 10.991f)
            cubicTo(15.078f, 10.802f, 15.169f, 10.707f, 15.202f, 10.59f)
            cubicTo(15.228f, 10.493f, 15.221f, 10.372f, 15.182f, 10.279f)
            cubicTo(15.135f, 10.167f, 15.042f, 10.092f, 14.854f, 9.942f)
            cubicTo(13.531f, 8.883f, 11.852f, 8.249f, 10.025f, 8.249f)
            cubicTo(8.171f, 8.249f, 6.47f, 8.901f, 5.138f, 9.989f)
            cubicTo(4.954f, 10.14f, 4.861f, 10.215f, 4.816f, 10.327f)
            cubicTo(4.778f, 10.419f, 4.771f, 10.54f, 4.798f, 10.636f)
            cubicTo(4.83f, 10.752f, 4.921f, 10.846f, 5.103f, 11.033f)
            lineTo(5.678f, 11.626f)
            close()
        }

    private fun wifiPathHigh(): Path =
        Path().apply {
            moveTo(16.025f, 8.728f)
            cubicTo(16.248f, 8.912f, 16.359f, 9.004f, 16.464f, 9.031f)
            cubicTo(16.562f, 9.055f, 16.649f, 9.05f, 16.744f, 9.015f)
            cubicTo(16.846f, 8.978f, 16.939f, 8.882f, 17.125f, 8.69f)
            lineTo(17.702f, 8.094f)
            cubicTo(17.886f, 7.904f, 17.978f, 7.809f, 18.011f, 7.695f)
            cubicTo(18.039f, 7.598f, 18.034f, 7.483f, 17.997f, 7.39f)
            cubicTo(17.954f, 7.279f, 17.859f, 7.198f, 17.67f, 7.036f)
            cubicTo(15.613f, 5.277f, 12.943f, 4.215f, 10.025f, 4.215f)
            cubicTo(7.081f, 4.215f, 4.389f, 5.296f, 2.326f, 7.083f)
            cubicTo(2.139f, 7.245f, 2.045f, 7.327f, 2.002f, 7.437f)
            cubicTo(1.966f, 7.53f, 1.961f, 7.645f, 1.989f, 7.741f)
            cubicTo(2.022f, 7.855f, 2.114f, 7.95f, 2.297f, 8.139f)
            lineTo(2.873f, 8.734f)
            cubicTo(3.061f, 8.927f, 3.154f, 9.023f, 3.257f, 9.061f)
            cubicTo(3.352f, 9.096f, 3.439f, 9.1f, 3.537f, 9.075f)
            cubicTo(3.643f, 9.048f, 3.754f, 8.955f, 3.977f, 8.768f)
            cubicTo(5.613f, 7.395f, 7.722f, 6.568f, 10.025f, 6.568f)
            cubicTo(12.306f, 6.568f, 14.396f, 7.379f, 16.025f, 8.728f)
            close()
        }

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
        const val MOBILE_TYPE_MIN_WEIGHT = 600
    }
}
