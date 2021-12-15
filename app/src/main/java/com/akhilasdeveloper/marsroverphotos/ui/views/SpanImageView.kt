package com.akhilasdeveloper.marsroverphotos.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.akhilasdeveloper.marsroverphotos.utilities.Constants

class SpanImageView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : AppCompatImageView(context, attrs, defStyleAttr) {

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            widthMeasureSpec / Constants.GALLERY_SPAN
        )
    }
}