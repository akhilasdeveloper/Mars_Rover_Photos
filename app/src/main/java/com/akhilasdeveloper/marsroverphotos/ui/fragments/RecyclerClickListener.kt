package com.akhilasdeveloper.marsroverphotos.ui.fragments

import com.akhilasdeveloper.marsroverphotos.data.RoverPhotoViewItem
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoItemBinding
import com.akhilasdeveloper.marsroverphotos.databinding.ViewPagerItemBinding
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb

interface RecyclerClickListener {
    fun onItemSelected(marsRoverPhoto: RoverPhotoViewItem, position: Int)
}