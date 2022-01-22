package com.akhilasdeveloper.marsroverphotos.db.table.rover

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akhilasdeveloper.marsroverphotos.api.Camera
import com.akhilasdeveloper.marsroverphotos.api.Rover
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "mars_rover_source_table")
data class MarsRoverSrcTable(
    @SerializedName("roverDescription") @Expose val roverDescription: String,
    @SerializedName("roverImage") @Expose val roverImage: String,
    @PrimaryKey(autoGenerate = false) @SerializedName("roverName") @Expose val roverName: String,
    @SerializedName("addedDate") @Expose val addedDate: Long,
    @Expose val id: Int,
    @SerializedName("landing_date") @Expose val landing_date: String,
    @SerializedName("launch_date") @Expose val launch_date: String,
    @SerializedName("max_date") @Expose val max_date: String,
    @SerializedName("max_sol") @Expose val max_sol: Int,
    @SerializedName("status") @Expose val status: String,
    @SerializedName("total_photos") @Expose val total_photos: Int
)