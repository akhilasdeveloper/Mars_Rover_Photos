package com.akhilasdeveloper.marsroverphotos.ui.fragments.home

import androidx.recyclerview.selection.ItemKeyProvider


class MarsPhotoKeyProvider(private val marsRoverPhotoAdapter: MarsRoverPhotoAdapter) : ItemKeyProvider<Long>(SCOPE_CACHED)
{
    override fun getKey(position: Int): Long? =
        marsRoverPhotoAdapter.snapshot()[position]?.photo_id
    override fun getPosition(key: Long): Int =
        marsRoverPhotoAdapter.snapshot().indexOfFirst {it?.photo_id == key}
}