package com.example.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.HourlyRadarPoint
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.CardBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.VoidBlack
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class UvRiskTier(
    val minUv: Double,
    val maxUv: Double,
    val color: Color,
    @StringRes val adviceRes: Int
) {
    LOW(0.0, 2.9, Color(0xFF10B981), R.string.advice_uv_low),
    MODERATE(3.0, 5.9, Color(0xFFF59E0B), R.string.advice_uv_moderate),
    HIGH(6.0, 7.9, Color(0xFFF97316), R.string.advice_uv_high),
    VERY_HIGH(8.0, 10.9, Color(0xFFEF4444), R.string.advice_uv_very_high),
    EXTREME(11.0, 20.0, Color(0xFF8B5CF6), R.string.advice_uv_extreme);

    companion object {
        fun fromUv(uv: Double): UvRiskTier = when {
            uv < 2.95 -> LOW
            uv < 5.95 -> MODERATE
            uv < 7.95 -> HIGH
            uv < 10.95 -> VERY_HIGH
            else -> EXTREME
        }
    }
}

/**
 * UvForecastChartCard:
 * D3-grade 24-Hour UV Index Forecast Chart featuring:
 * - Mathematical Monotone Cubic Spline (d3.curveMonotoneX) to prevent ringing/overshoot
 * - Area gradient shading mapped to WHO UV risk tiers
 * - Visual safe sun-exposure window threshold bands (UV < 3.0)
 * - Interactive touch scrubber showing exact hour, UV reading, and sun safety advice
 * - Toggleable sensitivity thresholds (Standard vs Sun-Sensitive)
 */
