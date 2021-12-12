package com.akhilasdeveloper.marsroverphotos.db.table.photo.key

import androidx.room.Entity


@Entity(tableName = "display_photos", primaryKeys = ["roverName", "date", "photoID"])
data class DisplayPhotosTable(
    val roverName: String,
    val date: Long,
    val photoID : Int
)