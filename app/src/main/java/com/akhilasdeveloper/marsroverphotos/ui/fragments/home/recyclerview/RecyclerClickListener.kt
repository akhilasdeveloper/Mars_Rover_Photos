package com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview

import android.widget.ImageView
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable

interface RecyclerClickListener {
    fun onItemSelected(marsRoverPhoto: MarsRoverPhotoTable, position: Int)
    fun onItemLongClick(marsRoverPhoto: MarsRoverPhotoTable, position: Int): Boolean
    fun onItemLongClick(marsRoverPhoto: MarsRoverPhotoTable, position: Int, imageView: ImageView): Boolean
}