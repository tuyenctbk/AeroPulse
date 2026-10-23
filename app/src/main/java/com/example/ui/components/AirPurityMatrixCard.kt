package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.domain.AirPurityMetrics
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.CardBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlin.math.roundToInt

@Composable
fun AirPurityMatrixCard(
    purity: AirPurityMetrics,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("air_purity_matrix_card"),
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
                        imageVector = Icons.Default.Air,
                        contentDescription = stringResource(R.string.air_purity_matrix_title),
                        tint = AeroCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.air_purity_matrix_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextWhite
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // US AQI Pill
                val aqiColor = when {
                    purity.usAqi <= 50 -> Color(0xFF10B981)
                    purity.usAqi <= 100 -> Color(0xFFF59E0B)
                    purity.usAqi <= 150 -> Color(0xFFF97316)
                    else -> Color(0xFFEF4444)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = aqiColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, aqiColor)
                ) {
                    Text(
                        text = stringResource(R.string.us_aqi_format, purity.usAqi),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = aqiColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val cleanText = stringResource(R.string.clean_status)
            val moderateText = stringResource(R.string.moderate_status)
            val elevatedText = stringResource(R.string.elevated_status)
            val nominalText = stringResource(R.string.nominal_status)
            val optimalText = stringResource(R.string.optimal_status)
            val lungIrritantText = stringResource(R.string.lung_irritant_status)
            val trafficLoadText = stringResource(R.string.traffic_load_status)
            val ugUnit = stringResource(R.string.ug_m3_unit)

            // 2x2 Grid for Key Contaminants
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    label = stringResource(R.string.pm25_particles),
                    value = "${"%.1f".format(purity.pm25)}",
                    unit = ugUnit,
                    status = if (purity.pm25 <= 15) cleanText else if (purity.pm25 <= 35) moderateText else elevatedText,
                    statusColor = if (purity.pm25 <= 15) Color(0xFF10B981) else if (purity.pm25 <= 35) Color(0xFFF59E0B) else Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = stringResource(R.string.pm10_inhalable),
                    value = "${purity.pm10.roundToInt()}",
                    unit = ugUnit,
                    status = if (purity.pm10 <= 45) nominalText else elevatedText,
                    statusColor = if (purity.pm10 <= 45) Color(0xFF10B981) else Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    label = stringResource(R.string.ozone_o3),
                    value = "${purity.ozone.roundToInt()}",
                    unit = ugUnit,
                    status = if (purity.ozone <= 80) optimalText else lungIrritantText,
                    statusColor = if (purity.ozone <= 80) Color(0xFF10B981) else Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = stringResource(R.string.nitrogen_dioxide),
                    value = "${purity.nitrogenDioxide.roundToInt()}",
                    unit = ugUnit,
                    status = if (purity.nitrogenDioxide <= 40) cleanText else trafficLoadText,
                    statusColor = if (purity.nitrogenDioxide <= 40) Color(0xFF10B981) else Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary row: Dust & European AQI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090E18))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dust_format, purity.dust),
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.european_aqi_format, purity.europeanAqi),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AeroCyan,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    unit: String,
    status: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0C1322))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = TextWhite
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = TextMuted
                    ),
                    modifier = Modifier.padding(bottom = 2.dp),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
