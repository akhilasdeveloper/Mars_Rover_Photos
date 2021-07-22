package com.akhilasdeveloper.marsroverphotos.api

import com.akhilasdeveloper.marsroverphotos.Constants
import retrofit2.http.GET
import retrofit2.http.Query

interface MarsRoverPhotosService {

    @GET("photos")
    suspend fun getMarsRoverPhotos(
        @Query("earth_date") earth_date : String,
        @Query("api_key") api_key : String
    ): MarsRoverPhotosApiResponse?
}