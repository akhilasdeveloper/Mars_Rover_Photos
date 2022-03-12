package com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.LayoutSavedItemSelectedBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.RecyclerClickListener
import com.akhilasdeveloper.marsroverphotos.utilities.formatMillisToDisplayDate
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class PhotoSavedSelectedViewHolder(
    private val binding: LayoutSavedItemSelectedBinding,
    private val interaction: RecyclerClickListener?,
    private val requestManager: RequestManager
) :
    RecyclerView.ViewHolder(binding.root) {

    fun bindPhoto(photo: MarsRoverPhotoTable, position: Int) {
        binding.apply {
            photo.let {
                imageDescription.setImageResource(R.drawable.imageview_placeholder)

                requestManager
                    .load(it.img_src)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageDescription)

                sol.text = it.sol.toString()
                date.text = it.earth_date.formatMillisToDisplayDate()
            }
        }
        binding.root.setOnClickListener {
            interaction?.onItemSelected(photo, absoluteAdapterPosition)
        }
    }

}