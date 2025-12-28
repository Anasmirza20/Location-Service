package com.locationtrackor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Query("SELECT * FROM location_logs WHERE isSynced = 0 ORDER BY timestamp ASC")
    fun getUnsyncedLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM location_logs WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedLocationsList(): List<LocationEntity>

    @Query("UPDATE location_logs SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM location_logs WHERE isSynced = 0")
    fun getUnsyncedCount(): Flow<Int>
}
