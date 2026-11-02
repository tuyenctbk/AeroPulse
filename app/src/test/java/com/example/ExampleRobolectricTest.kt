package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AirQualityHourly
import com.example.data.model.AirQualityResponse
import com.example.data.model.CurrentWeather
import com.example.data.model.DailyWeather
import com.example.data.model.WeatherResponse
import com.example.domain.GoNoGoEngine
import com.example.domain.UserProfile
import com.example.domain.VerdictLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AeroPulse", appName)
    }

    @Test
    fun `test go no go engine computes optimal window for clean air`() {
        val hours = List(24) { "2026-10-14T${if (it < 10) "0$it" else "$it"}:00" }
        val cleanAq = AirQualityResponse(
            latitude = 40.0,
            longitude = -105.0,
            hourly = AirQualityHourly(
                time = hours,
                pm25 = List(24) { 8.0 },
                pm10 = List(24) { 15.0 },
                ozone = List(24) { 40.0 },
                uvIndex = List(24) { 2.0 },
                grassPollen = List(24) { 2.0 },
                ragweedPollen = List(24) { 1.0 }
            )
        )
        val cleanWeather = WeatherResponse(
            latitude = 40.0,
            longitude = -105.0,
            current = CurrentWeather(temperature2m = 18.0),
            daily = DailyWeather(
                sunrise = listOf("2026-10-14T06:15"),
                sunset = listOf("2026-10-14T18:45"),
                uvIndexMax = listOf(4.5)
            )
        )

        val telemetry = GoNoGoEngine.computeTelemetry(
            locationName = "Boulder",
            regionCountry = "Colorado, US",
            airQuality = cleanAq,
            weather = cleanWeather,
            noaaData = null,
            profile = UserProfile.RUNNER
        )

        assertEquals(VerdictLevel.OPTIMAL_GO, telemetry.verdict.level)
        assertTrue(telemetry.verdict.score >= 80)
        assertEquals(24, telemetry.hourlyRadar.size)
    }

    @Test
    fun `test engagement manager smart prompt evaluation and anti-spam limits`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = com.example.ui.engagement.EngagementManager(context)

        // Initial launch metrics
        val initialMetrics = manager.getUsageMetrics()
        assertTrue(initialMetrics.sessionCount >= 1)

        // First 2 radar checks should return NONE (needs at least 3)
        var prompt = manager.recordRadarCheck(90)
        assertEquals(com.example.ui.engagement.EngagementDialogType.NONE, prompt)
        
        prompt = manager.recordRadarCheck(90)
        assertEquals(com.example.ui.engagement.EngagementDialogType.NONE, prompt)

        // 3rd check should suggest RATE_APP
        prompt = manager.recordRadarCheck(90)
        assertEquals(com.example.ui.engagement.EngagementDialogType.RATE_APP, prompt)

        // Mark shown
        manager.markPromptShown(prompt)

        // Immediately checking again must return NONE due to session & global 5-day cooldown
        prompt = manager.recordRadarCheck(90)
        assertEquals(com.example.ui.engagement.EngagementDialogType.NONE, prompt)
    }

    @Test
    fun `test engagement manager never prompts on initial launch`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = com.example.ui.engagement.EngagementManager(context)

        // Even with high checks or good score, initial launch flag strictly suppresses popups
        val prompt = manager.recordRadarCheck(verdictScore = 95, isInitialLaunch = true)
        assertEquals(com.example.ui.engagement.EngagementDialogType.NONE, prompt)
    }

    @Test
    fun `test outdoor spot evaluation within custom thresholds returns GO`() {
        val spot = com.example.data.model.OutdoorSpot(
            name = "Chautauqua Trail",
            latitude = 39.9989,
            longitude = -105.2828,
            maxPm25 = 25.0,
            maxGrassPollen = 20.0
        )
        val cleanAq = AirQualityResponse(
            latitude = 40.0,
            longitude = -105.0,
            hourly = AirQualityHourly(
                time = List(24) { "2026-10-14T00:00" },
                pm25 = listOf(10.0),
                pm10 = listOf(15.0),
                grassPollen = listOf(5.0),
                ragweedPollen = listOf(2.0)
            )
        )
        val weather = WeatherResponse(
            latitude = 40.0,
            longitude = -105.0,
            current = CurrentWeather(temperature2m = 20.0)
        )

        val evaluation = com.example.domain.SpotThresholdEvaluator.evaluate(spot, cleanAq, weather)
        assertEquals(VerdictLevel.OPTIMAL_GO, evaluation.verdictLevel)
        assertTrue(evaluation.score >= 80)
        assertEquals(0, evaluation.metricEvaluations.count { it.status == com.example.domain.MetricStatus.BREACH })
    }

    @Test
    fun `test outdoor spot evaluation exceeding custom threshold returns NO_GO`() {
        val spot = com.example.data.model.OutdoorSpot(
            name = "Sensitive Trail",
            latitude = 39.9989,
            longitude = -105.2828,
            maxPm25 = 15.0, // Strict threshold
            maxGrassPollen = 10.0
        )
        val highPollutionAq = AirQualityResponse(
            latitude = 40.0,
            longitude = -105.0,
            hourly = AirQualityHourly(
                time = List(24) { "2026-10-14T00:00" },
                pm25 = listOf(45.0), // Far exceeds threshold (15.0)
                pm10 = listOf(80.0),
                grassPollen = listOf(35.0), // Far exceeds threshold (10.0)
                ragweedPollen = listOf(5.0)
            )
        )
        val weather = WeatherResponse(
            latitude = 40.0,
            longitude = -105.0,
            current = CurrentWeather(temperature2m = 22.0)
        )

        val evaluation = com.example.domain.SpotThresholdEvaluator.evaluate(spot, highPollutionAq, weather)
        assertEquals(VerdictLevel.HAZARDOUS_NO_GO, evaluation.verdictLevel)
        assertTrue(evaluation.score < 55)
        assertTrue(evaluation.metricEvaluations.any { it.status == com.example.domain.MetricStatus.BREACH })
    }

    @Test
    fun `test outdoor spot evaluation incorporates UV index and sun sensitive mode triggers NO_GO`() {
        val sunSensitiveSpot = com.example.data.model.OutdoorSpot(
            name = "Sunny Meadows Park",
            latitude = 34.0522,
            longitude = -118.2437,
            maxPm25 = 25.0,
            maxUvIndex = 5.0,
            isSunSensitive = true
        )
        val highUvAq = AirQualityResponse(
            latitude = 34.0522,
            longitude = -118.2437,
            hourly = AirQualityHourly(
                time = List(24) { "2026-10-14T00:00" },
                pm25 = listOf(10.0),
                pm10 = listOf(15.0),
                uvIndex = listOf(7.5), // Exceeds 5.0 threshold
                grassPollen = listOf(2.0),
                ragweedPollen = listOf(1.0)
            )
        )
        val weather = WeatherResponse(
            latitude = 34.0522,
            longitude = -118.2437,
            current = CurrentWeather(temperature2m = 25.0),
            daily = DailyWeather(uvIndexMax = listOf(8.2))
        )

        val evaluation = com.example.domain.SpotThresholdEvaluator.evaluate(sunSensitiveSpot, highUvAq, weather)
        assertEquals(VerdictLevel.HAZARDOUS_NO_GO, evaluation.verdictLevel)
        assertTrue(evaluation.isSunSensitiveBreach)
        assertEquals(R.string.status_uv_breach, evaluation.factorRes)
        assertEquals("7.5", evaluation.factorArg)
        assertTrue(evaluation.metricEvaluations.any { it.isSolar && it.status == com.example.domain.MetricStatus.BREACH })
    }

    @Test
    fun `test outdoor spot evaluation with safe UV under sun sensitive mode returns OPTIMAL_GO`() {
        val sunSensitiveSpot = com.example.data.model.OutdoorSpot(
            name = "Shaded Grove Trail",
            latitude = 37.7749,
            longitude = -122.4194,
            maxPm25 = 25.0,
            maxUvIndex = 6.0,
            isSunSensitive = true
        )
        val safeAq = AirQualityResponse(
            latitude = 37.7749,
            longitude = -122.4194,
            hourly = AirQualityHourly(
                time = List(24) { "2026-10-14T00:00" },
                pm25 = listOf(8.0),
                pm10 = listOf(12.0),
                uvIndex = listOf(2.5), // Safe well below 6.0
                grassPollen = listOf(3.0),
                ragweedPollen = listOf(1.0)
            )
        )
        val weather = WeatherResponse(
            latitude = 37.7749,
            longitude = -122.4194,
            current = CurrentWeather(temperature2m = 19.0),
            daily = DailyWeather(uvIndexMax = listOf(3.5))
        )

        val evaluation = com.example.domain.SpotThresholdEvaluator.evaluate(sunSensitiveSpot, safeAq, weather)
        assertEquals(VerdictLevel.OPTIMAL_GO, evaluation.verdictLevel)
        assertTrue(evaluation.score >= 80)
        assertEquals(false, evaluation.isSunSensitiveBreach)
        assertEquals(0, evaluation.metricEvaluations.count { it.status == com.example.domain.MetricStatus.BREACH })
    }

    @Test
    fun `test UV risk tier classification and advice mapping`() {
        assertEquals(com.example.ui.components.UvRiskTier.LOW, com.example.ui.components.UvRiskTier.fromUv(1.5))
        assertEquals(com.example.ui.components.UvRiskTier.MODERATE, com.example.ui.components.UvRiskTier.fromUv(4.2))
        assertEquals(com.example.ui.components.UvRiskTier.HIGH, com.example.ui.components.UvRiskTier.fromUv(6.8))
        assertEquals(com.example.ui.components.UvRiskTier.VERY_HIGH, com.example.ui.components.UvRiskTier.fromUv(9.5))
        assertEquals(com.example.ui.components.UvRiskTier.EXTREME, com.example.ui.components.UvRiskTier.fromUv(11.8))
    }

    @Test
    fun `test pollen species layers and strings defined`() {
        val layers = com.example.ui.components.PollenSpeciesLayer.values()
        assertEquals(5, layers.size)
        assertTrue(layers.contains(com.example.ui.components.PollenSpeciesLayer.ALL))
        assertTrue(layers.contains(com.example.ui.components.PollenSpeciesLayer.GRASS))
        assertTrue(layers.contains(com.example.ui.components.PollenSpeciesLayer.RAGWEED))
        assertTrue(layers.contains(com.example.ui.components.PollenSpeciesLayer.BIRCH))
        assertTrue(layers.contains(com.example.ui.components.PollenSpeciesLayer.ALDER))
    }

    @Test
    fun `test 7-day trend calculation returns exactly 7 days with historical and forecast split`() {
        val cleanAq = AirQualityResponse(
            latitude = 40.0,
            longitude = -105.0,
            hourly = AirQualityHourly(
                time = List(24) { "2026-10-14T00:00" },
                pm25 = List(24) { 8.0 },
                pm10 = List(24) { 15.0 },
                uvIndex = List(24) { 2.0 }
            )
        )
        val cleanWeather = WeatherResponse(
            latitude = 40.0,
            longitude = -105.0,
            current = CurrentWeather(temperature2m = 18.0)
        )

        val telemetry = GoNoGoEngine.computeTelemetry(
            locationName = "Boulder",
            regionCountry = "Colorado, US",
            airQuality = cleanAq,
            weather = cleanWeather,
            noaaData = null,
            profile = UserProfile.RUNNER
        )

        assertEquals(7, telemetry.sevenDayTrend.size)
        
        // Exactly 3 historical days (offsets -3, -2, -1)
        val historicalDays = telemetry.sevenDayTrend.filter { it.isHistorical }
        assertEquals(3, historicalDays.size)

        // Exactly 1 today day (offset 0)
        val todayDays = telemetry.sevenDayTrend.filter { it.isToday }
        assertEquals(1, todayDays.size)

        // Exactly 3 upcoming forecast days (offsets 1, 2, 3)
        val forecastDays = telemetry.sevenDayTrend.filter { !it.isHistorical && !it.isToday }
        assertEquals(3, forecastDays.size)

        // Today's score matches the real-time verdict score
        assertEquals(telemetry.verdict.score, todayDays.first().score)
    }

    @Test
    fun `test 7-day trend with full multi-day daily input maps historical and forecast accurately`() {
        val days = listOf("2026-10-29", "2026-10-30", "2026-10-31", "2026-11-01", "2026-11-02", "2026-11-03", "2026-11-04")
        val dailyWeather = DailyWeather(
            time = days,
            sunrise = List(7) { "06:15" },
            sunset = List(7) { "18:45" },
            uvIndexMax = listOf(3.0, 4.0, 5.0, 6.0, 7.0, 4.5, 3.5),
            temperature2mMax = listOf(18.0, 19.0, 20.0, 21.0, 22.0, 19.5, 17.0),
            temperature2mMin = listOf(10.0, 11.0, 12.0, 13.0, 12.5, 11.0, 9.0)
        )
        val aq = AirQualityResponse(
            latitude = 37.77,
            longitude = -122.41,
            hourly = AirQualityHourly(
                time = List(7 * 24) { "2026-10-29T00:00" },
                pm25 = List(7 * 24) { 10.0 },
                pm10 = List(7 * 24) { 20.0 },
                uvIndex = List(7 * 24) { 3.0 }
            )
        )
        val weather = WeatherResponse(
            latitude = 37.77,
            longitude = -122.41,
            current = CurrentWeather(temperature2m = 20.0),
            daily = dailyWeather
        )

        val telemetry = GoNoGoEngine.computeTelemetry(
            locationName = "San Francisco",
            regionCountry = "California, US",
            airQuality = aq,
            weather = weather,
            noaaData = null,
            profile = UserProfile.RUNNER
        )

        assertEquals(7, telemetry.sevenDayTrend.size)
        assertEquals("2026-10-29", telemetry.sevenDayTrend[0].dateIso)
        assertTrue(telemetry.sevenDayTrend[0].isHistorical)
        assertTrue(telemetry.sevenDayTrend[1].isHistorical)
        assertTrue(telemetry.sevenDayTrend[2].isHistorical)
        assertTrue(telemetry.sevenDayTrend[3].isToday)
        assertFalse(telemetry.sevenDayTrend[4].isHistorical)
        assertFalse(telemetry.sevenDayTrend[5].isHistorical)
        assertFalse(telemetry.sevenDayTrend[6].isHistorical)
    }
}
