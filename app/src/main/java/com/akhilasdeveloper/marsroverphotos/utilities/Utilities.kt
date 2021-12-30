package com.akhilasdeveloper.marsroverphotos.utilities

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.MILLIS_IN_A_SOL
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject


class Utilities @Inject constructor(
    var context: Context
) {

    fun isConnectedToTheInternet(): Boolean {
        try {
            var result = false
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkCapabilities = connectivityManager.activeNetwork ?: return false
                val actNw =
                    connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
                result = when {
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } else {
                connectivityManager.run {
                    connectivityManager.activeNetworkInfo?.run {
                        result = when (type) {
                            ConnectivityManager.TYPE_WIFI -> true
                            ConnectivityManager.TYPE_MOBILE -> true
                            ConnectivityManager.TYPE_ETHERNET -> true
                            else -> false
                        }

                    }
                }
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
                val filename = "$displayName.png"
                file = File(folder.path, filename)
                fos1 = FileOutputStream(file)

                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos1)
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

    fun calculateDays(landingDate: Long, currentDate: Long?): Long? =
        currentDate?.let {
            ((currentDate - landingDate) / MILLIS_IN_A_SOL) + 1
        }

    fun calculateDaysEarthDate(sol: Long, minDate: Long): Long {
        return minDate + (sol * MILLIS_IN_A_SOL)
    }

}
