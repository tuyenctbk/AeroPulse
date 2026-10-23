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

                val telemetry = GoNoGoEngine.computeTelemetry(
                    locationName = locationName,
                    regionCountry = regionCountry,
                    airQuality = aq,
                    weather = weather,
                    noaaData = noaa,
                    profile = profile,
                    isCached = false
                )
                lastSuccessfulTelemetry = telemetry
                telemetry
            }
        } catch (e: Exception) {
            Log.e("AeroPulseRepo", "Remote fetch failed for $locationName", e)
            val cached = lastSuccessfulTelemetry
            if (cached != null && cached.locationName.equals(locationName, ignoreCase = true)) {
                cached.copy(isCachedFallback = true)
            } else {
                throw e
            }
        }
    }

    private var lastSuccessfulTelemetry: AeroPulseTelemetry? = null
}
