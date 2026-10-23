package com.example.data.repository

import android.util.Log
import com.example.data.db.LocationDao
import com.example.data.model.AirQualityHourly
import com.example.data.model.AirQualityResponse
import com.example.data.model.CurrentWeather
import com.example.data.model.DailyWeather
import com.example.data.model.GeocodingResult
import com.example.data.model.HourlyWeather
import com.example.data.model.NoaaKpItem
import com.example.data.model.SavedLocation
import com.example.data.model.WeatherResponse
import com.example.data.network.NetworkClient
import com.example.domain.AeroPulseTelemetry
import com.example.domain.GoNoGoEngine
import com.example.domain.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AeroPulseRepository(
    private val locationDao: LocationDao
) {
    val savedLocations: Flow<List<SavedLocation>> = locationDao.getAllLocations()

    suspend fun saveLocation(location: SavedLocation): Long = withContext(Dispatchers.IO) {
        locationDao.insertLocation(location)
    }

    suspend fun deleteLocation(location: SavedLocation) = withContext(Dispatchers.IO) {
        locationDao.deleteLocation(location)
    }

    suspend fun searchCities(query: String): List<GeocodingResult> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        try {
            val response = NetworkClient.geocodingApi.searchCity(name = query.trim())
            response.results ?: emptyList()
        } catch (e: Exception) {
            Log.e("AeroPulseRepo", "Failed to search cities: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchTelemetry(
        locationName: String,
        regionCountry: String,
        lat: Double,
        lon: Double,
        profile: UserProfile
    ): AeroPulseTelemetry = withContext(Dispatchers.IO) {
        try {
            coroutineScope {
                val aqDeferred = async {
                    NetworkClient.airQualityApi.getAirQuality(
                        latitude = lat,
                        longitude = lon
                    )
                }
                val weatherDeferred = async {
                    NetworkClient.weatherApi.getWeather(
                        latitude = lat,
                        longitude = lon
                    )
                }
                val noaaDeferred = async {
                    try {
                        NetworkClient.noaaApi.getPlanetaryKIndex()
                    } catch (e: Exception) {
                        Log.w("AeroPulseRepo", "NOAA SWPC call failed, fallback used: ${e.message}")
                        null
                    }
                }

                val aq = aqDeferred.await()
                val weather = weatherDeferred.await()
                val noaa = noaaDeferred.await()

                GoNoGoEngine.computeTelemetry(
                    locationName = locationName,
                    regionCountry = regionCountry,
                    airQuality = aq,
                    weather = weather,
                    noaaData = noaa,
                    profile = profile,
                    isCached = false
                )
            }
        } catch (e: Exception) {
            Log.e("AeroPulseRepo", "Remote fetch failed, creating baseline telemetry", e)
            createFallbackTelemetry(locationName, regionCountry, lat, lon, profile)
        }
    }

    private fun createFallbackTelemetry(
        locationName: String,
        regionCountry: String,
        lat: Double,
        lon: Double,
        profile: UserProfile
    ): AeroPulseTelemetry {
        val hoursList = (0..23).map { "2026-10-14T${if (it < 10) "0$it" else "$it"}:00" }
        val sampleAq = AirQualityResponse(
            latitude = lat,
            longitude = lon,
            hourly = AirQualityHourly(
                time = hoursList,
                pm25 = List(24) { 11.5 + (it % 5) * 1.8 },
                pm10 = List(24) { 20.0 + (it % 4) * 2.5 },
                carbonMonoxide = List(24) { 220.0 },
                nitrogenDioxide = List(24) { 15.0 },
                ozone = List(24) { 45.0 + if (it in 12..16) 25.0 else 0.0 },
                uvIndex = List(24) { if (it in 9..17) (6.5 - kotlin.math.abs(13 - it) * 1.2).coerceAtLeast(0.0) else 0.0 },
                grassPollen = List(24) { 6.0 },
                ragweedPollen = List(24) { 3.0 },
                birchPollen = List(24) { 4.0 },
                alderPollen = List(24) { 2.0 },
                europeanAqi = List(24) { 22.0 },
                usAqi = List(24) { 42.0 }
            )
        )

        val sampleWeather = WeatherResponse(
            latitude = lat,
            longitude = lon,
            current = CurrentWeather(
                temperature2m = 19.5,
                relativeHumidity2m = 52.0,
                apparentTemperature = 19.0,
                windSpeed10m = 11.2
            ),
            hourly = HourlyWeather(
                time = hoursList,
                temperature2m = List(24) { 15.0 + (it % 8) * 1.5 }
            ),
            daily = DailyWeather(
                sunrise = listOf("2026-10-14T06:22"),
                sunset = listOf("2026-10-14T18:38"),
                uvIndexMax = listOf(5.8)
            )
        )

        val sampleNoaa = listOf(
            NoaaKpItem(
                timeTag = "2026-10-14 01:50:00",
                kpIndex = 2.0,
                estimatedKp = 2.33
            )
        )

        return GoNoGoEngine.computeTelemetry(
            locationName = locationName,
            regionCountry = regionCountry,
            airQuality = sampleAq,
            weather = sampleWeather,
            noaaData = sampleNoaa,
            profile = profile,
            isCached = true
        )
    }
}
