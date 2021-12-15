package com.akhilasdeveloper.marsroverphotos.utilities

object Constants {

    const val URL = "https://mars-photos.herokuapp.com/api/v1/"
    const val URL_PHOTO = URL + "rovers/"
    const val URL_MANIFEST = URL + "manifests/"
    const val URL_DATA = "https://mars-rover-photos-58b3f.firebaseapp.com/"
    const val MARS_ROVER_PHOTOS_PAGE_SIZE = 4
    const val MARS_ROVER_PHOTOS_DISPLAY_PAGE_SIZE = 5
    const val MARS_ROVER_DATABASE_NAME = "MARS_ROVER_DATABASE"
    const val MILLIS_IN_A_DAY = (1000 * 60 * 60 * 24).toLong()
    const val MILLIS_IN_A_SOL = (1000 * 60 * 60 * 24).toLong() + (1000 * 60 * 39) + 35244
    const val GALLERY_SPAN = 3
    const val STARTING_PAGE_INDEX = 1
    const val PAGE_VALIDATED = "PAGE_VALIDATED"
    const val PAGE_NOT_VALIDATED = "PAGE_NOT_VALIDATED"
    const val PLACEHOLDER_STRING = "PLACEHOLDER_STRING"
    const val DATE_FORMAT = "yyyy-MM-dd"
    const val DISPLAY_DATE_FORMAT = "dd MMMM yyyy"
    const val FILE_DATE_FORMAT = "yyyy_MM_dd"
    const val SCROLL_DIRECTION_UP = -1
    const val SCROLL_DIRECTION_DOWN = 1
    const val PLACEHOLDER_ID = (-999).toLong()

    const val ERROR_NO_INTERNET = "No Internet! Offline mode"
}