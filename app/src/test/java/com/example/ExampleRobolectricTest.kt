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
}
