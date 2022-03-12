package com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.viewholders


import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoItemBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.RecyclerClickListener
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class PhotoViewHolder(
    private val binding: PhotoItemBinding,
    private val interaction: RecyclerClickListener?,
    private val requestManager: RequestManager
) :
    RecyclerView.ViewHolder(binding.root) {

    private var positionSel = 0
    var photo: MarsRoverPhotoTable? = null

    fun bindPhoto(photo: MarsRoverPhotoTable, position: Int) {
        positionSel = position
        this.photo = photo
        binding.apply {
            photo.let {
                requestManager
                    .load(it.img_src)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageDescription)
            }
        }

        binding.root.setOnClickListener {
            interaction?.onItemSelected(photo, absoluteAdapterPosition)
        }

        binding.root.setOnLongClickListener {
            interaction?.onItemLongClick(photo, absoluteAdapterPosition) ?: false
        }

    }


}