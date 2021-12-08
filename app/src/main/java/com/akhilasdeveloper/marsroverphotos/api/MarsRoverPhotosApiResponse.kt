package com.akhilasdeveloper.marsroverphotos.api

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class MarsRoverPhotosApiResponse(
    @SerializedName("photos") @Expose val photos: List<Photo>
)

data class Photo(
    @SerializedName("camera") @Expose val camera: Camera,
    @SerializedName("earth_date") @Expose val earth_date: String,
    @SerializedName("id") @Expose val id: Long,
    @SerializedName("img_src") @Expose val img_src: String,
    @SerializedName("rover") @Expose val rover: Rover,
    @SerializedName("sol") @Expose val sol: Int
)

data class Camera(
    @SerializedName("full_name") @Expose val full_name: String,
    @SerializedName("id") @Expose val id: Int,
    @SerializedName("name") @Expose val name: String,
    @SerializedName("rover_id") @Expose val rover_id: Int
)

data class Rover(
    @SerializedName("id") @Expose val id: Int,
    @SerializedName("landing_date") @Expose val landing_date: String,
    @SerializedName("launch_date") @Expose val launch_date: String,
    @SerializedName("name") @Expose val name: String,
    @SerializedName("status") @Expose val status: String
)