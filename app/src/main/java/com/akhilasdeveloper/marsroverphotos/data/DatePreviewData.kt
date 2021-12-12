package com.akhilasdeveloper.marsroverphotos.data

import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import kotlinx.coroutines.flow.Flow

data class DatePreviewData(
    val roverName: String,
    val currentDate: Long,
    val photos: Flow<List<MarsRoverPhotoTable>>
)