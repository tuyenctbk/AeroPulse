package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.OutdoorSpot
import com.example.domain.MetricStatus
import com.example.domain.SpotEvaluationResult
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.CardBorder
import com.example.ui.theme.PillSurface
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.VoidBlack
import kotlin.math.roundToInt

@Composable
fun OutdoorSpotCard(
    spots: List<OutdoorSpot>,
    selectedSpot: OutdoorSpot?,
    evaluation: SpotEvaluationResult?,
    isLoading: Boolean,
    onSelectSpot: (OutdoorSpot) -> Unit,
    onSaveSpot: (OutdoorSpot) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditingThresholds by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("outdoor_spot_card"),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AeroCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = AeroCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.outdoor_spots_title),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                color = TextWhite
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.outdoor_spot_subtitle),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = TextMuted
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedSpot != null) {
                        IconButton(
                            onClick = { isEditingThresholds = !isEditingThresholds },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = stringResource(R.string.edit_thresholds),
                                tint = if (isEditingThresholds) AeroCyan else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = AeroCyan
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.cd_refresh),
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Spot Selector Chips Row
            LazyRow(
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(spots) { spot ->
                    val isSelected = spot.id == selectedSpot?.id
                    val bg = if (isSelected) AeroCyan else SurfaceElevated
                    val textColor = if (isSelected) VoidBlack else TextWhite
                    val borderColor = if (isSelected) AeroCyan else CardBorder

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(bg)
                            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable { onSelectSpot(spot) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = spot.name,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Spot Details & Go/No-Go Evaluation
            if (selectedSpot != null) {
                if (isEditingThresholds) {
                    ThresholdEditor(
                        spot = selectedSpot,
                        onSave = { updated ->
                            onSaveSpot(updated)
                            isEditingThresholds = false
                        },
                        onCancel = { isEditingThresholds = false }
                    )
                } else if (evaluation != null) {
                    // Spot Go/No-Go Status Header Banner
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = evaluation.verdictLevel.color.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, evaluation.verdictLevel.color.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedSpot.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(
                                        R.string.spot_coordinates_format,
                                        selectedSpot.latitude,
                                        selectedSpot.longitude
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                )
                                if (selectedSpot.isSunSensitive) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = stringResource(R.string.sun_sensitive_label),
                                            color = Color(0xFFF59E0B),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(evaluation.headlineRes),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = evaluation.verdictLevel.color,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                if (evaluation.isSunSensitiveBreach && evaluation.factorArg != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.status_uv_breach, evaluation.factorArg),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFEF4444),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Score Dial / Badge
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = evaluation.verdictLevel.color,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${evaluation.score}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = VoidBlack
                                            )
                                        )
                                        Text(
                                            text = stringResource(evaluation.verdictLevel.shortLabelRes),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VoidBlack
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Threshold Evaluation Grid Header
                    Text(
                        text = stringResource(R.string.spot_evaluation_header),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold,
                            color = AeroCyan
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Metric Comparison Items
                    evaluation.metricEvaluations.forEach { metric ->
                        MetricRow(metric = metric)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_outdoor_spots_saved),
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricRow(metric: com.example.domain.SpotMetricEvaluation) {
    val (statusTextRes, statusBg, statusColor) = when (metric.status) {
        MetricStatus.PASS -> Triple(R.string.metric_status_pass, Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF10B981))
        MetricStatus.WARN -> Triple(R.string.metric_status_warn, Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFF59E0B))
        MetricStatus.BREACH -> Triple(R.string.metric_status_breach, Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFEF4444))
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(metric.nameRes),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "${stringResource(R.string.current_reading_label, metric.currentValue, metric.unit)} • ${stringResource(R.string.user_threshold_label, metric.thresholdValue, metric.unit)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusBg,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = stringResource(statusTextRes),
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ThresholdEditor(
    spot: OutdoorSpot,
    onSave: (OutdoorSpot) -> Unit,
    onCancel: () -> Unit
) {
    var pm25 by remember(spot) { mutableDoubleStateOf(spot.maxPm25) }
    var pm10 by remember(spot) { mutableDoubleStateOf(spot.maxPm10) }
    var grass by remember(spot) { mutableDoubleStateOf(spot.maxGrassPollen) }
    var ragweed by remember(spot) { mutableDoubleStateOf(spot.maxRagweedPollen) }
    var uvIndex by remember(spot) { mutableDoubleStateOf(spot.maxUvIndex) }
    var isSunSensitive by remember(spot) { mutableStateOf(spot.isSunSensitive) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0C1322),
        border = androidx.compose.foundation.BorderStroke(1.dp, AeroCyan.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${stringResource(R.string.edit_thresholds)}: ${spot.name}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = AeroCyan
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // PM2.5 Slider
            Text(
                text = "${stringResource(R.string.threshold_pm25)}: ${pm25.roundToInt()} µg/m³",
                style = MaterialTheme.typography.labelSmall.copy(color = TextWhite)
            )
            Slider(
                value = pm25.toFloat(),
                onValueChange = { pm25 = it.toDouble() },
                valueRange = 5f..60f,
                colors = SliderDefaults.colors(thumbColor = AeroCyan, activeTrackColor = AeroCyan)
            )

            // PM10 Slider
            Text(
                text = "${stringResource(R.string.threshold_pm10)}: ${pm10.roundToInt()} µg/m³",
                style = MaterialTheme.typography.labelSmall.copy(color = TextWhite)
            )
            Slider(
                value = pm10.toFloat(),
                onValueChange = { pm10 = it.toDouble() },
                valueRange = 10f..100f,
                colors = SliderDefaults.colors(thumbColor = AeroCyan, activeTrackColor = AeroCyan)
            )

            // Grass Pollen Slider
            Text(
                text = "${stringResource(R.string.threshold_grass_pollen)}: ${grass.roundToInt()} gr/m³",
                style = MaterialTheme.typography.labelSmall.copy(color = TextWhite)
            )
            Slider(
                value = grass.toFloat(),
                onValueChange = { grass = it.toDouble() },
                valueRange = 5f..50f,
                colors = SliderDefaults.colors(thumbColor = AeroCyan, activeTrackColor = AeroCyan)
            )

            // Ragweed Pollen Slider
            Text(
                text = "${stringResource(R.string.threshold_ragweed_pollen)}: ${ragweed.roundToInt()} gr/m³",
                style = MaterialTheme.typography.labelSmall.copy(color = TextWhite)
            )
            Slider(
                value = ragweed.toFloat(),
                onValueChange = { ragweed = it.toDouble() },
                valueRange = 2f..40f,
                colors = SliderDefaults.colors(thumbColor = AeroCyan, activeTrackColor = AeroCyan)
            )

            // Solar UV Slider
            Text(
                text = "${stringResource(R.string.threshold_uv_index)}: ${String.format(java.util.Locale.US, "%.1f", uvIndex)} ${stringResource(R.string.uv_index_unit)}",
                style = MaterialTheme.typography.labelSmall.copy(color = TextWhite)
            )
            Slider(
                value = uvIndex.toFloat(),
                onValueChange = { uvIndex = it.toDouble() },
                valueRange = 1f..12f,
                steps = 21,
                colors = SliderDefaults.colors(thumbColor = AeroCyan, activeTrackColor = AeroCyan),
                modifier = Modifier.testTag("slider_uv_threshold")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sun-Sensitive Mode Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceElevated)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.sun_sensitive_label),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    )
                    Text(
                        text = stringResource(R.string.sun_sensitive_desc),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isSunSensitive,
                    onCheckedChange = { isSunSensitive = it },
                    modifier = Modifier.testTag("switch_sun_sensitive"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VoidBlack,
                        checkedTrackColor = AeroCyan,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Color(0xFF1E293B)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = onCancel,
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceElevated
                ) {
                    Text(
                        text = stringResource(R.string.cd_close),
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = {
                        val updated = spot.copy(
                            maxPm25 = pm25,
                            maxPm10 = pm10,
                            maxGrassPollen = grass,
                            maxRagweedPollen = ragweed,
                            maxUvIndex = uvIndex,
                            isSunSensitive = isSunSensitive
                        )
                        onSave(updated)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = AeroCyan
                ) {
                    Text(
                        text = stringResource(R.string.save_spot_button),
                        color = VoidBlack,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
