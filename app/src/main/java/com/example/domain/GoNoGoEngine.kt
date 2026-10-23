package com.example.domain

import androidx.compose.ui.graphics.Color
import com.example.data.model.AirQualityResponse
import com.example.data.model.NoaaKpItem
import com.example.data.model.WeatherResponse
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

        // Extract current indices (or index 0 of hourly)
        val pm25Current = hourlyAq?.pm25?.firstOrNull() ?: 12.0
        val pm10Current = hourlyAq?.pm10?.firstOrNull() ?: 22.0
        val usAqiCurrent = hourlyAq?.usAqi?.firstOrNull()?.roundToInt() ?: 45
        val euAqiCurrent = hourlyAq?.europeanAqi?.firstOrNull()?.roundToInt() ?: 25
        val ozoneCurrent = hourlyAq?.ozone?.firstOrNull() ?: 45.0
        val no2Current = hourlyAq?.nitrogenDioxide?.firstOrNull() ?: 18.0
        val coCurrent = hourlyAq?.carbonMonoxide?.firstOrNull() ?: 210.0
        val dustCurrent = hourlyAq?.dust?.firstOrNull() ?: 5.0

        // Current Pollen values
        val grassPollen = hourlyAq?.grassPollen?.firstOrNull() ?: 4.0
        val ragweedPollen = hourlyAq?.ragweedPollen?.firstOrNull() ?: 2.0
        val birchPollen = hourlyAq?.birchPollen?.firstOrNull() ?: 3.0
        val alderPollen = hourlyAq?.alderPollen?.firstOrNull() ?: 1.0

        // Current UV
        val uvCurrent = hourlyAq?.uvIndex?.firstOrNull() ?: 2.5
        val peakUv = dailyW?.uvIndexMax?.firstOrNull() ?: 6.2
        val sunriseIso = dailyW?.sunrise?.firstOrNull() ?: "06:15"
        val sunsetIso = dailyW?.sunset?.firstOrNull() ?: "18:45"

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

        // Hourly timeline (24 hours)
        val hourlyCount = min(24, hourlyAq?.time?.size ?: 24)
        val hourlyRadar = mutableListOf<HourlyRadarPoint>()

        for (i in 0 until hourlyCount) {
            val isoTime = hourlyAq?.time?.getOrNull(i) ?: "2026-10-14T$i:00"
            val hourFormatted = formatDisplayHour(isoTime, i)
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
                    limitingFactor = hFactor
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
        val headline = when (currentLevel) {
            VerdictLevel.OPTIMAL_GO -> "Clean atmospheric flow. Ideal conditions for outdoor ${profile.title.lowercase()} activities."
            VerdictLevel.MODERATE_CAUTION -> "Moderate environmental load detected. ${currentLimitingFactor}."
            VerdictLevel.HAZARDOUS_NO_GO -> "Adverse conditions detected. High ${currentLimitingFactor}. Limit strenuous outdoor exposure."
        }

        val verdict = OutdoorVerdict(
            level = currentLevel,
            score = currentScore,
            headline = headline,
            primaryLimitingFactor = currentLimitingFactor,
            bestWindowSpan = bestWindow,
            actionableTip = actionableTip
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
            isCachedFallback = isCached
        )
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
    ): Pair<Int, String> {
        var penalty = 0.0
        val penalties = mutableListOf<Pair<String, Double>>()

        // PM2.5 Penalty (heavier weight for runners and allergy)
        val pm25Excess = max(0.0, pm25 - profile.pm25Sensitivity)
        val pm25Penalty = pm25Excess * 2.8
        if (pm25Penalty > 5.0) {
            penalties.add("PM2.5 spike (${pm25.roundToInt()} µg/m³)" to pm25Penalty)
        }
        penalty += pm25Penalty

        // Ozone Penalty (aerobic pulmonary irritant)
        val o3Excess = max(0.0, ozone - profile.ozoneSensitivity)
        val o3Penalty = o3Excess * 0.7
        if (o3Penalty > 5.0) {
            penalties.add("Elevated ground ozone (${ozone.roundToInt()} µg/m³)" to o3Penalty)
        }
        penalty += o3Penalty

        // Pollen Penalty (extreme for allergy sufferers)
        val maxPollen = max(grass, ragweed)
        val pollenExcess = max(0.0, maxPollen - profile.pollenSensitivity)
        val pollenMultiplier = if (profile == UserProfile.ALLERGY_SUFFERER) 3.5 else 1.2
        val pollenPenalty = pollenExcess * pollenMultiplier
        if (pollenPenalty > 5.0) {
            val allergen = if (grass >= ragweed) "Grass pollen" else "Ragweed pollen"
            penalties.add("$allergen count (${maxPollen.roundToInt()} gr/m³)" to pollenPenalty)
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
            penalties.add("High UV solar index (${"%.1f".format(uv)})" to uvPenalty)
        }
        penalty += uvPenalty

        // Temperature / Heat index penalty
        val tempExcess = max(0.0, temp - profile.tempMaxComfort)
        val tempPenalty = tempExcess * 2.5
        if (tempPenalty > 5.0) {
            val issue = if (profile == UserProfile.PET_WALKER) "Hot pavement heat" else "High ambient heat"
            penalties.add("$issue (${temp.roundToInt()}°C)" to tempPenalty)
        }
        penalty += tempPenalty

        val finalScore = max(5, min(100, (100 - penalty).roundToInt()))
        val topFactor = penalties.maxByOrNull { it.second }?.first ?: "Favorable clean air"

        return Pair(finalScore, topFactor)
    }

    private fun findBestWindow(points: List<HourlyRadarPoint>): String {
        if (points.isEmpty()) return "Continuous favorable window"
        val optimalHours = points.filter { it.score >= 70 }
        if (optimalHours.isEmpty()) {
            val bestPoint = points.maxByOrNull { it.score }
            return bestPoint?.displayHour?.let { "Marginal window around $it" } ?: "No optimal window today"
        }
        val first = optimalHours.first().displayHour
        val last = optimalHours.last().displayHour
        return "$first - $last"
    }

    private fun generateActionableAdvice(
        profile: UserProfile,
        level: VerdictLevel,
        limitingFactor: String,
        bestWindow: String
    ): String {
        return when (level) {
            VerdictLevel.OPTIMAL_GO -> when (profile) {
                UserProfile.RUNNER -> "Ideal VO2 max ventilation. Perfect for tempo runs or long endurance workouts."
                UserProfile.ALLERGY_SUFFERER -> "Low pollen suspension and clear airways. Great window for outdoor activities without mask."
                UserProfile.PARENT_STROLLER -> "Safe UV index and clean low-ground air for strollers and playground games."
                UserProfile.PET_WALKER -> "Cool asphalt and clean air. Safe for vigorous leash walks and dog park games."
            }
            VerdictLevel.MODERATE_CAUTION -> "Target the best window ($bestWindow). Watch for $limitingFactor; stay hydrated and monitor breathing."
            VerdictLevel.HAZARDOUS_NO_GO -> "Shift workout or activities indoors. Strenuous outdoor exertion during $limitingFactor may cause respiratory stress."
        }
    }

    private fun calculatePollen(grass: Double, ragweed: Double, birch: Double, alder: Double): PollenTelemetry {
        val maxVal = max(max(grass, ragweed), max(birch, alder))
        val dominant = when {
            grass >= ragweed && grass >= birch && grass >= alder -> "Grass (Poaceae)"
            ragweed >= grass && ragweed >= birch && ragweed >= alder -> "Ragweed (Ambrosia)"
            birch >= grass && birch >= ragweed && birch >= alder -> "Birch (Betula)"
            else -> "Alder (Alnus)"
        }

        val (risk, color) = when {
            maxVal < 8.0 -> "Low Risk" to Color(0xFF10B981)
            maxVal < 25.0 -> "Moderate" to Color(0xFFF59E0B)
            maxVal < 60.0 -> "High" to Color(0xFFF97316)
            else -> "Severe" to Color(0xFFEF4444)
        }

        return PollenTelemetry(
            grassPollen = grass,
            ragweedPollen = ragweed,
            birchPollen = birch,
            alderPollen = alder,
            dominantSpecies = dominant,
            riskLevel = risk,
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
        val (scale, status, aurora, tech) = when {
            kp < 4.0 -> Quad(
                "G0 (Quiet)",
                "Quiet Geomagnetic Field",
                "Aurora only visible near poles (65°+ N/S)",
                "GPS, smartwatches & outdoor cellular telemetry nominal"
            )
            kp < 5.0 -> Quad(
                "G0 (Unsettled)",
                "Unsettled Solar Wind",
                "Aurora possibly visible in high latitudes (58°+ N)",
                "Minor GNSS high-latitude precision jitter possible"
            )
            kp < 6.0 -> Quad(
                "G1 (Minor Storm)",
                "Minor Geomagnetic Storm",
                "Aurora visible down to ~55° N (Northern US / UK)",
                "Weak satellite power grid fluctuations"
            )
            kp < 7.0 -> Quad(
                "G2 (Moderate Storm)",
                "Moderate Geomagnetic Storm",
                "Aurora visible down to ~50° N (New York, Berlin)",
                "GPS positional drift of 2-5m in open sky"
            )
            else -> Quad(
                "G3+ (Strong Storm)",
                "Strong Geomagnetic Disturbance",
                "Broad visible aurora down to ~45° N",
                "High frequency radio blackouts & satellite tracking anomalies"
            )
        }

        return SpaceWeatherTelemetry(
            kpIndex = kp,
            stormScale = scale,
            statusText = status,
            auroraProspects = aurora,
            techGpsStatus = tech
        )
    }

    private data class Quad(val first: String, val second: String, val third: String, val fourth: String)

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
