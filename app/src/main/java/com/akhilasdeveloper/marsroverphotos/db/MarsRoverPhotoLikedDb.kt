package com.akhilasdeveloper.marsroverphotos.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akhilasdeveloper.marsroverphotos.api.Camera
import com.akhilasdeveloper.marsroverphotos.api.Rover
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

@Entity(tableName = "mars_rover_photo_liked_table")
data class MarsRoverPhotoLikedDb(
    @PrimaryKey
    @SerializedName("id") @Expose var id: Int,
    @SerializedName("rover_id") @Expose val rover_id: Int
)