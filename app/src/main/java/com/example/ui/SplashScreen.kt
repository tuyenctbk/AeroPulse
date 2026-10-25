package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.PulseEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.VoidBlack
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0.12f) }
    var statusTextRes by remember { mutableIntStateOf(R.string.splash_calibrating) }

    val infiniteTransition = rememberInfiniteTransition(label = "radarAtmosphere")

    // Radar 360 degree continuous sweep
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarSweep"
    )

    // Pulsing outer ripple
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "splashProgress"
    )

    LaunchedEffect(Unit) {
        delay(250)
        progress = 0.45f
        delay(550)
        progress = 0.82f
        delay(500)
        progress = 1.0f
        statusTextRes = R.string.splash_ready
        delay(400)
        onSplashComplete()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Allow instant skip on tap for fast-return UX
                onSplashComplete()
            },
        contentAlignment = Alignment.Center
    ) {
        val maxDim = maxWidth.coerceAtMost(maxHeight)
        val radarSize = (maxDim * 0.72f).coerceIn(240.dp, 420.dp)

        // Atmospheric Radar Sweep Background
        Canvas(modifier = Modifier.size(radarSize)) {
            val center = this.center
            val radius = size.minDimension / 2f

            // Concentric range rings
            val ringColors = listOf(
                Color(0xFF0F1C2E) to 0.35f,
                Color(0xFF162B44) to 0.65f,
                Color(0xFF1B3654) to 0.95f
            )
            ringColors.forEach { (col, fraction) ->
                drawCircle(
                    color = col,
                    radius = radius * fraction,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // Expanding ripple wave
            drawCircle(
                color = AeroCyan.copy(alpha = pulseAlpha),
                radius = radius * 0.95f * (pulseScale / 1.4f),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Radar Crosshairs
            drawLine(
                color = Color(0xFF14273E),
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color(0xFF14273E),
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1.dp.toPx()
            )

            // Rotating Radar Sweep Beam Line
            val rad = Math.toRadians(sweepAngle.toDouble())
            val sweepX = (center.x + radius * 0.95f * cos(rad)).toFloat()
            val sweepY = (center.y + radius * 0.95f * sin(rad)).toFloat()
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, AeroCyan.copy(alpha = 0.7f), AeroCyan),
                    start = center,
                    end = Offset(sweepX, sweepY)
                ),
                start = center,
                end = Offset(sweepX, sweepY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Ambient background radar glow arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        AeroCyan.copy(alpha = 0.03f),
                        AeroCyan.copy(alpha = 0.18f),
                        Color.Transparent
                    )
                ),
                startAngle = sweepAngle - 60f,
                sweepAngle = 60f,
                useCenter = true
            )
        }

        // Center Content Overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Emblem with glowing breath aura
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF132742), Color(0xFF070E1A))
                        )
                    )
            ) {
                Canvas(modifier = Modifier.size(68.dp)) {
                    // Concentric pulse arcs
                    drawArc(
                        brush = Brush.linearGradient(listOf(AeroCyan, PulseEmerald)),
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Waveform apex dot
                    drawCircle(
                        color = TextWhite,
                        radius = 4.5.dp.toPx(),
                        center = center
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Title
            Text(
                text = stringResource(R.string.app_name).uppercase(),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    color = TextWhite
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    color = AeroCyan
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = AeroCyan,
                trackColor = Color(0xFF111E30),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(statusTextRes),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    color = TextMuted
                )
            )
        }
    }
}