@Composable
fun UvForecastChartCard(
    hourlyPoints: List<HourlyRadarPoint>,
    modifier: Modifier = Modifier
) {
    if (hourlyPoints.isEmpty()) return

    var isSensitiveMode by remember { mutableStateOf(false) }
    val safeThreshold = if (isSensitiveMode) 2.0 else 3.0

    // Peak UV calculation
    val peakPoint = remember(hourlyPoints) {
        hourlyPoints.maxByOrNull { it.uvIndex } ?: hourlyPoints.first()
    }

    // Determine current/initial selected point (default to peak or first daylight hour)
    var selectedIndex by remember(hourlyPoints) {
        val initialIdx = hourlyPoints.indexOfFirst { it.uvIndex >= safeThreshold }
            .takeIf { it >= 0 } ?: (hourlyPoints.size / 2)
        mutableIntStateOf(initialIdx.coerceIn(0, hourlyPoints.size - 1))
    }

    val activePoint = hourlyPoints.getOrElse(selectedIndex) { hourlyPoints.first() }
    val activeTier = UvRiskTier.fromUv(activePoint.uvIndex)

    // Calculate Safe Exposure Windows summary
    val (safeWindowSummary, hazardWindowSummary) = remember(hourlyPoints, safeThreshold) {
        computeSafeSunWindows(hourlyPoints, safeThreshold)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("uv_forecast_d3_chart"),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Title & Sensitivity Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFBBF24).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = stringResource(R.string.uv_chart_title),
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.uv_chart_title),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.1.sp,
                                color = TextWhite
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.uv_chart_subtitle),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = TextMuted
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Threshold Mode Toggle Button (Standard vs Sensitive)
                Surface(
                    onClick = { isSensitiveMode = !isSensitiveMode },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSensitiveMode) Color(0xFFF59E0B).copy(alpha = 0.2f) else SurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSensitiveMode) Color(0xFFF59E0B) else CardBorder
                    ),
                    modifier = Modifier.testTag("btn_uv_threshold_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isSensitiveMode) Color(0xFFF59E0B) else AeroCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isSensitiveMode) {
                                stringResource(R.string.threshold_sensitive)
                            } else {
                                stringResource(R.string.threshold_standard)
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (isSensitiveMode) Color(0xFFF59E0B) else TextWhite
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Safe Exposure Window Announcement Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0C1322),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.safe_sun_window_title),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp,
                                    color = Color(0xFF10B981)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.safe_window_format, safeWindowSummary),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite
                            )
                        )
                        if (hazardWindowSummary.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    R.string.peak_uv_hazard_format,
                                    hazardWindowSummary,
                                    peakPoint.uvIndex
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFEF4444),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    // Peak UV Pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", peakPoint.uvIndex),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFEF4444)
                                )
                            )
                            Text(
                                text = peakPoint.displayHour,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // D3 Monotone Spline UV Chart Canvas
            D3UvSplineCanvas(
                hourlyPoints = hourlyPoints,
                selectedIndex = selectedIndex,
                safeThreshold = safeThreshold,
                onSelectIndex = { selectedIndex = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Active Inspection Callout Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, activeTier.color.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activePoint.displayHour,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = activeTier.color.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, activeTier.color)
                            ) {
                                Text(
                                    text = "${activeTier.name.replace("_", " ")} (${String.format(java.util.Locale.US, "%.1f", activePoint.uvIndex)} UV)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = activeTier.color,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(activeTier.adviceRes),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 10.5.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Safe / Caution Status Stamp
                    val isSafe = activePoint.uvIndex < safeThreshold
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSafe) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isSafe) "SAFE" else "SUNSCREEN",
                            color = if (isSafe) Color(0xFF10B981) else Color(0xFFEF4444),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                letterSpacing = 0.8.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Hint footer
            Text(
                text = stringResource(R.string.uv_scrubber_hint),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    color = TextMuted.copy(alpha = 0.7f)
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/**
 * Native Jetpack Compose Canvas executing D3's exact Fritsch-Carlson Monotone Cubic Spline
 * algorithm (d3.curveMonotoneX) with gradient area fill and interactive touch scrubber.
 */
@Composable
private fun D3UvSplineCanvas(
    hourlyPoints: List<HourlyRadarPoint>,
    selectedIndex: Int,
    safeThreshold: Double,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .pointerInput(hourlyPoints.size) {
                detectTapGestures { offset ->
                    val stepX = size.width / (hourlyPoints.size - 1).coerceAtLeast(1)
                    val idx = (offset.x / stepX).roundToInt().coerceIn(0, hourlyPoints.size - 1)
                    onSelectIndex(idx)
                }
            }
            .pointerInput(hourlyPoints.size) {
                detectDragGestures { change, _ ->
                    val stepX = size.width / (hourlyPoints.size - 1).coerceAtLeast(1)
                    val idx = (change.position.x / stepX).roundToInt().coerceIn(0, hourlyPoints.size - 1)
                    onSelectIndex(idx)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(170.dp)) {
            val w = size.width
            val h = size.height

            val padTop = 20f
            val padBottom = 26f
            val chartH = h - padTop - padBottom
            val chartW = w

            val maxUvValue = max(11.0, (hourlyPoints.maxOfOrNull { it.uvIndex } ?: 10.0) + 1.0)
            val pointsCount = hourlyPoints.size
            val stepX = chartW / (pointsCount - 1).coerceAtLeast(1)

            // Convert data points to canvas coordinates
            val canvasPoints = hourlyPoints.mapIndexed { i, pt ->
                val x = i * stepX
                val normalizedY = (pt.uvIndex / maxUvValue).coerceIn(0.0, 1.0).toFloat()
                val y = padTop + chartH * (1f - normalizedY)
                Offset(x, y)
            }

            // 1. Draw Safe Sun Exposure Zone (UV < safeThreshold)
            val safeZoneNormalized = (safeThreshold / maxUvValue).coerceIn(0.0, 1.0).toFloat()
            val safeZoneTopY = padTop + chartH * (1f - safeZoneNormalized)
            val baselineY = padTop + chartH

            drawRect(
                color = Color(0xFF10B981).copy(alpha = 0.07f),
                topLeft = Offset(0f, safeZoneTopY),
                size = androidx.compose.ui.geometry.Size(chartW, baselineY - safeZoneTopY)
            )

            // Dashed safe threshold line
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            drawLine(
                color = Color(0xFF10B981).copy(alpha = 0.5f),
                start = Offset(0f, safeZoneTopY),
                end = Offset(chartW, safeZoneTopY),
                strokeWidth = 1.5f,
                pathEffect = dashEffect
            )

            // High UV (6.0) guide line
            val highUvNormalized = (6.0 / maxUvValue).coerceIn(0.0, 1.0).toFloat()
            val highUvTopY = padTop + chartH * (1f - highUvNormalized)
            drawLine(
                color = Color(0xFFEF4444).copy(alpha = 0.25f),
                start = Offset(0f, highUvTopY),
                end = Offset(chartW, highUvTopY),
                strokeWidth = 1f,
                pathEffect = dashEffect
            )

            // Baseline
            drawLine(
                color = CardBorder,
                start = Offset(0f, baselineY),
                end = Offset(chartW, baselineY),
                strokeWidth = 1.5f
            )

            // 2. Build D3 Monotone Cubic Spline Area and Line
            val areaPath = buildD3MonotoneSplinePath(canvasPoints, areaPath = true, baselineY = baselineY)
            val strokePath = buildD3MonotoneSplinePath(canvasPoints, areaPath = false)

            // Fill area with multi-stop vertical gradient reflecting UV intensity
            val areaBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFEF4444).copy(alpha = 0.45f), // High UV red peak
                    Color(0xFFF59E0B).copy(alpha = 0.35f), // Mid UV amber
                    Color(0xFF10B981).copy(alpha = 0.20f), // Low UV safe emerald
                    Color(0xFF00E5FF).copy(alpha = 0.03f)  // Cyan base fade
                ),
                startY = padTop,
                endY = baselineY
            )
            drawPath(path = areaPath, brush = areaBrush)

            // Stroke D3 Monotone Curve
            val strokeBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF10B981),
                    Color(0xFFF59E0B),
                    Color(0xFFEF4444),
                    Color(0xFFF59E0B),
                    Color(0xFF10B981)
                ),
                startX = 0f,
                endX = chartW
            )
            drawPath(
                path = strokePath,
                brush = strokeBrush,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // 3. Draw Active Touch Cursor & Ring Indicator
            val activePt = canvasPoints.getOrElse(selectedIndex) { canvasPoints.first() }
            val activeUv = hourlyPoints.getOrElse(selectedIndex) { hourlyPoints.first() }.uvIndex
            val activeColor = UvRiskTier.fromUv(activeUv).color

            // Vertical indicator guideline
            drawLine(
                color = TextWhite.copy(alpha = 0.4f),
                start = Offset(activePt.x, padTop),
                end = Offset(activePt.x, baselineY),
                strokeWidth = 1.5f,
                pathEffect = dashEffect
            )

            // Glowing Outer Ring
            drawCircle(
                color = activeColor.copy(alpha = 0.3f),
                radius = 12.dp.toPx(),
                center = activePt
            )
            // Solid Center Point
            drawCircle(
                color = activeColor,
                radius = 5.dp.toPx(),
                center = activePt
            )
            drawCircle(
                color = VoidBlack,
                radius = 2.5.dp.toPx(),
                center = activePt
            )

            // 4. Time Axis Label Ticks (every 4 hours: 0, 4, 8, 12, 16, 20)
            val labelInterval = 4
            for (i in 0 until pointsCount step labelInterval) {
                val tickX = i * stepX
                drawLine(
                    color = CardBorder,
                    start = Offset(tickX, baselineY),
                    end = Offset(tickX, baselineY + 4.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

/**
 * Fritsch-Carlson Monotone Cubic Spline (d3.curveMonotoneX):
 * Guarantees that the interpolated curve between points does not introduce
 * extraneous extrema or oscillations, preserving the real shape of meteorological curves.
 */
private fun buildD3MonotoneSplinePath(
    points: List<Offset>,
    areaPath: Boolean = false,
    baselineY: Float = 0f
): Path {
    val path = Path()
    if (points.isEmpty()) return path
    if (points.size == 1) {
        path.moveTo(points[0].x, points[0].y)
        return path
    }

    val n = points.size
    val deltas = FloatArray(n - 1)
    val slopes = FloatArray(n - 1)

    for (i in 0 until n - 1) {
        val dx = points[i + 1].x - points[i].x
        val dy = points[i + 1].y - points[i].y
        deltas[i] = dx
        slopes[i] = if (dx != 0f) dy / dx else 0f
    }

    val tangents = FloatArray(n)
    tangents[0] = slopes[0]
    for (i in 1 until n - 1) {
        if (slopes[i - 1] * slopes[i] <= 0f) {
            tangents[i] = 0f
        } else {
            val dx0 = deltas[i - 1]
            val dx1 = deltas[i]
            val s0 = slopes[i - 1]
            val s1 = slopes[i]
            tangents[i] = 3f * (dx0 + dx1) / ((2f * dx1 + dx0) / s0 + (dx1 + 2f * dx0) / s1)
        }
    }
    tangents[n - 1] = slopes[n - 2]

    path.moveTo(points[0].x, points[0].y)
    for (i in 0 until n - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val dx = deltas[i]
        val cp1x = p0.x + dx / 3f
        val cp1y = p0.y + tangents[i] * (dx / 3f)
        val cp2x = p1.x - dx / 3f
        val cp2y = p1.y - tangents[i + 1] * (dx / 3f)
        path.cubicTo(cp1x, cp1y, cp2x, cp2y, p1.x, p1.y)
    }

    if (areaPath) {
        path.lineTo(points.last().x, baselineY)
        path.lineTo(points.first().x, baselineY)
        path.close()
    }

    return path
}

/**
 * Computes human-friendly safe exposure window descriptions:
 * e.g. "Before 10:00 & After 16:00" and hazard window "11:00 – 15:00".
 */
private fun computeSafeSunWindows(
    points: List<HourlyRadarPoint>,
    threshold: Double
): Pair<String, String> {
    val breachPoints = points.filter { it.uvIndex >= threshold }
    if (breachPoints.isEmpty()) {
        return Pair("Continuous safe exposure all day", "")
    }

    val firstBreach = breachPoints.first()
    val lastBreach = breachPoints.last()

    val safeDesc = "Before ${firstBreach.displayHour} & After ${lastBreach.displayHour}"
    val hazardDesc = "${firstBreach.displayHour} – ${lastBreach.displayHour}"
    return Pair(safeDesc, hazardDesc)
}
