package com.akhilasdeveloper.marsroverphotos.api

import com.akhilasdeveloper.marsroverphotos.Constants
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface MarsRoverPhotosService {

    @GET("photos")
    suspend fun getMarsRoverPhotos(
        @Query("earth_date") earth_date : String,
        @Query("api_key") api_key : String
    ): MarsRoverPhotosApiResponse?

    @GET
    suspend fun getRoverPhotos(
        @Url url: String,
        @Query("earth_date") earth_date : String
    ): MarsRoverPhotosApiResponse?
}