package com.akhilasdeveloper.marsroverphotos.ui.fragments

import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb

interface RecyclerClickListener {
    fun onItemSelected(marsRoverPhoto: MarsRoverPhotoDb, position: Int)
}