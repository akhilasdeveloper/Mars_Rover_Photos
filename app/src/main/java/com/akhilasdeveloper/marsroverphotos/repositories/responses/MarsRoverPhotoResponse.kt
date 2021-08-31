package com.akhilasdeveloper.marsroverphotos.repositories.responses

import androidx.paging.PagingData
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverSrcDb

class MarsRoverPhotoResponse(
    var error: String? = null,
    var isLoading: Boolean? = null,
    var data: PagingData<MarsRoverPhotoDb>? = null
)