package com.akhilasdeveloper.marsroverphotos.db.table.photo

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "mars_rover_photo_liked_table")
data class MarsRoverPhotoLikedTable(
    @PrimaryKey
    @SerializedName("id") @Expose var id: Long,
    @SerializedName("rover_id") @Expose val rover_id: Int
)