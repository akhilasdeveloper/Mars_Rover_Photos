package com.akhilasdeveloper.marsroverphotos.api

import retrofit2.http.GET
import retrofit2.http.Query

interface MarsRoverPhotosService {
    @GET("photos")
    suspend fun getMarsRoverPhotos(
        @Query("sol") sol : String,
        @Query("api_key") api_key : String
    ): MarsRoverPhotosApiResponse?
}