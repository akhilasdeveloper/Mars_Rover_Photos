package com.akhilasdeveloper.marsroverphotos.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akhilasdeveloper.marsroverphotos.api.Camera
import com.akhilasdeveloper.marsroverphotos.api.Rover
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

@Entity(tableName = "mars_rover_photo_table")
data class MarsRoverPhotoDb(
    @SerializedName("is_placeholder") @Expose val is_placeholder: Boolean,
    @SerializedName("earth_date") @Expose val earth_date: String,
    @SerializedName("img_src") @Expose val img_src: String,
    @SerializedName("sol") @Expose val sol: Int,
    @SerializedName("camera_full_name") @Expose val camera_full_name: String,
    @SerializedName("camera_name") @Expose val camera_name: String,
    @SerializedName("rover_id") @Expose val rover_id: Int,
    @SerializedName("rover_landing_date") @Expose val rover_landing_date: String,
    @SerializedName("rover_launch_date") @Expose val rover_launch_date: String,
    @SerializedName("rover_name") @Expose val rover_name: String,
    @SerializedName("rover_status") @Expose val rover_status: String,
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id") @Expose var id: Long? = null
)