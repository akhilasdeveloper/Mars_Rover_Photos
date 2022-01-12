package com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview

import android.view.View
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable

interface RecyclerClickListener {
    fun onItemSelected(marsRoverPhoto: MarsRoverPhotoTable, position: Int)
    fun onItemLongClick(
        marsRoverPhoto: MarsRoverPhotoTable,
        position: Int,
        view: View,
        x: Float,
        y: Float
    ): Boolean
}