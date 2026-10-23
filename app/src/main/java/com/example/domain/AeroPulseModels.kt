package com.example.domain

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.R

enum class UserProfile(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val title: String,
    val subtitle: String,
    val pm25Sensitivity: Double,    // lower means more sensitive
    val ozoneSensitivity: Double,
    val uvSensitivity: Double,
    val pollenSensitivity: Double,
    val tempMaxComfort: Double
) {
    RUNNER(
        titleRes = R.string.profile_runner_title,
        subtitleRes = R.string.profile_runner_sub,
        title = "Runner / Athlete",
        subtitle = "Aerobic ventilation: High PM2.5 & Ozone sensitivity",
        pm25Sensitivity = 20.0,
        ozoneSensitivity = 90.0,
        uvSensitivity = 7.0,
        pollenSensitivity = 20.0,
        tempMaxComfort = 28.0
    ),
    ALLERGY_SUFFERER(
        titleRes = R.string.profile_allergy_title,
        subtitleRes = R.string.profile_allergy_sub,
        title = "Allergy & Asthma",
        subtitle = "Respiratory focus: Extreme grass & ragweed sensitivity",
        pm25Sensitivity = 15.0,
        ozoneSensitivity = 70.0,
        uvSensitivity = 8.0,
        pollenSensitivity = 8.0,
        tempMaxComfort = 30.0
    ),
    PARENT_STROLLER(
        titleRes = R.string.profile_parent_title,
        subtitleRes = R.string.profile_parent_sub,
        title = "Parent & Stroller",
        subtitle = "Child safety: Strict UV solar limits & ground PM2.5",
        pm25Sensitivity = 18.0,
        ozoneSensitivity = 80.0,
        uvSensitivity = 5.0,
        pollenSensitivity = 15.0,
        tempMaxComfort = 27.0
    ),
    PET_WALKER(
        titleRes = R.string.profile_pet_title,
        subtitleRes = R.string.profile_pet_sub,
        title = "Pet Walker",
        subtitle = "Pavement safety: Midday heat, UV & low-ground dust",
        pm25Sensitivity = 25.0,
        ozoneSensitivity = 100.0,
        uvSensitivity = 6.5,
        pollenSensitivity = 25.0,
        tempMaxComfort = 26.5
    )
}

enum class VerdictLevel(
    @StringRes val labelRes: Int,
    @StringRes val shortLabelRes: Int,
    val label: String,
    val shortLabel: String,
    val color: Color,
    val secondaryColor: Color
) {
    OPTIMAL_GO(
        labelRes = R.string.verdict_go_label,
        shortLabelRes = R.string.verdict_go_short,
        label = "OPTIMAL WINDOW • GO",
        shortLabel = "GO",
        color = Color(0xFF10B981),
        secondaryColor = Color(0xFF00E5FF)
    ),
    MODERATE_CAUTION(
        labelRes = R.string.verdict_caution_label,
        shortLabelRes = R.string.verdict_caution_short,
        label = "MODERATE • PROCEED WITH CAUTION",
        shortLabel = "CAUTION",
        color = Color(0xFFF59E0B),
        secondaryColor = Color(0xFFFBBF24)
    ),
    HAZARDOUS_NO_GO(
        labelRes = R.string.verdict_no_go_label,
        shortLabelRes = R.string.verdict_no_go_short,
        label = "HAZARDOUS • NO-GO OUTDOORS",
        shortLabel = "NO-GO",
        color = Color(0xFFEF4444),
        secondaryColor = Color(0xFFF87171)
    )
}

data class OutdoorVerdict(
    val level: VerdictLevel,
    val score: Int,                    // 0 (hazardous) to 100 (pristine)
    @StringRes val headlineRes: Int = R.string.headline_optimal_flow,
    val headline: String = "",
    @StringRes val factorRes: Int = R.string.factor_clean_air,
    val factorArg: Any? = null,
    val primaryLimitingFactor: String = "",
    @StringRes val bestWindowRes: Int = R.string.window_continuous_favorable,
    val bestWindowArg: String? = null,
    val bestWindowSpan: String = "",
    @StringRes val actionableTipRes: Int = R.string.tip_runner_optimal,
    val actionableTipArgs: List<Any> = emptyList(),
    val actionableTip: String = ""
)

data class HourlyRadarPoint(
    val isoTime: String,
    val displayHour: String,
    val pm25: Double,
    val pm10: Double,
    val uvIndex: Double,
    val temperatureC: Double,
    val score: Int,
    val level: VerdictLevel,
    @StringRes val limitingFactorRes: Int = R.string.factor_clean_air,
    val limitingFactorArg: Any? = null,
    val limitingFactor: String = ""
)

data class PollenTelemetry(
    val grassPollen: Double,
    val ragweedPollen: Double,
    val birchPollen: Double,
    val alderPollen: Double,
    @StringRes val dominantSpeciesRes: Int = R.string.species_grass,
    val dominantSpecies: String = "",
    @StringRes val riskLevelRes: Int = R.string.risk_low,
    val riskLevel: String = "",
    val riskColor: Color
)

data class SolarTelemetry(
    val currentUv: Double,
    val peakUv: Double,
    val peakUvTime: String,
    val sunrise: String,
    val sunset: String,
    val goldenHourMorning: String,
    val goldenHourEvening: String
)

data class SpaceWeatherTelemetry(
    val kpIndex: Double,
    @StringRes val scaleRes: Int = R.string.sw_scale_g0_quiet,
    val stormScale: String = "",
    @StringRes val statusRes: Int = R.string.sw_status_quiet,
    val statusText: String = "",
    @StringRes val auroraRes: Int = R.string.sw_aurora_quiet,
    val auroraProspects: String = "",
    @StringRes val techRes: Int = R.string.sw_tech_quiet,
    val techGpsStatus: String = ""
)

data class AirPurityMetrics(
    val pm25: Double,
    val pm10: Double,
    val usAqi: Int,
    val europeanAqi: Int,
    val ozone: Double,
    val nitrogenDioxide: Double,
    val carbonMonoxide: Double,
    val dust: Double
)

data class AeroPulseTelemetry(
    val locationName: String,
    val regionCountry: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double,
    val humidityPct: Double,
    val apparentTempC: Double,
    val windSpeedKmh: Double,
    val verdict: OutdoorVerdict,
    val purity: AirPurityMetrics,
    val pollen: PollenTelemetry,
    val solar: SolarTelemetry,
    val spaceWeather: SpaceWeatherTelemetry,
    val hourlyRadar: List<HourlyRadarPoint>,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    val isCachedFallback: Boolean = false
)
