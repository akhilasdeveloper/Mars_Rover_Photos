package com.akhilasdeveloper.marsroverphotos

object Constants {

    const val URL = "https://mars-photos.herokuapp.com/api/v1/"
    const val URL_PHOTO = URL + "rovers/"
    const val URL_MANIFEST = URL + "manifests/"
    const val URL_DATA = "https://mars-rover-photos-58b3f.web.app/"
    const val MARS_ROVER_PHOTOS_PAGE_SIZE = 25
    const val MARS_ROVER_DATABASE_NAME = "MARS_ROVER_DATABASE"
    const val MILLIS_IN_A_DAY = (1000 * 60 * 60 * 24).toLong()
    const val MILLIS_IN_A_SOL = (1000 * 60 * 60 * 24).toLong() + (1000 * 60 * 39) + 35244
    const val GALLERY_SPAN = 2

    const val ERROR_NO_INTERNET = "No Internet! Offline mode"
}