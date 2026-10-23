package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.PollenTelemetry
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.CardBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlin.math.roundToInt

@Composable
fun PollenRadarCard(
    pollen: PollenTelemetry,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pollen_radar_card"),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
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
                        imageVector = Icons.Default.Park,
                        contentDescription = stringResource(R.string.botanical_pollen_radar_title),
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.botanical_pollen_radar_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextWhite
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Risk Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = pollen.riskColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, pollen.riskColor)
                ) {
                    Text(
                        text = stringResource(pollen.riskLevelRes).uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = pollen.riskColor
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.dominant_allergen_format, stringResource(pollen.dominantSpeciesRes)),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pollen Species Bars
            PollenSpeciesRow(
                speciesName = stringResource(R.string.pollen_grass),
                count = pollen.grassPollen,
                maxRef = 40.0,
                barColor = Color(0xFF10B981)
            )

            Spacer(modifier = Modifier.height(10.dp))

            PollenSpeciesRow(
                speciesName = stringResource(R.string.pollen_ragweed),
                count = pollen.ragweedPollen,
                maxRef = 30.0,
                barColor = Color(0xFFF59E0B)
            )

            Spacer(modifier = Modifier.height(10.dp))

            PollenSpeciesRow(
                speciesName = stringResource(R.string.pollen_birch),
                count = pollen.birchPollen,
                maxRef = 50.0,
                barColor = Color(0xFF00E5FF)
            )

            Spacer(modifier = Modifier.height(10.dp))

            PollenSpeciesRow(
                speciesName = stringResource(R.string.pollen_alder),
                count = pollen.alderPollen,
                maxRef = 40.0,
                barColor = Color(0xFFA78BFA)
            )
        }
    }
}

@Composable
private fun PollenSpeciesRow(
    speciesName: String,
    count: Double,
    maxRef: Double,
    barColor: Color
) {
    val progress = (count / maxRef).coerceIn(0.05, 1.0).toFloat()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = speciesName,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextWhite,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.grains_per_m3, count.roundToInt()),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = Color(0xFF131D2E),
            strokeCap = StrokeCap.Round
        )
    }
}
