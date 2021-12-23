package com.akhilasdeveloper.marsroverphotos.ui.fragments.home

import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable

interface SelectionChecker {

    fun isSelected(marsRoverPhotoTable: MarsRoverPhotoTable): Boolean

}