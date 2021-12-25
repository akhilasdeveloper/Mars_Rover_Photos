package com.akhilasdeveloper.marsroverphotos.ui.fragments.home

import com.akhilasdeveloper.marsroverphotos.databinding.PhotoDateItemBinding
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoItemBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable

interface RecyclerClickListener {
    fun onItemSelected(marsRoverPhoto: MarsRoverPhotoTable, position: Int)
    fun onItemLongClick(marsRoverPhoto: MarsRoverPhotoTable, position: Int): Boolean
}