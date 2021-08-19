package com.akhilasdeveloper.marsroverphotos.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akhilasdeveloper.marsroverphotos.api.Camera
import com.akhilasdeveloper.marsroverphotos.api.Rover
import com.akhilasdeveloper.marsroverphotos.data.Photo
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "mars_rover_manifest_table")
data class MarsRoverManifestDb(
    @SerializedName("landing_date") @Expose val landing_date: String,
    @SerializedName("launch_date") @Expose val launch_date: String,
    @SerializedName("max_date") @Expose val max_date: String,
    @SerializedName("max_sol") @Expose val max_sol: Int,
    @SerializedName("status") @Expose val status: String,
    @SerializedName("total_photos") @Expose val total_photos: Int,
    @PrimaryKey(autoGenerate = false)
    @SerializedName("name") @Expose val name: String
)