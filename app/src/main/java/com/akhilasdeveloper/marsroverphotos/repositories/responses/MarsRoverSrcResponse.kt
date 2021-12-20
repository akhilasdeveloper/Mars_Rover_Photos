package com.akhilasdeveloper.marsroverphotos.repositories.responses

import com.akhilasdeveloper.marsroverphotos.data.RoverMaster

class MarsRoverSrcResponse(
    var error: String? = null,
    var message: String? = null,
    var isLoading: Boolean = false,
    var data: List<RoverMaster>? = null
)