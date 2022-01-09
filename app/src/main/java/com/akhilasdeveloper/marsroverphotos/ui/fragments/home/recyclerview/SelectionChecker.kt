package com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview

import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable

interface SelectionChecker {

    fun isSelected(marsRoverPhotoTable: MarsRoverPhotoTable): Boolean
    fun isSelection(marsRoverPhotoTable: MarsRoverPhotoTable): Boolean

}