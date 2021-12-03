package com.akhilasdeveloper.marsroverphotos

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.akhilasdeveloper.marsroverphotos.Constants.MILLIS_IN_A_SOL
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

fun Int.simplify() = when {
    abs(this / 1000000) > 1 -> {
        (this / 1000000).toString() + "M"
    }
    abs(this / 1000) > 1 -> {
        (this / 1000).toString() + "K"
    }
    else -> {
        this.toString()
    }
}

fun Fragment.showShortToast(message: String){
    Toast.makeText(this.requireContext(),message,Toast.LENGTH_SHORT).show()
}

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

    fun formatMillis(millis: Long): String {
        val pattern = "yyyy-MM-dd"
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(Date(millis))
    }

    fun formatDateToMillis(date: String): Long? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = dateFormat.parse(date)
        return parsedDate?.time
    }

    fun calculateDays(landingDate: Long, currentDate: Long): Long {
        return (currentDate - landingDate) / MILLIS_IN_A_SOL
    }

    fun calculateDaysEarthDate(sol: Long, minDate: Long): Long {
        return minDate + (sol * MILLIS_IN_A_SOL)
    }

}
