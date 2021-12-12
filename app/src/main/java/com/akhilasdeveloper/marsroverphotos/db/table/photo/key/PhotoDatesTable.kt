package com.akhilasdeveloper.marsroverphotos.db.table.photo.key

import androidx.room.Entity


@Entity(tableName = "photo_dates", primaryKeys = ["roverName", "date"])
data class PhotoDatesTable(
    val roverName: String,
    val date: Long
)