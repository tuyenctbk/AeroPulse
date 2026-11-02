package com.example.domain

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.data.model.AirQualityResponse
import com.example.data.model.OutdoorSpot
import com.example.data.model.WeatherResponse
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class MetricStatus(val color: Color) {
    PASS(Color(0xFF10B981)),   // Green: well within user-defined threshold
    WARN(Color(0xFFF59E0B)),   // Amber: approaching user-defined threshold (>= 80%)
    BREACH(Color(0xFFEF4444))  // Red: exceeds user-defined threshold
}

data class SpotMetricEvaluation(
    @StringRes val nameRes: Int,
    val currentValue: Double,
    val thresholdValue: Double,
    val unit: String,
    val status: MetricStatus,
    val isPollen: Boolean = false,
    val isSolar: Boolean = false
)

data class SpotEvaluationResult(
    val spot: OutdoorSpot,
    val score: Int,                         // 0 (hazardous) to 100 (pristine)
    val verdictLevel: VerdictLevel,
    @StringRes val headlineRes: Int,
    @StringRes val factorRes: Int,
    val factorArg: String? = null,
    val metricEvaluations: List<SpotMetricEvaluation>,
    val temperatureC: Double,
    val uvIndex: Double = 0.0,
    val peakUvIndex: Double = 0.0,
    val isSunSensitiveBreach: Boolean = false,
    val weatherDescription: String = "",
    val evaluationTimestamp: Long = System.currentTimeMillis()
)

object SpotThresholdEvaluator {

