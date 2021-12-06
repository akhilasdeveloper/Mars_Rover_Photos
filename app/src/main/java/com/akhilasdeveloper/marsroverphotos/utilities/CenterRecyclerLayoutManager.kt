package com.akhilasdeveloper.marsroverphotos.utilities

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.R
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CenterRecyclerLayoutManager(context: Context, span: Int): GridLayoutManager(context,span) {
    override fun scrollToPosition(position: Int) {
        super.scrollToPosition(position)

    }
}