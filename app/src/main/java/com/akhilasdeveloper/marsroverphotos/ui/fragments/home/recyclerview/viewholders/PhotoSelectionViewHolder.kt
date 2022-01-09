package com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoItemSelectedBinding
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoItemSelectionBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.RecyclerClickListener
import com.bumptech.glide.RequestManager

class PhotoSelectionViewHolder(
    private val binding: PhotoItemSelectionBinding,
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
                    .into(imageDescription)
            }
        }

        binding.root.setOnClickListener {
            interaction?.onItemSelected(photo, absoluteAdapterPosition)
        }

        /*binding.root.setOnLongClickListener {
            interaction?.onItemLongClick(photo, position, binding) ?: false
        }*/
    }

}