    fun evaluate(
        spot: OutdoorSpot,
        airQuality: AirQualityResponse,
        weather: WeatherResponse
    ): SpotEvaluationResult {
        val hourlyAq = airQuality.hourly
        val currentW = weather.current

        val currentTemp = currentW?.temperature2m ?: 20.0

        // Extract latest values (or index 0 of hourly forecast)
        val pm25 = hourlyAq?.pm25?.firstOrNull { it != null } ?: 12.0
        val pm10 = hourlyAq?.pm10?.firstOrNull { it != null } ?: 20.0
        val aqi = (hourlyAq?.usAqi?.firstOrNull { it != null } ?: 45.0).roundToInt()
        val ozone = hourlyAq?.ozone?.firstOrNull { it != null } ?: 40.0
        val no2 = hourlyAq?.nitrogenDioxide?.firstOrNull { it != null } ?: 15.0

        val grassPollen = hourlyAq?.grassPollen?.firstOrNull { it != null } ?: 4.0
        val ragweedPollen = hourlyAq?.ragweedPollen?.firstOrNull { it != null } ?: 2.0
        val birchPollen = hourlyAq?.birchPollen?.firstOrNull { it != null } ?: 3.0
        val alderPollen = hourlyAq?.alderPollen?.firstOrNull { it != null } ?: 1.0

        // Extract Solar UV data
        val uvCurrent = hourlyAq?.uvIndex?.firstOrNull { it != null } ?: 2.5
        val peakUv = weather.daily?.uvIndexMax?.firstOrNull { it != null } ?: uvCurrent

        // Evaluate each metric against the spot's user-defined thresholds
        fun evaluateMetric(
            @StringRes nameRes: Int,
            actual: Double,
            threshold: Double,
            unit: String,
            isPollen: Boolean = false,
            isSolar: Boolean = false
        ): SpotMetricEvaluation {
            val ratio = if (threshold > 0) actual / threshold else 1.0
            val status = when {
                ratio > 1.0 -> MetricStatus.BREACH
                ratio >= 0.8 -> MetricStatus.WARN
                else -> MetricStatus.PASS
            }
            return SpotMetricEvaluation(
                nameRes = nameRes,
                currentValue = actual,
                thresholdValue = threshold,
                unit = unit,
                status = status,
                isPollen = isPollen,
                isSolar = isSolar
            )
        }

        val uvEval = evaluateMetric(
            nameRes = R.string.threshold_uv_index,
            actual = uvCurrent,
            threshold = spot.maxUvIndex,
            unit = "UV",
            isSolar = true
        )

        val evaluations = listOf(
            evaluateMetric(R.string.pm25_particles, pm25, spot.maxPm25, "µg/m³"),
            evaluateMetric(R.string.pm10_inhalable, pm10, spot.maxPm10, "µg/m³"),
            evaluateMetric(R.string.threshold_aqi, aqi.toDouble(), spot.maxAqi.toDouble(), "AQI"),
            evaluateMetric(R.string.ozone_o3, ozone, spot.maxOzone, "µg/m³"),
            evaluateMetric(R.string.nitrogen_dioxide, no2, spot.maxNitrogenDioxide, "µg/m³"),
            evaluateMetric(R.string.pollen_grass, grassPollen, spot.maxGrassPollen, "gr/m³", isPollen = true),
            evaluateMetric(R.string.pollen_ragweed, ragweedPollen, spot.maxRagweedPollen, "gr/m³", isPollen = true),
            evaluateMetric(R.string.pollen_birch, birchPollen, spot.maxBirchPollen, "gr/m³", isPollen = true),
            evaluateMetric(R.string.pollen_alder, alderPollen, spot.maxAlderPollen, "gr/m³", isPollen = true),
            uvEval
        )

        val breachCount = evaluations.count { it.status == MetricStatus.BREACH }
        val warnCount = evaluations.count { it.status == MetricStatus.WARN }

        // Sun-sensitive UV breach check
        val isSunSensitiveBreach = spot.isSunSensitive && uvEval.status == MetricStatus.BREACH

        // Base score starts at 100, drops with breaches and warnings
        var calculatedScore = 100
        evaluations.forEach { eval ->
            val ratio = eval.currentValue / eval.thresholdValue
            if (eval.isSolar && spot.isSunSensitive) {
                if (ratio > 1.0) {
                    val overage = ratio - 1.0
                    // Intensified penalty for UV breaches when user is sun-sensitive
                    calculatedScore -= (overage * 55).roundToInt().coerceIn(25, 60)
                } else if (ratio >= 0.8) {
                    calculatedScore -= 14
                }
            } else {
                if (ratio > 1.0) {
                    val overage = ratio - 1.0
                    calculatedScore -= (overage * 35).roundToInt().coerceIn(15, 45)
                } else if (ratio >= 0.8) {
                    calculatedScore -= 8
                }
            }
        }
        val finalScore = calculatedScore.coerceIn(5, 100)

        // Find primary limiting factor
        val worstBreach = evaluations.filter { it.status == MetricStatus.BREACH }
            .maxByOrNull { it.currentValue / it.thresholdValue }
        val worstWarn = evaluations.filter { it.status == MetricStatus.WARN }
            .maxByOrNull { it.currentValue / it.thresholdValue }

        val level = when {
            isSunSensitiveBreach && (uvCurrent >= spot.maxUvIndex * 1.2 || finalScore < 60) -> VerdictLevel.HAZARDOUS_NO_GO
            isSunSensitiveBreach -> VerdictLevel.MODERATE_CAUTION
            breachCount == 0 && warnCount == 0 && finalScore >= 80 -> VerdictLevel.OPTIMAL_GO
            breachCount == 0 && finalScore >= 55 -> VerdictLevel.MODERATE_CAUTION
            breachCount == 1 && finalScore >= 55 -> VerdictLevel.MODERATE_CAUTION
            else -> VerdictLevel.HAZARDOUS_NO_GO
        }

        val headlineRes = when {
            isSunSensitiveBreach && level == VerdictLevel.HAZARDOUS_NO_GO -> R.string.status_no_go_exceeded
            level == VerdictLevel.OPTIMAL_GO -> R.string.status_go_within_thresholds
            level == VerdictLevel.MODERATE_CAUTION -> R.string.status_caution_near_threshold
            else -> R.string.status_no_go_exceeded
        }

        val (factorRes, factorArg) = when {
            isSunSensitiveBreach -> Pair(
                R.string.status_uv_breach,
                String.format(java.util.Locale.US, "%.1f", uvCurrent)
            )
            worstBreach != null -> Pair(
                R.string.threshold_exceeded_format,
                "${worstBreach.currentValue.roundToInt()} ${worstBreach.unit} > ${worstBreach.thresholdValue.roundToInt()}"
            )
            worstWarn != null -> Pair(
                R.string.threshold_pass_format,
                "${worstWarn.currentValue.roundToInt()} ${worstWarn.unit} ~ ${worstWarn.thresholdValue.roundToInt()}"
            )
            else -> Pair(R.string.factor_clean_air, null)
        }

        return SpotEvaluationResult(
            spot = spot,
            score = finalScore,
            verdictLevel = level,
            headlineRes = headlineRes,
            factorRes = factorRes,
            factorArg = factorArg,
            metricEvaluations = evaluations,
            temperatureC = currentTemp,
            uvIndex = uvCurrent,
            peakUvIndex = peakUv,
            isSunSensitiveBreach = isSunSensitiveBreach
        )
    }
}
