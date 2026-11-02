package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.OutdoorSpot
import kotlinx.coroutines.flow.Flow

@Dao
interface OutdoorSpotDao {
    @Query("SELECT * FROM outdoor_spots ORDER BY isFavorite DESC, createdAt DESC")
    fun getAllSpots(): Flow<List<OutdoorSpot>>

    @Query("SELECT * FROM outdoor_spots WHERE id = :id LIMIT 1")
    suspend fun getSpotById(id: Long): OutdoorSpot?

    @Query("SELECT * FROM outdoor_spots WHERE id = :id LIMIT 1")
    fun observeSpotById(id: Long): Flow<OutdoorSpot?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpot(spot: OutdoorSpot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(spots: List<OutdoorSpot>)

    @Update
    suspend fun updateSpot(spot: OutdoorSpot)

    @Delete
    suspend fun deleteSpot(spot: OutdoorSpot)

    @Query("DELETE FROM outdoor_spots WHERE id = :id")
    suspend fun deleteSpotById(id: Long)

    @Query("SELECT COUNT(*) FROM outdoor_spots")
    suspend fun count(): Int
}
