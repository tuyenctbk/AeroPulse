package com.example.data.repository

import android.util.Log
import com.example.data.db.LocationDao
import com.example.data.db.TelemetryCacheDao
import com.example.data.model.GeocodingResult
import com.example.data.model.SavedLocation
import com.example.data.model.TelemetryCacheEntity
import com.example.data.network.NetworkClient
import com.example.domain.AeroPulseTelemetry
import com.example.domain.GoNoGoEngine
import com.example.domain.UserProfile
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AeroPulseRepository(
    private val locationDao: LocationDao,
    private val telemetryCacheDao: TelemetryCacheDao
) {
    val savedLocations: Flow<List<SavedLocation>> = locationDao.getAllLocations()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val telemetryAdapter = moshi.adapter(AeroPulseTelemetry::class.java)

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

                // Save successfully fetched telemetry into Room cache
                try {
                    val json = telemetryAdapter.toJson(telemetry)
                    telemetryCacheDao.insertCache(
                        TelemetryCacheEntity(
                            locationName = locationName,
                            regionCountry = regionCountry,
                            latitude = lat,
                            longitude = lon,
                            jsonPayload = json,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } catch (ce: Exception) {
                    Log.e("AeroPulseRepo", "Failed to serialize and cache telemetry: ${ce.message}")
                }

                telemetry
            }
        } catch (e: Exception) {
            Log.e("AeroPulseRepo", "Remote fetch failed for $locationName. Attempting Room offline cache fallback...", e)
            try {
                val cachedEntity = telemetryCacheDao.getCache(locationName)
                if (cachedEntity != null) {
                    val telemetry = telemetryAdapter.fromJson(cachedEntity.jsonPayload)
                    if (telemetry != null) {
                        return@withContext telemetry.copy(isCachedFallback = true)
                    }
                }
            } catch (ce: Exception) {
                Log.e("AeroPulseRepo", "Failed to load from Room telemetry cache: ${ce.message}")
            }
            throw e
        }
    }
}

