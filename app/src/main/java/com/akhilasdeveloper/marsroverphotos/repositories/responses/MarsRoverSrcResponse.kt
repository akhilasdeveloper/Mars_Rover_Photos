package com.akhilasdeveloper.marsroverphotos.repositories.responses

import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverSrcDb

class MarsRoverSrcResponse(
    var error: String? = null,
    var isLoading: Boolean? = null,
    var data: List<RoverMaster>? = null
)