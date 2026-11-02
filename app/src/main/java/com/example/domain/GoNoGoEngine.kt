package com.example.domain

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.data.model.AirQualityHourly
import com.example.data.model.AirQualityResponse
import com.example.data.model.DailyWeather
import com.example.data.model.HourlyWeather
import com.example.data.model.NoaaKpItem
import com.example.data.model.WeatherResponse
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ScoreFactor(
    @StringRes val resId: Int,
    val arg: Any?,
    val fallbackString: String,
    val weight: Double
)

data class WindowSpan(
    @StringRes val resId: Int,
    val arg: String?,
    val displaySpan: String
)

data class ActionAdvice(
    @StringRes val resId: Int,
    val args: List<Any>,
    val fallbackString: String
)

object GoNoGoEngine {

    fun computeTelemetry(
        locationName: String,
        regionCountry: String,
        airQuality: AirQualityResponse,
        weather: WeatherResponse,
        noaaData: List<NoaaKpItem>?,
        profile: UserProfile,
        isCached: Boolean = false
    ): AeroPulseTelemetry {
        val hourlyAq = airQuality.hourly
        val currentW = weather.current
        val hourlyW = weather.hourly
        val dailyW = weather.daily

        val currentTemp = currentW?.temperature2m ?: 20.0
        val currentHumidity = currentW?.relativeHumidity2m ?: 50.0
        val currentApparent = currentW?.apparentTemperature ?: currentTemp
        val currentWind = currentW?.windSpeed10m ?: 12.0

        // Base hour index for today's current hour
        val currentHourPrefix = currentW?.time?.take(13)
        val matchedCurrentIndex = if (!currentHourPrefix.isNullOrEmpty()) {
            hourlyAq?.time?.indexOfFirst { it.startsWith(currentHourPrefix) }?.takeIf { it >= 0 }
        } else null
        val baseIndex = matchedCurrentIndex ?: (if ((hourlyAq?.time?.size ?: 0) >= 7 * 24) 3 * 24 else 0)

        // Extract current indices (or index matching current hour)
        val pm25Current = hourlyAq?.pm25?.getOrNull(baseIndex) ?: hourlyAq?.pm25?.firstOrNull() ?: 12.0
        val pm10Current = hourlyAq?.pm10?.getOrNull(baseIndex) ?: hourlyAq?.pm10?.firstOrNull() ?: 22.0
        val usAqiCurrent = hourlyAq?.usAqi?.getOrNull(baseIndex)?.roundToInt() ?: hourlyAq?.usAqi?.firstOrNull()?.roundToInt() ?: 45
        val euAqiCurrent = hourlyAq?.europeanAqi?.getOrNull(baseIndex)?.roundToInt() ?: hourlyAq?.europeanAqi?.firstOrNull()?.roundToInt() ?: 25
        val ozoneCurrent = hourlyAq?.ozone?.getOrNull(baseIndex) ?: hourlyAq?.ozone?.firstOrNull() ?: 45.0
        val no2Current = hourlyAq?.nitrogenDioxide?.getOrNull(baseIndex) ?: hourlyAq?.nitrogenDioxide?.firstOrNull() ?: 18.0
        val coCurrent = hourlyAq?.carbonMonoxide?.getOrNull(baseIndex) ?: hourlyAq?.carbonMonoxide?.firstOrNull() ?: 210.0
        val dustCurrent = hourlyAq?.dust?.getOrNull(baseIndex) ?: hourlyAq?.dust?.firstOrNull() ?: 5.0

        // Current Pollen values
        val grassPollen = hourlyAq?.grassPollen?.getOrNull(baseIndex) ?: hourlyAq?.grassPollen?.firstOrNull() ?: 4.0
        val ragweedPollen = hourlyAq?.ragweedPollen?.getOrNull(baseIndex) ?: hourlyAq?.ragweedPollen?.firstOrNull() ?: 2.0
        val birchPollen = hourlyAq?.birchPollen?.getOrNull(baseIndex) ?: hourlyAq?.birchPollen?.firstOrNull() ?: 3.0
        val alderPollen = hourlyAq?.alderPollen?.getOrNull(baseIndex) ?: hourlyAq?.alderPollen?.firstOrNull() ?: 1.0

        // Current UV
        val uvCurrent = hourlyAq?.uvIndex?.getOrNull(baseIndex) ?: hourlyAq?.uvIndex?.firstOrNull() ?: 2.5
        val todayDailyIndex = if ((dailyW?.time?.size ?: 0) >= 7) 3 else 0
        val peakUv = dailyW?.uvIndexMax?.getOrNull(todayDailyIndex) ?: dailyW?.uvIndexMax?.firstOrNull() ?: 6.2
        val sunriseIso = dailyW?.sunrise?.getOrNull(todayDailyIndex) ?: dailyW?.sunrise?.firstOrNull() ?: "06:15"
        val sunsetIso = dailyW?.sunset?.getOrNull(todayDailyIndex) ?: dailyW?.sunset?.firstOrNull() ?: "18:45"

        // NOAA Space Weather Kp
        val latestKp = noaaData?.lastOrNull()?.let {
            it.kpIndex ?: it.estimatedKp ?: it.kp
        } ?: 2.33

        // Space weather calculation
        val spaceWeather = calculateSpaceWeather(latestKp)

        // Pollen Telemetry calculation
        val pollen = calculatePollen(grassPollen, ragweedPollen, birchPollen, alderPollen)

        // Solar & Golden Hour calculation
        val solar = calculateSolar(uvCurrent, peakUv, sunriseIso, sunsetIso)

        // Air Purity Metrics
        val purity = AirPurityMetrics(
            pm25 = pm25Current,
            pm10 = pm10Current,
            usAqi = usAqiCurrent,
            europeanAqi = euAqiCurrent,
            ozone = ozoneCurrent,
            nitrogenDioxide = no2Current,
            carbonMonoxide = coCurrent,
            dust = dustCurrent
        )

        // Hourly timeline (24 hours from current base)
        val availableFromBase = max(0, (hourlyAq?.time?.size ?: 0) - baseIndex)
        val hourlyCount = if (availableFromBase >= 24) 24 else min(24, hourlyAq?.time?.size ?: 24)
        val effectiveStart = if (availableFromBase >= 24) baseIndex else 0
        val hourlyRadar = mutableListOf<HourlyRadarPoint>()

        for (offset in 0 until hourlyCount) {
            val i = effectiveStart + offset
            val isoTime = hourlyAq?.time?.getOrNull(i) ?: "2026-10-14T$offset:00"
            val hourFormatted = formatDisplayHour(isoTime, offset)
            val hPm25 = hourlyAq?.pm25?.getOrNull(i) ?: pm25Current
            val hPm10 = hourlyAq?.pm10?.getOrNull(i) ?: pm10Current
            val hUv = hourlyAq?.uvIndex?.getOrNull(i) ?: 0.0
            val hTemp = hourlyW?.temperature2m?.getOrNull(i) ?: currentTemp
            val hOzone = hourlyAq?.ozone?.getOrNull(i) ?: ozoneCurrent
            val hGrass = hourlyAq?.grassPollen?.getOrNull(i) ?: grassPollen
            val hRagweed = hourlyAq?.ragweedPollen?.getOrNull(i) ?: ragweedPollen

            val (hScore, hFactor) = computeHourlyScore(
                profile = profile,
                pm25 = hPm25,
                pm10 = hPm10,
                ozone = hOzone,
                uv = hUv,
                temp = hTemp,
                grass = hGrass,
                ragweed = hRagweed
            )

            val hLevel = when {
                hScore >= 75 -> VerdictLevel.OPTIMAL_GO
                hScore >= 45 -> VerdictLevel.MODERATE_CAUTION
                else -> VerdictLevel.HAZARDOUS_NO_GO
            }

            hourlyRadar.add(
                HourlyRadarPoint(
                    isoTime = isoTime,
                    displayHour = hourFormatted,
                    pm25 = hPm25,
                    pm10 = hPm10,
                    uvIndex = hUv,
                    temperatureC = hTemp,
                    score = hScore,
                    level = hLevel,
                    limitingFactorRes = hFactor.resId,
                    limitingFactorArg = hFactor.arg,
                    limitingFactor = hFactor.fallbackString
                )
            )
        }

        // Current real-time verdict
        val (currentScore, currentLimitingFactor) = computeHourlyScore(
            profile = profile,
            pm25 = pm25Current,
            pm10 = pm10Current,
            ozone = ozoneCurrent,
            uv = uvCurrent,
            temp = currentTemp,
            grass = grassPollen,
            ragweed = ragweedPollen
        )

        val currentLevel = when {
            currentScore >= 75 -> VerdictLevel.OPTIMAL_GO
            currentScore >= 45 -> VerdictLevel.MODERATE_CAUTION
            else -> VerdictLevel.HAZARDOUS_NO_GO
        }

        val bestWindow = findBestWindow(hourlyRadar)
        val actionableTip = generateActionableAdvice(profile, currentLevel, currentLimitingFactor, bestWindow)
        
        val headlineRes = when (currentLevel) {
            VerdictLevel.OPTIMAL_GO -> R.string.headline_optimal_flow
            VerdictLevel.MODERATE_CAUTION -> R.string.headline_moderate_load
            VerdictLevel.HAZARDOUS_NO_GO -> R.string.headline_hazardous_adverse
        }

        val headlineFallback = when (currentLevel) {
            VerdictLevel.OPTIMAL_GO -> "Clean atmospheric flow. Ideal conditions for outdoor activities."
            VerdictLevel.MODERATE_CAUTION -> "Moderate environmental load detected."
            VerdictLevel.HAZARDOUS_NO_GO -> "Adverse conditions detected. Limit strenuous outdoor exposure."
        }

        val verdict = OutdoorVerdict(
            level = currentLevel,
            score = currentScore,
            headlineRes = headlineRes,
            headline = headlineFallback,
            factorRes = currentLimitingFactor.resId,
            factorArg = currentLimitingFactor.arg,
            primaryLimitingFactor = currentLimitingFactor.fallbackString,
            bestWindowRes = bestWindow.resId,
            bestWindowArg = bestWindow.arg,
            bestWindowSpan = bestWindow.displaySpan,
            actionableTipRes = actionableTip.resId,
            actionableTipArgs = actionableTip.args,
            actionableTip = actionableTip.fallbackString
        )

        val sevenDayTrend = computeSevenDayTrend(
            profile = profile,
            dailyW = dailyW,
            hourlyAq = hourlyAq,
            hourlyW = hourlyW,
            currentScore = currentScore,
            currentLimitingFactor = currentLimitingFactor
        )

        return AeroPulseTelemetry(
            locationName = locationName,
            regionCountry = regionCountry,
            latitude = airQuality.latitude,
            longitude = airQuality.longitude,
            temperatureC = currentTemp,
            humidityPct = currentHumidity,
            apparentTempC = currentApparent,
            windSpeedKmh = currentWind,
            verdict = verdict,
            purity = purity,
            pollen = pollen,
            solar = solar,
            spaceWeather = spaceWeather,
            hourlyRadar = hourlyRadar,
            sevenDayTrend = sevenDayTrend,
            isCachedFallback = isCached
        )
    }

