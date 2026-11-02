package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * OutdoorSpot: A Kotlin data class and Room entity representing a user-saved outdoor spot
 * (e.g. running trail, bike path, park, athletic grounds, playground) that stores geographical
 * coordinates and user-defined thresholds for air quality pollutants, botanical pollen,
 * and solar UV radiation (especially critical for sun-sensitive users, infants, or runners).
 */
@Entity(tableName = "outdoor_spots")
data class OutdoorSpot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String = "",
    val isFavorite: Boolean = false,

    // User-defined Air Quality Thresholds (µg/m³ or AQI index)
    val maxPm25: Double = 25.0,           // WHO 24-hr guideline: 15-25 µg/m³
    val maxPm10: Double = 50.0,           // Coarse dust guideline: 45-50 µg/m³
    val maxAqi: Int = 100,                // US AQI index (<= 100 is Moderate or better)
    val maxOzone: Double = 100.0,         // Ground-level ozone (µg/m³)
    val maxNitrogenDioxide: Double = 40.0,// Nitrogen Dioxide limit (µg/m³)

    // User-defined Botanical Pollen Thresholds (grains / m³)
    val maxGrassPollen: Double = 20.0,    // Grass pollen allergen threshold
    val maxRagweedPollen: Double = 10.0,  // Ragweed allergen threshold
    val maxBirchPollen: Double = 25.0,    // Birch tree allergen threshold
    val maxAlderPollen: Double = 25.0,    // Alder tree allergen threshold
    val maxTotalPollen: Double = 50.0,    // Aggregate botanical pollen threshold

    // User-defined Solar UV Radiation Thresholds
    val maxUvIndex: Double = 6.0,         // Solar UV Index threshold (WHO: 6+ High, 8+ Very High)
    val isSunSensitive: Boolean = false,  // Sun-sensitivity mode: applies stricter weighting & sunburn warnings

    val createdAt: Long = System.currentTimeMillis()
)
