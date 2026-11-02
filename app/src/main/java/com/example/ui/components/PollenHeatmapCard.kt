package com.example.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.example.data.model.OutdoorSpot
import com.example.domain.PollenTelemetry
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.CardBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.VoidBlack
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

enum class PollenSpeciesLayer(
    @StringRes val titleRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ALL(R.string.layer_all_pollen, Icons.Default.Forest),
    GRASS(R.string.species_grass, Icons.Default.Forest),
    RAGWEED(R.string.species_ragweed, Icons.Default.Forest),
    BIRCH(R.string.species_birch, Icons.Default.Forest),
    ALDER(R.string.species_alder, Icons.Default.Forest)
}

data class MapHotspot(
    val id: String,
    val name: String,
    val relX: Float, // -1.0 to +1.0 from center
    val relY: Float, // -1.0 to +1.0 from center
    val baseConcentration: Double,
    val speciesBias: PollenSpeciesLayer,
    val isUserSpot: Boolean = false
)

/**
 * PollenHeatmapCard:
 * A rich visual map-like heatmap overlay component displaying localized botanical pollen
 * concentration fields, Gaussian dispersion plumes, wind drift vectors, and allergy planning guidance.
 */
@Composable
fun PollenHeatmapCard(
    pollen: PollenTelemetry,
    locationName: String,
    windSpeedKmh: Double = 12.0,
    spots: List<OutdoorSpot> = emptyList(),
    modifier: Modifier = Modifier
) {
    var selectedLayer by remember { mutableStateOf(PollenSpeciesLayer.ALL) }
    var selectedDistanceKm by remember { mutableStateOf(10) } // 5, 10, 20 km range
    var tappedOffset by remember { mutableStateOf<Offset?>(null) }

    // Wind animation transition for drift vectors
    val infiniteTransition = rememberInfiniteTransition(label = "pollen_wind_drift")
    val windPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift_phase"
    )

    // Build synthetic botanical clusters around the current reference point
    val hotspots = remember(pollen, spots, selectedLayer) {
        val list = mutableListOf<MapHotspot>()
        val total = pollen.grassPollen + pollen.ragweedPollen + pollen.birchPollen + pollen.alderPollen

        // 1. North Parklands (Grass-rich)
        list.add(
            MapHotspot(
                id = "north_park",
                name = "North Parklands",
                relX = 0.15f,
                relY = -0.48f,
                baseConcentration = pollen.grassPollen * 1.35,
                speciesBias = PollenSpeciesLayer.GRASS
            )
        )
        // 2. East River Valley (Alder & Birch trees)
        list.add(
            MapHotspot(
                id = "east_river",
                name = "River Greenbelt",
                relX = 0.52f,
                relY = 0.10f,
                baseConcentration = (pollen.birchPollen + pollen.alderPollen) * 1.4,
                speciesBias = PollenSpeciesLayer.BIRCH
            )
        )
        // 3. South Agricultural Basin (Ragweed heavy)
        list.add(
            MapHotspot(
                id = "south_fields",
                name = "South Basin Trail",
                relX = -0.25f,
                relY = 0.52f,
                baseConcentration = pollen.ragweedPollen * 1.5,
                speciesBias = PollenSpeciesLayer.RAGWEED
            )
        )
        // 4. West Ridge Botanical Reserve
        list.add(
            MapHotspot(
                id = "west_ridge",
                name = "West Ridge Reserve",
                relX = -0.55f,
                relY = -0.20f,
                baseConcentration = (pollen.grassPollen + pollen.birchPollen) * 0.9,
                speciesBias = PollenSpeciesLayer.ALL
            )
        )

        // Include user spots projected locally
        spots.take(3).forEachIndexed { idx, spot ->
            val angle = (idx * 2.1) + 0.8
            val dist = 0.38f + (idx * 0.12f)
            list.add(
                MapHotspot(
                    id = "spot_${spot.id}",
                    name = spot.name,
                    relX = (cos(angle) * dist).toFloat(),
                    relY = (sin(angle) * dist).toFloat(),
                    baseConcentration = total * 0.85,
                    speciesBias = PollenSpeciesLayer.ALL,
                    isUserSpot = true
                )
            )
        }
        list
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pollen_heatmap_card"),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Title & Range Selector
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
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = stringResource(R.string.pollen_heatmap_title),
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.pollen_heatmap_title),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.1.sp,
                                color = TextWhite
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.pollen_heatmap_subtitle, locationName),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = TextMuted
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Range Pill (5km, 10km, 20km)
                Surface(
                    onClick = {
                        selectedDistanceKm = when (selectedDistanceKm) {
                            5 -> 10
                            10 -> 20
                            else -> 5
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.testTag("btn_pollen_map_range")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompassCalibration,
                            contentDescription = null,
                            tint = AeroCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.range_km_format, selectedDistanceKm),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Species Filter Chips Row
            LazyRow(
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(PollenSpeciesLayer.values()) { layer ->
                    val isSelected = layer == selectedLayer
                    val bg = if (isSelected) Color(0xFF10B981) else SurfaceElevated
                    val textColor = if (isSelected) VoidBlack else TextWhite
                    val border = if (isSelected) Color(0xFF10B981) else CardBorder

                    Surface(
                        onClick = { selectedLayer = layer },
                        shape = RoundedCornerShape(12.dp),
                        color = bg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, border),
                        modifier = Modifier.testTag("chip_species_${layer.name.lowercase()}")
                    ) {
                        Text(
                            text = stringResource(layer.titleRes),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.sp,
                                color = textColor
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Map Heatmap Canvas Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF090E17))
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            tappedOffset = offset
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    val maxRadius = minOf(cx, cy) * 0.88f

                    // 1. Draw Stylized Radar Distance Rings
                    val ringSteps = 3
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    for (i in 1..ringSteps) {
                        val r = maxRadius * (i.toFloat() / ringSteps)
                        drawCircle(
                            color = CardBorder.copy(alpha = 0.5f),
                            radius = r,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1f, pathEffect = dashEffect)
                        )
                    }

                    // Crosshair Axes
                    drawLine(
                        color = CardBorder.copy(alpha = 0.35f),
                        start = Offset(cx - maxRadius, cy),
                        end = Offset(cx + maxRadius, cy),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = CardBorder.copy(alpha = 0.35f),
                        start = Offset(cx, cy - maxRadius),
                        end = Offset(cx, cy + maxRadius),
                        strokeWidth = 1f
                    )

                    // 2. Render Gaussian Pollen Dispersion Heatmap Splats
                    hotspots.forEach { hotspot ->
                        // Calculate effective concentration according to active species filter
                        val concentration = when (selectedLayer) {
                            PollenSpeciesLayer.ALL -> hotspot.baseConcentration
                            PollenSpeciesLayer.GRASS -> if (hotspot.speciesBias == PollenSpeciesLayer.GRASS || hotspot.speciesBias == PollenSpeciesLayer.ALL) pollen.grassPollen * 1.3 else pollen.grassPollen * 0.3
                            PollenSpeciesLayer.RAGWEED -> if (hotspot.speciesBias == PollenSpeciesLayer.RAGWEED || hotspot.speciesBias == PollenSpeciesLayer.ALL) pollen.ragweedPollen * 1.4 else pollen.ragweedPollen * 0.25
                            PollenSpeciesLayer.BIRCH -> if (hotspot.speciesBias == PollenSpeciesLayer.BIRCH || hotspot.speciesBias == PollenSpeciesLayer.ALL) pollen.birchPollen * 1.3 else pollen.birchPollen * 0.3
                            PollenSpeciesLayer.ALDER -> if (hotspot.speciesBias == PollenSpeciesLayer.BIRCH || hotspot.speciesBias == PollenSpeciesLayer.ALL) pollen.alderPollen * 1.3 else pollen.alderPollen * 0.3
                        }

                        val hx = cx + hotspot.relX * maxRadius
                        val hy = cy + hotspot.relY * maxRadius

                        val (coreColor, outerRadius) = when {
                            concentration < 10.0 -> Pair(Color(0xFF10B981), maxRadius * 0.35f)
                            concentration < 25.0 -> Pair(Color(0xFFF59E0B), maxRadius * 0.45f)
                            concentration < 55.0 -> Pair(Color(0xFFF97316), maxRadius * 0.55f)
                            else -> Pair(Color(0xFFEF4444), maxRadius * 0.65f)
                        }

                        // Multi-layer radial Gaussian gradient splat
                        val radialBrush = Brush.radialGradient(
                            colors = listOf(
                                coreColor.copy(alpha = 0.55f),
                                coreColor.copy(alpha = 0.30f),
                                coreColor.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(hx, hy),
                            radius = outerRadius
                        )
                        drawCircle(
                            brush = radialBrush,
                            radius = outerRadius,
                            center = Offset(hx, hy)
                        )

                        // Cluster Core Indicator Marker
                        drawCircle(
                            color = coreColor,
                            radius = if (hotspot.isUserSpot) 6.dp.toPx() else 4.dp.toPx(),
                            center = Offset(hx, hy)
                        )
                        if (hotspot.isUserSpot) {
                            drawCircle(
                                color = VoidBlack,
                                radius = 3.dp.toPx(),
                                center = Offset(hx, hy)
                            )
                        }
                    }

                    // 3. Draw Atmospheric Wind Drift Streamlines
                    // Wind direction vector pointing northeast (representing 45° dispersal)
                    val windAngleRad = 0.85 // ~48 degrees
                    val windCos = cos(windAngleRad).toFloat()
                    val windSin = -sin(windAngleRad).toFloat()
                    val driftOffset = windPhase * 40f

                    for (driftY in -2..2) {
                        val startY = cy + driftY * (maxRadius * 0.35f)
                        val startX = cx - maxRadius * 0.6f + (driftOffset % 40f)

                        val pStart = Offset(startX, startY)
                        val pEnd = Offset(startX + 35f * windCos, startY + 35f * windSin)

                        drawLine(
                            color = Color(0xFF10B981).copy(alpha = 0.22f),
                            start = pStart,
                            end = pEnd,
                            strokeWidth = 1.5f,
                            cap = StrokeCap.Round
                        )
                    }

                    // 4. Center Reference GPS Point
                    drawCircle(
                        color = AeroCyan.copy(alpha = 0.25f),
                        radius = 14.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = AeroCyan,
                        radius = 5.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = VoidBlack,
                        radius = 2.dp.toPx(),
                        center = Offset(cx, cy)
                    )

                    // 5. Tapped Inspection Reticle
                    tappedOffset?.let { tap ->
                        drawCircle(
                            color = TextWhite.copy(alpha = 0.4f),
                            radius = 16.dp.toPx(),
                            center = tap,
                            style = Stroke(width = 1.5f, pathEffect = dashEffect)
                        )
                        drawCircle(
                            color = AeroCyan,
                            radius = 3.dp.toPx(),
                            center = tap
                        )
                    }
                }

                // Compass Cardinal Badges
                Text(
                    text = "N",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
                Text(
                    text = "S",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
                Text(
                    text = "W",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                )
                Text(
                    text = "E",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                )

                // Heatmap Color Scale Legend Overlay (Bottom Left)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0C1322).copy(alpha = 0.88f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendPip(Color(0xFF10B981), stringResource(R.string.risk_low))
                        Spacer(modifier = Modifier.width(6.dp))
                        LegendPip(Color(0xFFF59E0B), stringResource(R.string.risk_moderate))
                        Spacer(modifier = Modifier.width(6.dp))
                        LegendPip(Color(0xFFEF4444), stringResource(R.string.risk_high))
                    }
                }

                // Current Wind Vector Pill (Top Right)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0C1322).copy(alpha = 0.88f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.wind_plume_format, windSpeedKmh.roundToInt()),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Allergy Activity Planning Advice Banner
            val nearestCluster = remember(tappedOffset, hotspots) {
                // If user tapped, find nearest cluster, otherwise pick highest concentration
                tappedOffset?.let { tap ->
                    hotspots.minByOrNull { h ->
                        // approximate coordinate mapping
                        val normX = (tap.x - 200f) / 150f
                        val normY = (tap.y - 115f) / 115f
                        hypot((h.relX - normX).toDouble(), (h.relY - normY).toDouble())
                    }
                } ?: hotspots.maxByOrNull { it.baseConcentration }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.allergy_planning_title),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = Color(0xFF10B981)
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (nearestCluster != null) {
                                stringResource(
                                    R.string.allergy_hotspot_advice_format,
                                    nearestCluster.name,
                                    nearestCluster.baseConcentration.roundToInt()
                                )
                            } else {
                                stringResource(R.string.allergy_planning_default_advice)
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextWhite,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tap hint
            Text(
                text = stringResource(R.string.heatmap_tap_hint),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    color = TextMuted.copy(alpha = 0.7f)
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun LegendPip(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                color = TextMuted
            )
        )
    }
}
