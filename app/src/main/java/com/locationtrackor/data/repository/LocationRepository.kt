package com.locationtrackor.data.repository

import com.locationtrackor.data.local.LocationDao
import com.locationtrackor.data.local.LocationEntity
import com.locationtrackor.data.remote.LocationApi
import com.locationtrackor.data.remote.LocationRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao,
    private val locationApi: LocationApi
) {
    fun getUnsyncedCount(): Flow<Int> = locationDao.getUnsyncedCount()

    suspend fun saveLocation(location: LocationEntity) {
        locationDao.insertLocation(location)
    }

    suspend fun syncLocations(): Boolean {
        val unsynced = locationDao.getUnsyncedLocationsList()
        if (unsynced.isEmpty()) return true

        val requests = unsynced.map {
            LocationRequest(
                employeeId = it.employeeId,
                latitude = it.latitude,
                longitude = it.longitude,
                accuracy = it.accuracy,
                timestamp = it.timestamp,
                speed = it.speed
            )
        }

        return try {
            val response = locationApi.uploadLocations(requests)
            if (response.isSuccessful) {
                locationDao.markAsSynced(unsynced.map { it.id })
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
