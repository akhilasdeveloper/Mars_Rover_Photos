package com.akhilasdeveloper.marsroverphotos.ui.fragments.home

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView


class MarsPhotoItemLookup(private val recyclerView: RecyclerView,private val adapter: MarsRoverPhotoAdapter) :
    ItemDetailsLookup<Long>() {
    override fun getItemDetails(event: MotionEvent): ItemDetails<Long>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            val pos = recyclerView.getChildAdapterPosition(view)
            val data = adapter.snapshot()[pos]
            return if(data?.is_placeholder == true ) (recyclerView.getChildViewHolder(view) as MarsRoverPhotoAdapter.PhotoDateViewHolder).getItemDetails() else
                (recyclerView.getChildViewHolder(view) as MarsRoverPhotoAdapter.PhotoViewHolder).getItemDetails()
        }
        return null
    }
}