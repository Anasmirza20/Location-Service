package com.locationtrackor.data.remote

import com.locationtrackor.data.local.LocationEntity
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class LocationRequest(
    val employeeId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val speed: Float?
)

interface LocationApi {
    @POST("location/update")
    suspend fun uploadLocation(@Body location: LocationRequest): Response<Unit>

    @POST("/posts")
    suspend fun uploadLocations(@Body locations: List<LocationRequest>): Response<Unit>
}
