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
    @SerializedName("id") @Expose val id: Int
)