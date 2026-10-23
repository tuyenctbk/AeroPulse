package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AirQualityResponse(
    val latitude: Double,
    val longitude: Double,
    val hourly: AirQualityHourly? = null
)

@JsonClass(generateAdapter = true)
data class AirQualityHourly(
    val time: List<String> = emptyList(),
    @Json(name = "pm2_5") val pm25: List<Double?> = emptyList(),
    val pm10: List<Double?> = emptyList(),
    @Json(name = "carbon_monoxide") val carbonMonoxide: List<Double?> = emptyList(),
    @Json(name = "nitrogen_dioxide") val nitrogenDioxide: List<Double?> = emptyList(),
    @Json(name = "sulphur_dioxide") val sulphurDioxide: List<Double?> = emptyList(),
    val ozone: List<Double?> = emptyList(),
    @Json(name = "aerosol_optical_depth") val aerosolOpticalDepth: List<Double?> = emptyList(),
    val dust: List<Double?> = emptyList(),
    @Json(name = "uv_index") val uvIndex: List<Double?> = emptyList(),
    @Json(name = "alder_pollen") val alderPollen: List<Double?> = emptyList(),
    @Json(name = "birch_pollen") val birchPollen: List<Double?> = emptyList(),
    @Json(name = "grass_pollen") val grassPollen: List<Double?> = emptyList(),
    @Json(name = "mugwort_pollen") val mugwortPollen: List<Double?> = emptyList(),
    @Json(name = "olive_pollen") val olivePollen: List<Double?> = emptyList(),
    @Json(name = "ragweed_pollen") val ragweedPollen: List<Double?> = emptyList(),
    @Json(name = "european_aqi") val europeanAqi: List<Double?> = emptyList(),
    @Json(name = "us_aqi") val usAqi: List<Double?> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentWeather? = null,
    val hourly: HourlyWeather? = null,
    val daily: DailyWeather? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    val time: String = "",
    @Json(name = "temperature_2m") val temperature2m: Double = 0.0,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Double = 0.0,
    @Json(name = "apparent_temperature") val apparentTemperature: Double = 0.0,
    val precipitation: Double = 0.0,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class HourlyWeather(
    val time: List<String> = emptyList(),
    @Json(name = "temperature_2m") val temperature2m: List<Double?> = emptyList(),
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Double?> = emptyList(),
    @Json(name = "precipitation_probability") val precipitationProbability: List<Double?> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DailyWeather(
    val time: List<String> = emptyList(),
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList(),
    @Json(name = "uv_index_max") val uvIndexMax: List<Double?> = emptyList()
)

@JsonClass(generateAdapter = true)
data class NoaaKpItem(
    @Json(name = "time_tag") val timeTag: String = "",
    @Json(name = "kp_index") val kpIndex: Double? = null,
    @Json(name = "estimated_kp") val estimatedKp: Double? = null,
    val kp: Double? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @Json(name = "admin1") val adminRegion: String? = null,
    val country: String? = null,
    @Json(name = "country_code") val countryCode: String? = null
)
