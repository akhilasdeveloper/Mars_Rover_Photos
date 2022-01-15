package com.akhilasdeveloper.marsroverphotos.utilities

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.DATE_FORMAT
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.DISPLAY_DATE_FORMAT
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.FILE_DATE_FORMAT
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.SCROLL_DIRECTION_DOWN
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.SCROLL_DIRECTION_UP
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

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

fun Context.showShortToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.isDarkThemeOn(): Boolean {
    return resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

fun RecyclerView.observeFirstItemPosition(firstItemPosition: (position: Int) -> Unit) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            evaluate()
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            evaluate()
        }

        fun evaluate() {
            val layoutManager = layoutManager
            if (layoutManager is LinearLayoutManager) {
                firstItemPosition(
                    layoutManager.findFirstVisibleItemPosition()
                )
            } else {
                firstItemPosition(0)
            }
        }
    })
}

fun RecyclerView.fastScrollListener(fastScrolled: () -> Unit,extraFastScrolled: () -> Unit) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (abs(dy) > 30)
                fastScrolled()
            if (abs(dy) > 500)
                extraFastScrolled()
        }
    })
}

fun RecyclerView.isIdle(isIdle: (isIdle: Boolean) -> Unit) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            isIdle(newState == RecyclerView.SCROLL_STATE_IDLE)
        }
    })
}

fun RecyclerView.observeVisibleItemPositions(visibleItemPosition: (firstVisibleItemPosition: Int, secondVisibleItemPosition: Int) -> Unit) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                val layoutManager = layoutManager
                if (layoutManager is LinearLayoutManager) {
                    visibleItemPosition(
                        layoutManager.findFirstVisibleItemPosition(),
                        layoutManager.findLastVisibleItemPosition()
                    )
                } else {
                    visibleItemPosition(-1, -1)
                }
            }
        }
    })
}


fun RecyclerView.scrollToCenter(position: Int) {
    addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            v: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            val viewAtPosition = layoutManager?.findViewByPosition(position)
            if (viewAtPosition == null || layoutManager?.isViewPartiallyVisible(
                    viewAtPosition,
                    false,
                    true
                ) == true
            ) {

                this@scrollToCenter.post {
                    layoutManager?.scrollToPosition(position)
/*                    layoutManager?.startSmoothScroll(CenterSmoothScroller(context = context).apply {
                        targetPosition = position
                    })*/
                }
            }
            removeOnLayoutChangeListener(this)
        }
    })
}

fun Long.formatMillisToDisplayDate(): String =
    SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault()).format(Date(this))

fun Long.formatMillisToFileDate(): String =
    SimpleDateFormat(FILE_DATE_FORMAT, Locale.getDefault()).format(Date(this))

fun Long.formatMillisToDate(): String =
    SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date(this))

fun String.formatDateToMillis(): Long? =
    SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).parse(this)?.time

fun Long.nextDate(): Long =
    (this + Constants.MILLIS_IN_A_DAY).formatMillisToDate().formatDateToMillis()!!

fun Long.prevDate(): Long =
    (this - Constants.MILLIS_IN_A_DAY).formatMillisToDate().formatDateToMillis()!!

fun String.downloadImageAsBitmap(context: Context, callback: (Bitmap?) -> (Unit)) {
    Glide.with(context).asBitmap().load(this)
        .into(object : CustomTarget<Bitmap?>() {
            override fun onResourceReady(
                resource: Bitmap,
                transition: Transition<in Bitmap?>?
            ) {
                callback(resource)
            }

            override fun onLoadCleared(placeholder: Drawable?) {}
        })
}

fun String.downloadImageAsBitmap2(requestManager: RequestManager): Bitmap? = requestManager.asBitmap().load(this).submit().get()

fun String.downloadImageAsUri(requestManager : RequestManager, callback: (Uri?) -> (Unit)) {
    CoroutineScope(Dispatchers.IO).launch {
        val data = requestManager.asFile().load(this@downloadImageAsUri).submit().get()
        withContext(Dispatchers.Main) {
            callback(data.toUri())
        }
    }
}

fun String.downloadImageAsFile(context: Context, callback: (File?) -> (Unit)) {
    CoroutineScope(Dispatchers.IO).launch {
        val data = Glide.with(context).asFile().load(this@downloadImageAsFile).submit().get()
        withContext(Dispatchers.Main) {
            callback(data)
        }
    }
}

inline fun <T> sdk29andUp(onSdk29: () -> T): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        onSdk29()
    else null
}

inline fun sdkAndUp(version: Int, onSdkAndAbove: () -> Unit, belowSdk: () -> Unit) {
    if (Build.VERSION.SDK_INT >= version)
        onSdkAndAbove()
    else belowSdk()
}

fun View.updateMarginAndHeight(
    top: Int? = null,
    bottom: Int? = null,
    start: Int? = null,
    end: Int? = null,
    height: Int? = null,
    width: Int? = null
) {
    (layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
        start?.let { this.marginStart = it }
        end?.let { this.marginEnd = it }
        top?.let { this.topMargin = it }
        bottom?.let { this.bottomMargin = it }
        height?.let { this.height = it }
        width?.let { this.width = it }
    }
}

fun Fragment.toDpi(int: Int): Int = (this.resources.displayMetrics.density * int).toInt()
val Fragment.displayHeightPx: Int get() = this.resources.displayMetrics.heightPixels

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")