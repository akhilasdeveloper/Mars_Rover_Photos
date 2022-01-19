package com.akhilasdeveloper.marsroverphotos.utilities

object Constants {

    const val URL = "https://mars-photos.herokuapp.com/api/v1/"
    const val URL_PHOTO = URL + "rovers/"
    const val URL_MANIFEST = URL + "manifests/"
    const val URL_DATA = "https://mars-rover-photos-58b3f.firebaseapp.com/"
    const val MARS_ROVER_PHOTOS_PAGE_SIZE = 4
    const val MARS_ROVER_DATABASE_NAME = "MARS_ROVER_DATABASE"
    const val MILLIS_IN_A_DAY = (1000 * 60 * 60 * 24).toLong()
    const val MILLIS_IN_A_SOL = (1000 * 60 * 60 * 24).toLong() + (1000 * 60 * 39) + 35244
    const val GALLERY_SPAN = 3
    const val DATE_FORMAT = "yyyy-MM-dd"
    const val DISPLAY_DATE_FORMAT = "dd MMMM yyyy"
    const val FILE_DATE_FORMAT = "yyyy_MM_dd"
    const val SCROLL_DIRECTION_UP = -1
    const val SCROLL_DIRECTION_DOWN = 1
    const val ERROR_NO_INTERNET = "No Internet! Offline mode"
    const val ERROR_NETWORK_TIMEOUT = "Network timeout"
    const val ADD_ITEM_TYPE = "ADD_ITEM_TYPE"
    const val NETWORK_TIME_OUT = (10 * 60 * 1000).toLong()
    const val AD_ENABLED = false
    const val CACHE_IMAGE_EXTENSION = ".png"
    const val DATASTORE_LIKES_SYNC = "DATASTORE_LIKES_SYNC"
    const val DATASTORE_TRUE = "DATASTORE_TRUE"
    const val DATASTORE_FALSE = "DATASTORE_FALSE"
    const val FIREBASE_NODE_PHOTO_IDS = "photo_ids"
    const val FIREBASE_NODE_USER_IDS = "user_ids"
    const val FIREBASE_NODE_PHOTOS = "photos"
    const val FIREBASE_URL = "https://mars-rover-photos-58b3f-default-rtdb.asia-southeast1.firebasedatabase.app"
}