package com.akhilasdeveloper.marsroverphotos.data

import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.api.Photo
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb

data class RoverPhotoViewItem(var date: String? = null,
                              var photo: MarsRoverPhotoDb? = null)