package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.AeroPulseTelemetry
import com.example.domain.UserProfile
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.CardBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.WatchBezel
import com.example.ui.theme.WatchScreenBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class WearOsSimView {
    WATCH_FACE,
    GLANCE_TILE
}

@Composable
fun WearOsWatchSimulator(
    telemetry: AeroPulseTelemetry,
    profile: UserProfile,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var simMode by remember { mutableStateOf(WearOsSimView.WATCH_FACE) }
    val currentTime = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    // Handle back button on device to pop back to dashboard
    BackHandler {
        onNavigateBack()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("wear_os_simulator_card"),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Simulator Mode Header Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = stringResource(R.string.wear_os_companion_title),
                        tint = AeroCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.wear_os_companion_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextWhite
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Sub-mode pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0C1322))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (simMode == WearOsSimView.WATCH_FACE) AeroCyan else Color.Transparent)
                            .clickable { simMode = WearOsSimView.WATCH_FACE }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.watch_face_tab),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (simMode == WearOsSimView.WATCH_FACE) Color(0xFF070B12) else TextMuted
                            ),
                            maxLines = 1
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (simMode == WearOsSimView.GLANCE_TILE) AeroCyan else Color.Transparent)
                            .clickable { simMode = WearOsSimView.GLANCE_TILE }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.wrist_tile_tab),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (simMode == WearOsSimView.GLANCE_TILE) Color(0xFF070B12) else TextMuted
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Adaptive Physical Watch Chassis with Crown dial
            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                val chassisSize = maxWidth.coerceAtMost(300.dp)
                val bezelSize = (chassisSize - 20.dp).coerceAtLeast(220.dp)
                val screenDiameter = (bezelSize - 24.dp).coerceAtLeast(196.dp)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(chassisSize)
                ) {
                    // Crown dial on right
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(width = 10.dp, height = 32.dp)
                            .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                            .background(Color(0xFF334155))
                    )

                    // Circular Watch Casing
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(bezelSize)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF222F3E), WatchBezel, Color(0xFF0F1722))
                                )
                            )
                            .border(4.dp, Color(0xFF3B4D66), CircleShape)
                            .shadow(12.dp, CircleShape)
                    ) {
                        // Inner Circular AMOLED Screen (Wear OS standard)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(screenDiameter)
                                .clip(CircleShape)
                                .background(WatchScreenBg)
                                .border(1.dp, Color(0xFF162335), CircleShape)
                        ) {
                            if (simMode == WearOsSimView.WATCH_FACE) {
                                WatchFaceContent(telemetry = telemetry, time = currentTime)
                            } else {
                                WatchTileContent(telemetry = telemetry, profile = profile, onRefresh = onRefresh)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (simMode == WearOsSimView.WATCH_FACE)
                    stringResource(R.string.wear_os_watch_face_desc)
                else
                    stringResource(R.string.wear_os_tile_desc),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun WatchFaceContent(
    telemetry: AeroPulseTelemetry,
    time: String
) {
    val verdict = telemetry.verdict
    val score = verdict.score
    val level = verdict.level

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Outer Go/No-Go Halo Ring - dynamically conforms to inner screen size
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val strokeWidth = 5.dp.toPx()
            val sweep = 360f * (score / 100f)

            // Track
            drawArc(
                color = Color(0xFF131D2E),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )

            // Progress Ring
            drawArc(
                brush = Brush.sweepGradient(listOf(level.secondaryColor, level.color)),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Watch Face Center Components
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Top Complication: PM2.5
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1A2A))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = null,
                    tint = AeroCyan,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${telemetry.purity.pm25.roundToInt()} µg",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Digital Time
            Text(
                text = time,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = TextWhite
                )
            )

            // Verdict Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = level.color.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, level.color)
            ) {
                Text(
                    text = "${stringResource(level.shortLabelRes)} • $score",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = level.color
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Complications Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // UV
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F1A2A))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = stringResource(R.string.uv_prefix, telemetry.solar.currentUv),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            color = TextWhite
                        )
                    )
                }

                // Pollen
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F1A2A))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Park,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = stringResource(telemetry.pollen.riskLevelRes),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            color = TextWhite
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchTileContent(
    telemetry: AeroPulseTelemetry,
    profile: UserProfile,
    onRefresh: () -> Unit
) {
    val verdict = telemetry.verdict

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Text(
            text = stringResource(R.string.aeropulse_tile_label),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                color = AeroCyan,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(verdict.level.shortLabelRes),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = verdict.level.color
            )
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Safe Window Span
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF101B2B)
        ) {
            Text(
                text = if (verdict.bestWindowArg != null) {
                    stringResource(verdict.bestWindowRes, verdict.bestWindowArg)
                } else {
                    stringResource(verdict.bestWindowRes)
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    color = TextWhite,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (verdict.factorArg != null) {
                stringResource(verdict.factorRes, verdict.factorArg)
            } else {
                stringResource(verdict.factorRes)
            },
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 9.5.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            ),
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Wrist Refresh Button
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(AeroCyan)
                .clickable { onRefresh() }
                .padding(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.wrist_sync_button),
                tint = Color(0xFF070B12),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