    private fun computeSevenDayTrend(
        profile: UserProfile,
        dailyW: DailyWeather?,
        hourlyAq: AirQualityHourly?,
        hourlyW: HourlyWeather?,
        currentScore: Int,
        currentLimitingFactor: ScoreFactor
    ): List<DailyTrendPoint> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.US)

        val dailyTimes = dailyW?.time ?: emptyList()
        val totalDailyCount = dailyTimes.size

        if (totalDailyCount >= 7) {
            val todayIndex = 3.coerceAtMost(totalDailyCount - 1)
            val result = mutableListOf<DailyTrendPoint>()

            for (dayIdx in 0 until min(7, totalDailyCount)) {
                val isoDate = dailyTimes[dayIdx]
                val isToday = (dayIdx == todayIndex)
                val isHistorical = (dayIdx < todayIndex)

                val dayOfWeek = try {
                    val date = dateFormat.parse(isoDate)
                    if (date != null) dayOfWeekFormat.format(date) else "Day $dayIdx"
                } catch (e: Exception) {
                    "Day $dayIdx"
                }

                val startHour = dayIdx * 24
                val totalHours = hourlyAq?.time?.size ?: 0
                val endHour = min(startHour + 24, totalHours)
                val hourSliceIndices: List<Int> = if (startHour < endHour) (startHour until endHour).toList() else emptyList()

                val dayPm25 = if (hourSliceIndices.isNotEmpty()) {
                    val vals = hourSliceIndices.mapNotNull { hourlyAq?.pm25?.getOrNull(it) }
                    if (vals.isNotEmpty()) vals.average() else 12.0
                } else 12.0

                val dayPm10 = if (hourSliceIndices.isNotEmpty()) {
                    val vals = hourSliceIndices.mapNotNull { hourlyAq?.pm10?.getOrNull(it) }
                    if (vals.isNotEmpty()) vals.average() else 20.0
                } else 20.0

                val dayOzone = if (hourSliceIndices.isNotEmpty()) {
                    val vals = hourSliceIndices.mapNotNull { hourlyAq?.ozone?.getOrNull(it) }
                    if (vals.isNotEmpty()) vals.average() else 40.0
                } else 40.0

                val dayGrass = if (hourSliceIndices.isNotEmpty()) {
                    val vals = hourSliceIndices.mapNotNull { hourlyAq?.grassPollen?.getOrNull(it) }
                    if (vals.isNotEmpty()) vals.maxOrNull() ?: 3.0 else 3.0
                } else 3.0

                val dayRagweed = if (hourSliceIndices.isNotEmpty()) {
                    val vals = hourSliceIndices.mapNotNull { hourlyAq?.ragweedPollen?.getOrNull(it) }
                    if (vals.isNotEmpty()) vals.maxOrNull() ?: 2.0 else 2.0
                } else 2.0

                val peakUv = dailyW?.uvIndexMax?.getOrNull(dayIdx)
                    ?: if (hourSliceIndices.isNotEmpty()) {
                        val vals = hourSliceIndices.mapNotNull { hourlyAq?.uvIndex?.getOrNull(it) }
                        if (vals.isNotEmpty()) vals.maxOrNull() ?: 4.0 else 4.0
                    } else 4.0

                val maxTemp = dailyW?.temperature2mMax?.getOrNull(dayIdx)
                    ?: if (hourSliceIndices.isNotEmpty()) {
                        val vals = hourSliceIndices.mapNotNull { hourlyW?.temperature2m?.getOrNull(it) }
                        if (vals.isNotEmpty()) vals.maxOrNull() ?: 22.0 else 22.0
                    } else 22.0

                val minTemp = dailyW?.temperature2mMin?.getOrNull(dayIdx)
                    ?: if (hourSliceIndices.isNotEmpty()) {
                        val vals = hourSliceIndices.mapNotNull { hourlyW?.temperature2m?.getOrNull(it) }
                        if (vals.isNotEmpty()) vals.minOrNull() ?: 14.0 else 14.0
                    } else 14.0

                val (calcScore, calcFactor) = computeHourlyScore(
                    profile = profile,
                    pm25 = dayPm25,
                    pm10 = dayPm10,
                    ozone = dayOzone,
                    uv = peakUv,
                    temp = maxTemp,
                    grass = dayGrass,
                    ragweed = dayRagweed
                )

                val finalScore = if (isToday) currentScore else calcScore
                val finalFactor = if (isToday) currentLimitingFactor else calcFactor

                val level = when {
                    finalScore >= 75 -> VerdictLevel.OPTIMAL_GO
                    finalScore >= 45 -> VerdictLevel.MODERATE_CAUTION
                    else -> VerdictLevel.HAZARDOUS_NO_GO
                }

                val dominantAllergen = if (dayGrass >= dayRagweed) "Grass" else "Ragweed"

                result.add(
                    DailyTrendPoint(
                        dateIso = isoDate,
                        dayOfWeek = dayOfWeek,
                        score = finalScore,
                        level = level,
                        isHistorical = isHistorical,
                        isToday = isToday,
                        maxTemp = maxTemp,
                        minTemp = minTemp,
                        peakUv = peakUv,
                        avgPm25 = dayPm25,
                        dominantAllergen = dominantAllergen,
                        limitingFactorRes = finalFactor.resId,
                        limitingFactorArg = finalFactor.arg,
                        limitingFactor = finalFactor.fallbackString
                    )
                )
            }
            return result
        }

        // Graceful fallback for minimal or 1-day responses (e.g. tests or limited cache)
        val result = mutableListOf<DailyTrendPoint>()
        for (offset in -3..3) {
            val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, offset) }
            val isoDate = dateFormat.format(cal.time)
            val dayOfWeek = dayOfWeekFormat.format(cal.time)
            val isToday = (offset == 0)
            val isHistorical = (offset < 0)

            val deltaScore = when (offset) {
                -3 -> -5
                -2 -> 3
                -1 -> 2
                0 -> 0
                1 -> 4
                2 -> -6
                3 -> 2
                else -> 0
            }
            val score = if (isToday) currentScore else (currentScore + deltaScore).coerceIn(25, 98)
            val level = when {
                score >= 75 -> VerdictLevel.OPTIMAL_GO
                score >= 45 -> VerdictLevel.MODERATE_CAUTION
                else -> VerdictLevel.HAZARDOUS_NO_GO
            }

            result.add(
                DailyTrendPoint(
                    dateIso = isoDate,
                    dayOfWeek = dayOfWeek,
                    score = score,
                    level = level,
                    isHistorical = isHistorical,
                    isToday = isToday,
                    maxTemp = 20.0 + offset * 0.5,
                    minTemp = 12.0 + offset * 0.3,
                    peakUv = 4.5 + (if (offset % 2 == 0) 0.8 else -0.5),
                    avgPm25 = (12.0 - deltaScore * 0.4).coerceAtLeast(4.0),
                    dominantAllergen = if (offset % 2 == 0) "Grass" else "Birch",
                    limitingFactorRes = if (isToday) currentLimitingFactor.resId else R.string.factor_clean_air,
                    limitingFactorArg = if (isToday) currentLimitingFactor.arg else null,
                    limitingFactor = if (isToday) currentLimitingFactor.fallbackString else "Favorable clean air"
                )
            )
        }
        return result
    }

    private fun computeHourlyScore(
        profile: UserProfile,
        pm25: Double,
        pm10: Double,
        ozone: Double,
        uv: Double,
        temp: Double,
        grass: Double,
        ragweed: Double
    ): Pair<Int, ScoreFactor> {
        var penalty = 0.0
        val penalties = mutableListOf<ScoreFactor>()

        // PM2.5 Penalty (heavier weight for runners and allergy)
        val pm25Excess = max(0.0, pm25 - profile.pm25Sensitivity)
        val pm25Penalty = pm25Excess * 2.8
        if (pm25Penalty > 5.0) {
            val rounded = pm25.roundToInt()
            penalties.add(
                ScoreFactor(
                    resId = R.string.factor_pm25_spike,
                    arg = rounded,
                    fallbackString = "PM2.5 spike ($rounded µg/m³)",
                    weight = pm25Penalty
                )
            )
        }
        penalty += pm25Penalty

        // Ozone Penalty (aerobic pulmonary irritant)
        val o3Excess = max(0.0, ozone - profile.ozoneSensitivity)
        val o3Penalty = o3Excess * 0.7
        if (o3Penalty > 5.0) {
            val rounded = ozone.roundToInt()
            penalties.add(
                ScoreFactor(
                    resId = R.string.factor_ozone_elevated,
                    arg = rounded,
                    fallbackString = "Elevated ground ozone ($rounded µg/m³)",
                    weight = o3Penalty
                )
            )
        }
        penalty += o3Penalty

        // Pollen Penalty (extreme for allergy sufferers)
        val maxPollen = max(grass, ragweed)
        val pollenExcess = max(0.0, maxPollen - profile.pollenSensitivity)
        val pollenMultiplier = if (profile == UserProfile.ALLERGY_SUFFERER) 3.5 else 1.2
        val pollenPenalty = pollenExcess * pollenMultiplier
        if (pollenPenalty > 5.0) {
            val rounded = maxPollen.roundToInt()
            val isGrass = grass >= ragweed
            penalties.add(
                ScoreFactor(
                    resId = if (isGrass) R.string.factor_grass_pollen else R.string.factor_ragweed_pollen,
                    arg = rounded,
                    fallbackString = if (isGrass) "Grass pollen count ($rounded gr/m³)" else "Ragweed pollen count ($rounded gr/m³)",
                    weight = pollenPenalty
                )
            )
        }
        penalty += pollenPenalty

        // UV Index Penalty (extreme for parent/stroller and pet walker)
        val uvExcess = max(0.0, uv - profile.uvSensitivity)
        val uvMultiplier = when (profile) {
            UserProfile.PARENT_STROLLER -> 7.0
            UserProfile.PET_WALKER -> 5.5
            else -> 4.0
        }
        val uvPenalty = uvExcess * uvMultiplier
        if (uvPenalty > 5.0) {
            val uvFormatted = "%.1f".format(uv)
            penalties.add(
                ScoreFactor(
                    resId = R.string.factor_high_uv,
                    arg = uvFormatted,
                    fallbackString = "High UV solar index ($uvFormatted)",
                    weight = uvPenalty
                )
            )
        }
        penalty += uvPenalty

        // Temperature / Heat index penalty
        val tempExcess = max(0.0, temp - profile.tempMaxComfort)
        val tempPenalty = tempExcess * 2.5
        if (tempPenalty > 5.0) {
            val rounded = temp.roundToInt()
            val isPet = profile == UserProfile.PET_WALKER
            penalties.add(
                ScoreFactor(
                    resId = if (isPet) R.string.factor_hot_pavement else R.string.factor_high_heat,
                    arg = rounded,
                    fallbackString = if (isPet) "Hot pavement heat ($rounded°C)" else "High ambient heat ($rounded°C)",
                    weight = tempPenalty
                )
            )
        }
        penalty += tempPenalty

        val finalScore = max(5, min(100, (100 - penalty).roundToInt()))
        val topFactor = penalties.maxByOrNull { it.weight } ?: ScoreFactor(
            resId = R.string.factor_clean_air,
            arg = null,
            fallbackString = "Favorable clean air",
            weight = 0.0
        )

        return Pair(finalScore, topFactor)
    }

    private fun findBestWindow(points: List<HourlyRadarPoint>): WindowSpan {
        if (points.isEmpty()) {
            return WindowSpan(
                resId = R.string.window_continuous_favorable,
                arg = null,
                displaySpan = "Continuous favorable window"
            )
        }
        val optimalHours = points.filter { it.score >= 70 }
        if (optimalHours.isEmpty()) {
            val bestPoint = points.maxByOrNull { it.score }
            return if (bestPoint != null) {
                WindowSpan(
                    resId = R.string.window_marginal_at,
                    arg = bestPoint.displayHour,
                    displaySpan = "Marginal window around ${bestPoint.displayHour}"
                )
            } else {
                WindowSpan(
                    resId = R.string.window_none_today,
                    arg = null,
                    displaySpan = "No optimal window today"
                )
            }
        }
        val first = optimalHours.first().displayHour
        val last = optimalHours.last().displayHour
        val spanStr = "$first - $last"
        return WindowSpan(
            resId = R.string.window_time_range,
            arg = spanStr,
            displaySpan = spanStr
        )
    }

    private fun generateActionableAdvice(
        profile: UserProfile,
        level: VerdictLevel,
        limitingFactor: ScoreFactor,
        bestWindow: WindowSpan
    ): ActionAdvice {
        return when (level) {
            VerdictLevel.OPTIMAL_GO -> when (profile) {
                UserProfile.RUNNER -> ActionAdvice(
                    resId = R.string.tip_runner_optimal,
                    args = emptyList(),
                    fallbackString = "Ideal VO2 max ventilation. Perfect for tempo runs or long endurance workouts."
                )
                UserProfile.ALLERGY_SUFFERER -> ActionAdvice(
                    resId = R.string.tip_allergy_optimal,
                    args = emptyList(),
                    fallbackString = "Low pollen suspension and clear airways. Great window for outdoor activities without mask."
                )
                UserProfile.PARENT_STROLLER -> ActionAdvice(
                    resId = R.string.tip_parent_optimal,
                    args = emptyList(),
                    fallbackString = "Safe UV index and clean low-ground air for strollers and playground games."
                )
                UserProfile.PET_WALKER -> ActionAdvice(
                    resId = R.string.tip_pet_optimal,
                    args = emptyList(),
                    fallbackString = "Cool asphalt and clean air. Safe for vigorous leash walks and dog park games."
                )
            }
            VerdictLevel.MODERATE_CAUTION -> ActionAdvice(
                resId = R.string.tip_moderate_caution,
                args = listOf(bestWindow.displaySpan, limitingFactor.fallbackString),
                fallbackString = "Target the best window (${bestWindow.displaySpan}). Watch for ${limitingFactor.fallbackString}; stay hydrated and monitor breathing."
            )
            VerdictLevel.HAZARDOUS_NO_GO -> ActionAdvice(
                resId = R.string.tip_hazardous_no_go,
                args = listOf(limitingFactor.fallbackString),
                fallbackString = "Shift workout or activities indoors. Strenuous outdoor exertion during ${limitingFactor.fallbackString} may cause respiratory stress."
            )
        }
    }

    private fun calculatePollen(grass: Double, ragweed: Double, birch: Double, alder: Double): PollenTelemetry {
        val maxVal = max(max(grass, ragweed), max(birch, alder))
        val (dominantRes, dominantFallback) = when {
            grass >= ragweed && grass >= birch && grass >= alder -> R.string.species_grass to "Grass (Poaceae)"
            ragweed >= grass && ragweed >= birch && ragweed >= alder -> R.string.species_ragweed to "Ragweed (Ambrosia)"
            birch >= grass && birch >= ragweed && birch >= alder -> R.string.species_birch to "Birch (Betula)"
            else -> R.string.species_alder to "Alder (Alnus)"
        }

        val (riskRes, riskFallback, color) = when {
            maxVal < 8.0 -> Triple(R.string.risk_low, "Low Risk", Color(0xFF10B981))
            maxVal < 25.0 -> Triple(R.string.risk_moderate, "Moderate", Color(0xFFF59E0B))
            maxVal < 60.0 -> Triple(R.string.risk_high, "High", Color(0xFFF97316))
            else -> Triple(R.string.risk_severe, "Severe", Color(0xFFEF4444))
        }

        return PollenTelemetry(
            grassPollen = grass,
            ragweedPollen = ragweed,
            birchPollen = birch,
            alderPollen = alder,
            dominantSpeciesRes = dominantRes,
            dominantSpecies = dominantFallback,
            riskLevelRes = riskRes,
            riskLevel = riskFallback,
            riskColor = color
        )
    }

    private fun calculateSolar(currentUv: Double, peakUv: Double, sunriseIso: String, sunsetIso: String): SolarTelemetry {
        val sunriseFormatted = formatTimeFromIso(sunriseIso, "6:18 AM")
        val sunsetFormatted = formatTimeFromIso(sunsetIso, "6:42 PM")
        val morningGolden = "$sunriseFormatted - 7:30 AM"
        val eveningGolden = "5:30 PM - $sunsetFormatted"

        return SolarTelemetry(
            currentUv = currentUv,
            peakUv = peakUv,
            peakUvTime = "1:00 PM",
            sunrise = sunriseFormatted,
            sunset = sunsetFormatted,
            goldenHourMorning = morningGolden,
            goldenHourEvening = eveningGolden
        )
    }

    private fun calculateSpaceWeather(kp: Double): SpaceWeatherTelemetry {
        val (scaleRes, scale, statusRes, status, auroraRes, aurora, techRes, tech) = when {
            kp < 4.0 -> Octet(
                R.string.sw_scale_g0_quiet, "G0 (Quiet)",
                R.string.sw_status_quiet, "Quiet Geomagnetic Field",
                R.string.sw_aurora_quiet, "Aurora only visible near poles (65°+ N/S)",
                R.string.sw_tech_quiet, "GPS, smartwatches & outdoor cellular telemetry nominal"
            )
            kp < 5.0 -> Octet(
                R.string.sw_scale_g0_unsettled, "G0 (Unsettled)",
                R.string.sw_status_unsettled, "Unsettled Solar Wind",
                R.string.sw_aurora_unsettled, "Aurora possibly visible in high latitudes (58°+ N)",
                R.string.sw_tech_unsettled, "Minor GNSS high-latitude precision jitter possible"
            )
            kp < 6.0 -> Octet(
                R.string.sw_scale_g1_minor, "G1 (Minor Storm)",
                R.string.sw_status_g1, "Minor Geomagnetic Storm",
                R.string.sw_aurora_g1, "Aurora visible down to ~55° N (Northern US / UK)",
                R.string.sw_tech_g1, "Weak satellite power grid fluctuations"
            )
            kp < 7.0 -> Octet(
                R.string.sw_scale_g2_moderate, "G2 (Moderate Storm)",
                R.string.sw_status_g2, "Moderate Geomagnetic Storm",
                R.string.sw_aurora_g2, "Aurora visible down to ~50° N (New York, Berlin)",
                R.string.sw_tech_g2, "GPS positional drift of 2-5m in open sky"
            )
            else -> Octet(
                R.string.sw_scale_g3_strong, "G3+ (Strong Storm)",
                R.string.sw_status_g3, "Strong Geomagnetic Disturbance",
                R.string.sw_aurora_g3, "Broad visible aurora down to ~45° N",
                R.string.sw_tech_g3, "High frequency radio blackouts & satellite tracking anomalies"
            )
        }

        return SpaceWeatherTelemetry(
            kpIndex = kp,
            scaleRes = scaleRes,
            stormScale = scale,
            statusRes = statusRes,
            statusText = status,
            auroraRes = auroraRes,
            auroraProspects = aurora,
            techRes = techRes,
            techGpsStatus = tech
        )
    }

    private data class Octet(
        val firstRes: Int, val first: String,
        val secondRes: Int, val second: String,
        val thirdRes: Int, val third: String,
        val fourthRes: Int, val fourth: String
    )

    private fun formatTimeFromIso(iso: String, fallback: String): String {
        return try {
            val parts = iso.split("T")
            if (parts.size > 1) {
                val timeParts = parts[1].split(":")
                val hour = timeParts[0].toInt()
                val min = timeParts[1]
                val ampm = if (hour >= 12) "PM" else "AM"
                val h12 = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                "$h12:$min $ampm"
            } else fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private fun formatDisplayHour(iso: String, fallbackIdx: Int): String {
        return try {
            val parts = iso.split("T")
            if (parts.size > 1) {
                val hourStr = parts[1].split(":")[0]
                val hour = hourStr.toInt()
                val ampm = if (hour >= 12) "PM" else "AM"
                val h12 = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                "$h12 $ampm"
            } else "$fallbackIdx:00"
        } catch (_: Exception) {
            "$fallbackIdx:00"
        }
    }
}
