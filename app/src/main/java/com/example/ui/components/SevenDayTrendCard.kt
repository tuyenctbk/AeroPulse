package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.DailyTrendPoint
import com.example.domain.UserProfile
import com.example.domain.VerdictLevel
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.HazardRed
import com.example.ui.theme.PillSurface
import com.example.ui.theme.PulseEmerald
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.VoidBlack
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * SevenDayTrendCard:
 * D3 continuous monotone spline visualization comparing historical (past 3 days)
 * with forecast (today + upcoming 3 days) Go/No-Go outdoor activity scores.
 */
@Composable
fun SevenDayTrendCard(
    trendPoints: List<DailyTrendPoint>,
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    if (trendPoints.isEmpty()) return

    // Find index of Today (default selection)
    val todayIdx = remember(trendPoints) {
        val idx = trendPoints.indexOfFirst { it.isToday }
        if (idx >= 0) idx else (trendPoints.size / 2).coerceIn(0, trendPoints.size - 1)
    }

    var selectedIndex by remember(trendPoints) {
        mutableIntStateOf(todayIdx)
    }

    val selectedPoint = trendPoints.getOrElse(selectedIndex) { trendPoints.first() }

    // Statistical Computations
    val avgScore = remember(trendPoints) {
        trendPoints.map { it.score }.average().roundToInt()
    }

    val bestPoint = remember(trendPoints) {
        trendPoints.maxByOrNull { it.score } ?: trendPoints.first()
    }

    // Historical mean vs Forecast mean to determine overall trend trajectory
    val historicalMean = remember(trendPoints) {
        val hist = trendPoints.filter { it.isHistorical }
        if (hist.isNotEmpty()) hist.map { it.score }.average() else avgScore.toDouble()
    }

    val forecastMean = remember(trendPoints) {
        val fcst = trendPoints.filter { !it.isHistorical }
        if (fcst.isNotEmpty()) fcst.map { it.score }.average() else avgScore.toDouble()
    }

    val isImproving = forecastMean >= historicalMean

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("seven_day_trend_card"),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Title & 7-Day Average Metric
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0F1E33),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AeroCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = AeroCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.seven_day_trend_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Text(
                            text = stringResource(R.string.seven_day_trend_subtitle),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        avgScore >= 75 -> PulseEmerald
                                        avgScore >= 45 -> SolarAmber
                                        else -> HazardRed
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.trend_avg_score_format, avgScore),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Outlook & Best Day Summary Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0B1626))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isImproving) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (isImproving) PulseEmerald else SolarAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            if (isImproving) R.string.trend_outlook_improving else R.string.trend_outlook_declining
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isImproving) PulseEmerald else SolarAmber,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }

                val bestStatus = stringResource(bestPoint.level.shortLabelRes)
                val bestLabel = if (bestPoint.isToday) stringResource(R.string.trend_today_badge) else bestPoint.dayOfWeek
                Text(
                    text = stringResource(R.string.trend_best_day_format, bestLabel, bestPoint.score, bestStatus),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // D3 Continuous Spline Canvas Chart
            D3SevenDaySplineChart(
                trendPoints = trendPoints,
                selectedIndex = selectedIndex,
                todayIndex = todayIdx,
                onSelectIndex = { selectedIndex = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Day Selector Strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(trendPoints) { index, pt ->
                    val isSelected = index == selectedIndex
                    val chipBg = if (isSelected) Color(0xFF0F253E) else SurfaceElevated
                    val chipBorder = if (isSelected) AeroCyan else CardBorder

                    Surface(
                        onClick = { selectedIndex = index },
                        shape = RoundedCornerShape(14.dp),
                        color = chipBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder),
                        modifier = Modifier
                            .dpadFocusable(RoundedCornerShape(14.dp))
                            .testTag("trend_day_chip_$index")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (pt.isToday) stringResource(R.string.trend_today_badge) else pt.dayOfWeek.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected || pt.isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (pt.isToday) AeroCyan else TextWhite,
                                    fontSize = 10.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Surface(
                                shape = CircleShape,
                                color = pt.level.color.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, pt.level.color.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "${pt.score}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = pt.level.color,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = if (pt.isHistorical) stringResource(R.string.trend_historical_label) else stringResource(R.string.trend_forecast_label),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    color = if (pt.isHistorical) TextMuted else AeroCyan.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected Day Detailed Breakdown Card
            SelectedDayDetailCard(
                point = selectedPoint,
                profile = profile
            )
        }
    }
}

@Composable
private fun D3SevenDaySplineChart(
    trendPoints: List<DailyTrendPoint>,
    selectedIndex: Int,
    todayIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val count = trendPoints.size
    if (count < 2) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF080E18))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .pointerInput(trendPoints) {
                detectTapGestures { offset ->
                    val stepX = size.width / (count - 1).coerceAtLeast(1)
                    val idx = (offset.x / stepX).roundToInt().coerceIn(0, count - 1)
                    onSelectIndex(idx)
                }
            }
            .pointerInput(trendPoints) {
                detectDragGestures { change, _ ->
                    val stepX = size.width / (count - 1).coerceAtLeast(1)
                    val idx = (change.position.x / stepX).roundToInt().coerceIn(0, count - 1)
                    onSelectIndex(idx)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val w = size.width
            val h = size.height

            val padTop = 26f
            val padBottom = 32f
            val chartH = h - padTop - padBottom
            val chartW = w
            val stepX = chartW / (count - 1).coerceAtLeast(1)

            // 1. Shaded Historical (Past) vs Forecast Zones
            val todayX = todayIndex * stepX
            // Historical zone rectangle (0 to todayX)
            drawRect(
                color = Color(0xFF1E293B).copy(alpha = 0.18f),
                topLeft = Offset(0f, 0f),
                size = Size(todayX, h)
            )

            // Forecast zone rectangle (todayX to width)
            drawRect(
                color = Color(0xFF0F172A).copy(alpha = 0.08f),
                topLeft = Offset(todayX, 0f),
                size = Size(chartW - todayX, h)
            )

            // Dashed Divider separating Historical from Forecast
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            drawLine(
                color = AeroCyan.copy(alpha = 0.5f),
                start = Offset(todayX, padTop - 10f),
                end = Offset(todayX, h - padBottom + 10f),
                strokeWidth = 1.5f,
                pathEffect = dashEffect
            )

            // 2. Threshold Reference Lines (GO >= 75 pts, CAUTION >= 45 pts)
            val goY = padTop + chartH * (1f - (75f / 100f))
            val cautionY = padTop + chartH * (1f - (45f / 100f))
            val baselineY = padTop + chartH

            // GO threshold (Emerald)
            drawLine(
                color = PulseEmerald.copy(alpha = 0.35f),
                start = Offset(0f, goY),
                end = Offset(chartW, goY),
                strokeWidth = 1f,
                pathEffect = dashEffect
            )

            // CAUTION threshold (Amber)
            drawLine(
                color = SolarAmber.copy(alpha = 0.25f),
                start = Offset(0f, cautionY),
                end = Offset(chartW, cautionY),
                strokeWidth = 1f,
                pathEffect = dashEffect
            )

            // Baseline
            drawLine(
                color = CardBorder,
                start = Offset(0f, baselineY),
                end = Offset(chartW, baselineY),
                strokeWidth = 1.2f
            )

            // 3. Map Data Points to Canvas Coordinates
            val canvasPoints = trendPoints.mapIndexed { i, pt ->
                val x = i * stepX
                val normalizedY = (pt.score / 100f).coerceIn(0f, 1f)
                val y = padTop + chartH * (1f - normalizedY)
                Offset(x, y)
            }

            // 4. Build D3 Monotone Cubic Spline Paths (d3.curveMonotoneX)
            val areaPath = buildD3MonotoneSplinePath(canvasPoints, areaPath = true, baselineY = baselineY)
            val strokePath = buildD3MonotoneSplinePath(canvasPoints, areaPath = false)

            // Multi-stop Vertical Area Fill Gradient
            val areaBrush = Brush.verticalGradient(
                colors = listOf(
                    PulseEmerald.copy(alpha = 0.40f),
                    SolarAmber.copy(alpha = 0.25f),
                    HazardRed.copy(alpha = 0.10f),
                    VoidBlack.copy(alpha = 0.02f)
                ),
                startY = padTop,
                endY = baselineY
            )
            drawPath(path = areaPath, brush = areaBrush)

            // Multi-stop Horizontal Stroke Gradient
            val strokeBrush = Brush.horizontalGradient(
                colors = trendPoints.map { it.level.color },
                startX = 0f,
                endX = chartW
            )
            drawPath(
                path = strokePath,
                brush = strokeBrush,
                style = Stroke(
                    width = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // 5. Draw Node Circles and Score Dots
            canvasPoints.forEachIndexed { i, pt ->
                val pointData = trendPoints[i]
                val isNodeSelected = (i == selectedIndex)
                val isTodayNode = (i == todayIndex)

                // Outer Halo for Today or Selected Node
                if (isNodeSelected || isTodayNode) {
                    drawCircle(
                        color = (if (isNodeSelected) AeroCyan else PulseEmerald).copy(alpha = 0.35f),
                        radius = (if (isNodeSelected) 11.dp else 8.dp).toPx(),
                        center = pt
                    )
                }

                // Node Center
                drawCircle(
                    color = pointData.level.color,
                    radius = 4.5.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = VoidBlack,
                    radius = 2.dp.toPx(),
                    center = pt
                )
            }

            // 6. Draw Selected Index Scrubber Line
            val activePt = canvasPoints.getOrElse(selectedIndex) { canvasPoints.first() }
            drawLine(
                color = TextWhite.copy(alpha = 0.6f),
                start = Offset(activePt.x, padTop - 8f),
                end = Offset(activePt.x, baselineY),
                strokeWidth = 1.5f,
                pathEffect = dashEffect
            )
        }
    }
}

@Composable
private fun SelectedDayDetailCard(
    point: DailyTrendPoint,
    profile: UserProfile
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Day Header with Score Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (point.isToday) {
                                stringResource(R.string.trend_today_badge) + " • " + point.dayOfWeek
                            } else {
                                point.dayOfWeek + ", " + point.dateIso
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (point.isHistorical) DeepNavy else Color(0xFF0A2234)
                        ) {
                            Text(
                                text = if (point.isHistorical) {
                                    stringResource(R.string.trend_historical_label)
                                } else {
                                    stringResource(R.string.trend_forecast_label)
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (point.isHistorical) TextMuted else AeroCyan
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(point.level.labelRes),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = point.level.color,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = point.level.color.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, point.level.color)
                ) {
                    Text(
                        text = "${point.score}/100",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = point.level.color
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Environmental Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Temp Range
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F1A29)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.trend_temp_range_format, point.maxTemp, point.minTemp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = stringResource(R.string.metric_temp_high_low),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                color = TextMuted
                            )
                        )
                    }
                }

                // Metric 2: Peak UV
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F1A29)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.trend_peak_uv_format, point.peakUv),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (point.peakUv >= 6.0) SolarAmber else PulseEmerald,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = stringResource(R.string.metric_solar_uv_index),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                color = TextMuted
                            )
                        )
                    }
                }

                // Metric 3: Avg PM2.5
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F1A29)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.trend_avg_pm25_format, point.avgPm25),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (point.avgPm25 > 25.0) HazardRed else TextWhite,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = stringResource(R.string.metric_particle_load),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                color = TextMuted
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Factor Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PillSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AeroCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (point.limitingFactorArg != null) {
                            stringResource(point.limitingFactorRes, point.limitingFactorArg)
                        } else {
                            stringResource(point.limitingFactorRes)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextWhite,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
