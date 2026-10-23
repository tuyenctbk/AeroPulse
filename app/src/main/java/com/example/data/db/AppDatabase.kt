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
import com.example.data.model.SavedLocation
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

@Database(entities = [SavedLocation::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao

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
                        populateInitialSpots(database.locationDao())
                    }
                }
            }

            suspend fun populateInitialSpots(dao: LocationDao) {
                val defaults = listOf(
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
                dao.insertAll(defaults)
            }
        }
    }
}
