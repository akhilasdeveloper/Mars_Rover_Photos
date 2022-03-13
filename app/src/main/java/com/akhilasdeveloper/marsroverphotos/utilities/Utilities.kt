package com.akhilasdeveloper.marsroverphotos.utilities

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.DATASTORE_LIKES_SYNC
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.DATASTORE_FALSE
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.DATASTORE_LIKE_SYNC
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.DATASTORE_TRUE
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.MILLIS_IN_A_SOL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject


class Utilities @Inject constructor(
    var context: Context,
    var vibrator: Vibrator
) {

    fun isConnectedToTheInternet(): Boolean {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            val result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
            return result
        } catch (e: Exception) {
            Timber.d("isConnectedToTheInternet: ${e.message}")
        }
        return false
    }

    fun toImageURI(bitmap: Bitmap?, displayName: String): Uri? {
        bitmap?.let {
            var file: File? = null
            var fos1: FileOutputStream? = null
            var imageUri: Uri? = null
            try {
                val folder = File(
                    context.cacheDir.toString() + File.separator + "MarsRoverPhotos Temp Files"
                )
                if (!folder.exists()) {
                    folder.mkdir()
                }
                file = File(folder.path, displayName)

                if (!file.exists()) {
                    fos1 = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos1)
                }
                imageUri = FileProvider.getUriForFile(
                    context.applicationContext,
                    context.applicationContext.packageName.toString() + ".provider",
                    file
                )
            } catch (ex: java.lang.Exception) {
            } finally {
                try {
                    fos1?.close()
                } catch (e: IOException) {
                    Timber.d("Unable to close connection Utilities toImageURI : ${e.toString()}")
                }
            }
            return imageUri
        }
        return null
    }

    fun toImageUriFromName(displayName: String): Uri? {
        var file: File? = null
        var imageUri: Uri? = null
        try {
            val folder = File(getCacheFolder())
            if (!folder.exists()) {
                folder.mkdir()
            }
            file = File(folder.path, displayName)
            imageUri = FileProvider.getUriForFile(
                context.applicationContext,
                context.applicationContext.packageName.toString() + ".provider",
                file
            )

        } catch (ex: java.lang.Exception) {
        }
        return imageUri
    }

    fun isFileExistInCache(displayName: String) = File(getCacheFolder(), displayName).exists()

    private fun getCacheFolder() = context.cacheDir.toString() + File.separator + "MarsRoverPhotos Temp Files"

    fun calculateDays(landingDate: Long, currentDate: Long?): Long? =
        currentDate?.let {
            ((currentDate - landingDate) / MILLIS_IN_A_SOL) + 1
        }

    fun calculateDaysEarthDate(sol: Long, minDate: Long): Long {
        return minDate + (sol * MILLIS_IN_A_SOL)
    }

    fun vibrate(){
        sdkAndUp(Build.VERSION_CODES.O, onSdkAndAbove = {
            vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        }, belowSdk = {
            vibrator.vibrate(25)
        })
    }


    suspend fun isShowLikeConsent(): Boolean {
        val dataStoreKey = stringPreferencesKey(DATASTORE_LIKE_SYNC)
        val preferences = context.dataStore.data.first()
        return (preferences[dataStoreKey] ?: DATASTORE_TRUE) == DATASTORE_TRUE
    }

    suspend fun setHideLikesConsent() {
        val dataStoreKey = stringPreferencesKey(DATASTORE_LIKES_SYNC)
        context.dataStore.edit { settings ->
            settings[dataStoreKey] = DATASTORE_FALSE
        }
    }

    suspend fun isShowLikesConsent(): Boolean {
        val dataStoreKey = stringPreferencesKey(DATASTORE_LIKES_SYNC)
        val preferences = context.dataStore.data.first()
        return (preferences[dataStoreKey] ?: DATASTORE_TRUE) == DATASTORE_TRUE
    }

    fun getDisplayName(rover: MarsRoverPhotoTable) = "${rover.rover_name}_${rover.camera_name}_${rover.earth_date.formatMillisToFileDate()}_${rover.photo_id}${Constants.CACHE_IMAGE_EXTENSION}"

    fun getString(@StringRes id:Int) = context.getString(id)
    fun getString(@StringRes id:Int, vararg formatArgs: Any?) = context.getString(id,formatArgs)

    fun getDummyRoverMaster() = RoverMaster(
        landing_date = "",
        landing_date_in_millis = 0L,
        launch_date = "",
        launch_date_in_millis = 0L,
        max_sol = 0,
        max_date = "",
        max_date_in_millis = 0L,
        name = "",
        status = "",
        total_photos = 0,
        description = "",
        image = "",
        id = -999
    )

    fun deleteCache() {
        try {
            val dir: File = context.cacheDir
            CoroutineScope(Dispatchers.IO).launch {
                deleteDir(dir)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        return if (dir != null && dir.isDirectory) {
            val children: Array<String> = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
            dir.delete()
        } else if (dir != null && dir.isFile) {
            dir.delete()
        } else {
            false
        }
    }

}
