package com.akhilasdeveloper.marsroverphotos.api

import androidx.lifecycle.LiveData
import com.akhilasdeveloper.marsroverphotos.Constants
import com.akhilasdeveloper.marsroverphotos.data.RoverData
import com.akhilasdeveloper.marsroverphotos.data.RoverManifest
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

    @GET(Constants.URL_DATA + "data.json")
    suspend fun getRoverData(): RoverData?

    @GET
    suspend fun getRoverManifest(
        @Url url:String
    ): RoverManifest?

}