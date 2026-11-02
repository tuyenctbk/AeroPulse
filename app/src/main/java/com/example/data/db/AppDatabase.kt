package com.example.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.OutdoorSpot
import com.example.data.model.SavedLocation
import com.example.data.model.TelemetryCacheEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Dao
interface LocationDao {
    @Query("SELECT * FROM saved_locations ORDER BY lastAccessed DESC")
    fun getAllLocations(): Flow<List<SavedLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: SavedLocation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<SavedLocation>)

    @Delete
    suspend fun deleteLocation(location: SavedLocation)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteLocationById(id: Long)

    @Query("SELECT COUNT(*) FROM saved_locations")
    suspend fun count(): Int
}

@Dao
interface TelemetryCacheDao {
    @Query("SELECT * FROM telemetry_cache WHERE locationName = :locationName")
    suspend fun getCache(locationName: String): TelemetryCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: TelemetryCacheEntity)
}

@Database(
    entities = [SavedLocation::class, OutdoorSpot::class, TelemetryCacheEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun outdoorSpotDao(): OutdoorSpotDao
    abstract fun telemetryCacheDao(): TelemetryCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aeropulse_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialSpots(database.locationDao(), database.outdoorSpotDao())
                    }
                }
            }

            suspend fun populateInitialSpots(
                locationDao: LocationDao,
                outdoorSpotDao: OutdoorSpotDao
            ) {
                val defaultLocations = listOf(
                    SavedLocation(
                        name = "Boulder",
                        region = "Colorado",
                        country = "United States",
                        latitude = 40.0150,
                        longitude = -105.2705,
                        isFavorite = true,
                        isDefault = true
                    ),
                    SavedLocation(
                        name = "San Francisco",
                        region = "California",
                        country = "United States",
                        latitude = 37.7749,
                        longitude = -122.4194,
                        isFavorite = true
                    ),
                    SavedLocation(
                        name = "London",
                        region = "England",
                        country = "United Kingdom",
                        latitude = 51.5074,
                        longitude = -0.1278,
                        isFavorite = false
                    ),
                    SavedLocation(
                        name = "Tokyo",
                        region = "Kanto",
                        country = "Japan",
                        latitude = 35.6762,
                        longitude = 139.6503,
                        isFavorite = false
                    )
                )
                locationDao.insertAll(defaultLocations)

                // Pre-configure initial outdoor spots with tailored air quality, pollen & UV solar thresholds
                val defaultSpots = listOf(
                    OutdoorSpot(
                        name = "Chautauqua Mountain Trail",
                        latitude = 39.9989,
                        longitude = -105.2828,
                        description = "High-altitude trail running route with strict UV solar and ozone sensitivity",
                        isFavorite = true,
                        maxPm25 = 20.0,
                        maxPm10 = 40.0,
                        maxAqi = 75,
                        maxOzone = 80.0,
                        maxGrassPollen = 15.0,
                        maxRagweedPollen = 10.0,
                        maxBirchPollen = 20.0,
                        maxUvIndex = 5.0,
                        isSunSensitive = true
                    ),
                    OutdoorSpot(
                        name = "Central Park Reservoir Loop",
                        latitude = 40.7829,
                        longitude = -73.9654,
                        description = "Urban runner circuit with strict PM2.5, allergen and midday UV controls",
                        isFavorite = true,
                        maxPm25 = 25.0,
                        maxPm10 = 50.0,
                        maxAqi = 100,
                        maxOzone = 90.0,
                        maxGrassPollen = 20.0,
                        maxRagweedPollen = 15.0,
                        maxBirchPollen = 25.0,
                        maxUvIndex = 6.0,
                        isSunSensitive = false
                    ),
                    OutdoorSpot(
                        name = "Hyde Park Running Circuit",
                        latitude = 51.5073,
                        longitude = -0.1657,
                        description = "London parkway with low NO2 and birch allergen thresholds",
                        isFavorite = false,
                        maxPm25 = 22.0,
                        maxPm10 = 45.0,
                        maxAqi = 85,
                        maxOzone = 85.0,
                        maxGrassPollen = 18.0,
                        maxRagweedPollen = 12.0,
                        maxBirchPollen = 22.0,
                        maxUvIndex = 5.5,
                        isSunSensitive = false
                    )
                )
                outdoorSpotDao.insertAll(defaultSpots)
            }
        }
    }
}
