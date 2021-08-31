package com.akhilasdeveloper.marsroverphotos.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akhilasdeveloper.marsroverphotos.api.Camera
import com.akhilasdeveloper.marsroverphotos.api.Rover
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "mars_rover_source_table")
data class MarsRoverSrcDb(
    @SerializedName("roverDescription") @Expose val roverDescription: String,
    @SerializedName("roverImage") @Expose val roverImage: String,
    @PrimaryKey(autoGenerate = false)
    @SerializedName("roverName")
    @Expose val roverName: String,
    @SerializedName("roverName")
    @Expose val addedDate: Long
)