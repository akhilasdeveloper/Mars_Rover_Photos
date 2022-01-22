package com.akhilasdeveloper.marsroverphotos.data

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


data class RoverData(
    @SerializedName("photo_manifest") @Expose val roverSrc: List<RoverSrc>
)

data class RoverSrc(
    @SerializedName("description") @Expose val description: String,
    @SerializedName("image") @Expose val image: String,
    @SerializedName("name") @Expose val name: String,
    @SerializedName("id") @Expose val id: Int,
    @SerializedName("landing_date") @Expose val landing_date: String,
    @SerializedName("launch_date") @Expose val launch_date: String,
    @SerializedName("max_date") @Expose val max_date: String,
    @SerializedName("max_sol") @Expose val max_sol: Int,
    @SerializedName("status") @Expose val status: String,
    @SerializedName("total_photos") @Expose val total_photos: Int
)