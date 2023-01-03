package com.akhilasdeveloper.marsroverphotos.utilities

object Constants {

    const val URL = "https://mars-photos.herokuapp.com/api/v1/"
    const val URL_PHOTO = URL + "rovers/"
    const val PHOTOS = "/photos"
    const val URL_DATA = "https://mars-rover-photos-58b3f.firebaseapp.com/"
    const val MARS_ROVER_PHOTOS_PAGE_SIZE = 4
    const val MARS_ROVER_DATABASE_NAME = "MARS_ROVER_DATABASE"
    const val MILLIS_IN_A_DAY = (1000 * 60 * 60 * 24).toLong()
    const val MILLIS_IN_A_SOL = (1000 * 60 * 60 * 24).toLong() + (1000 * 60 * 39) + 35244

    const val GALLERY_SPAN_LARGE = 6
    const val GALLERY_SPAN_X_LARGE = 6
    const val GALLERY_SPAN_SMALL = 3
    const val GALLERY_SPAN_NORMAL = 3
    const val GALLERY_SPAN_LANDSCAPE_LARGE = 6
    const val GALLERY_SPAN_LANDSCAPE_X_LARGE = 6
    const val GALLERY_SPAN_LANDSCAPE_SMALL = 4
    const val GALLERY_SPAN_LANDSCAPE_NORMAL = 6

    const val ROVER_SPAN = 1
    const val ROVER_SPAN_MULTI = 2

    const val DATE_FORMAT = "yyyy-MM-dd"
    const val DISPLAY_DATE_FORMAT = "dd MMMM yyyy"
    const val FILE_DATE_FORMAT = "yyyy_MM_dd"
    const val NETWORK_TIME_OUT = (60 * 60 * 1000).toLong()
    const val ABOUT_VIEW_ID = -11
    const val CACHE_IMAGE_EXTENSION = ".png"
    const val DATASTORE_LIKES_SYNC = "DATASTORE_LIKES_SYNC"
    const val DATASTORE_LIKE_SYNC = "DATASTORE_LIKE_SYNC"
    const val THEME = "THEME"
    const val DARK = "DARK"
    const val LIGHT = "LIGHT"
    const val SYSTEM = "SYSTEM"
    const val DATASTORE_TRUE = "DATASTORE_TRUE"
    const val DATASTORE_FALSE = "DATASTORE_FALSE"
    const val ROVER_STATUS_ACTIVE = "active"
    const val ROVER_STATUS_COMPLETE = "complete"

}