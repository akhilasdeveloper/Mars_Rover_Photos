package com.akhilasdeveloper.marsroverphotos.data

import androidx.paging.Pager
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb

data class DateItem(
    var date: String,
    var data: Pager<Int, MarsRoverPhotoDb>
) {
}