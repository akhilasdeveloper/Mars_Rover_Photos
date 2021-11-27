package com.akhilasdeveloper.marsroverphotos.ui.fragments

import com.akhilasdeveloper.marsroverphotos.databinding.ViewPagerItemBinding
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb

interface PagerClickListener {
    fun onClick()
    fun loaded(binding: ViewPagerItemBinding, photo: MarsRoverPhotoDb, position: Int)
}