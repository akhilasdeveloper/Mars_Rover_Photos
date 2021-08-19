package com.akhilasdeveloper.marsroverphotos.data

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class RoverManifest(
    @SerializedName("photo_manifest") @Expose val photo_manifest: PhotoManifest
)

data class PhotoManifest(
    @SerializedName("landing_date") @Expose val landing_date: String,
    @SerializedName("launch_date") @Expose val launch_date: String,
    @SerializedName("max_date") @Expose val max_date: String,
    @SerializedName("max_sol") @Expose val max_sol: Int,
    @SerializedName("name") @Expose val name: String,
    @SerializedName("photos") @Expose val photos: List<Photo>,
    @SerializedName("status") @Expose val status: String,
    @SerializedName("total_photos") @Expose val total_photos: Int
)

data class Photo(
    @SerializedName("cameras") @Expose val cameras: List<String>,
    @SerializedName("earth_date") @Expose val earth_date: String,
    @SerializedName("sol") @Expose val sol: Int,
    @SerializedName("total_photos") @Expose val total_photos: Int
)