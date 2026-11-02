package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telemetry_cache")
data class TelemetryCacheEntity(
    @PrimaryKey
    val locationName: String,
    val regionCountry: String,
    val latitude: Double,
    val longitude: Double,
    val jsonPayload: String, // serialized AeroPulseTelemetry json via Moshi
    val timestamp: Long = System.currentTimeMillis()
)
