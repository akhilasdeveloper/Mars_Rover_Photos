package com.akhilasdeveloper.marsroverphotos.ui.fragments

import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable

interface RecyclerClickListener {
    fun onItemSelected(marsRoverPhoto: MarsRoverPhotoTable, position: Int)
}