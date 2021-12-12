package com.akhilasdeveloper.marsroverphotos.data

import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb

data class DatePreviewData(
    val roverName: String,
    val currentDate: Long,
    val photos:List<MarsRoverPhotoDb>
)