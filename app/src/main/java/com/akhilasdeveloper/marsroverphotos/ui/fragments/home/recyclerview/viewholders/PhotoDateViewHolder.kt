package com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoDateItemBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.RecyclerClickListener
import com.akhilasdeveloper.marsroverphotos.utilities.formatMillisToDisplayDate
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class PhotoDateViewHolder(
    private val binding: PhotoDateItemBinding,
    private val interaction: RecyclerClickListener?,
    private val requestManager: RequestManager
) :
    RecyclerView.ViewHolder(binding.root) {

    var positionSel = 0
    var photo: MarsRoverPhotoTable? = null

    fun bindPhoto(photo: MarsRoverPhotoTable, position: Int) {
        positionSel = position
        this.photo = photo
        binding.apply {
            photo.let {
                imageDescription.transitionName = it.photo_id.toString()
                requestManager
                    .load(it.img_src)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageDescription)


                binding.count.text = it.total_count.toString() + " photos"
                binding.sol.text = it.sol.toString()
                binding.date.text = it.earth_date.formatMillisToDisplayDate()
            }
        }

        binding.root.setOnClickListener {
            interaction?.onItemSelected(photo, absoluteAdapterPosition)
        }

        binding.root.setOnLongClickListener {
            interaction?.onItemLongClick(photo, absoluteAdapterPosition, binding.imageDescription) ?: false
        }
    }

}