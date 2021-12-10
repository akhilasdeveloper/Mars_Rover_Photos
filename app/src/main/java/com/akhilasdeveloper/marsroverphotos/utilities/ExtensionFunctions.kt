package com.akhilasdeveloper.marsroverphotos.utilities

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.DATE_FORMAT
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.SCROLL_DIRECTION_DOWN
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.SCROLL_DIRECTION_UP
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
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

        fun evaluate(){
            val layoutManager = layoutManager
            if (layoutManager is LinearLayoutManager) {
                firstItemPosition(
                    layoutManager.findFirstVisibleItemPosition())
            } else {
                firstItemPosition(0)
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
            removeOnLayoutChangeListener(this)
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
        }
    })
}

fun Long.formatMillisToDate(): String = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date(this))
fun String.formatDateToMillis(): Long? = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).parse(this)?.time
fun String.nextDate(): String = (this.formatDateToMillis()!!.nextDate()).formatMillisToDate()
fun Long.nextDate(): Long = (this + Constants.MILLIS_IN_A_DAY)
fun String.prevDate(): String = (this.formatDateToMillis()!!.prevDate()).formatMillisToDate()
fun Long.prevDate(): Long = (this - Constants.MILLIS_IN_A_DAY)

fun String.downloadImage(context: Context, callback: (Bitmap?) -> (Unit)) {
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

