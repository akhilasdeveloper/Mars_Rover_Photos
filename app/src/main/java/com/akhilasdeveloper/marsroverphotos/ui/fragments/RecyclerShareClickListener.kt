package com.akhilasdeveloper.marsroverphotos.ui.fragments

import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable

interface RecyclerShareClickListener {
    fun onItemDeleteClicked(marsRoverPhotoTable: MarsRoverPhotoTable, position: Int)
}