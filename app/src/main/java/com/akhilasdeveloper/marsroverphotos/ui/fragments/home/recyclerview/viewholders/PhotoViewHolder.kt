package com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.viewholders

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.R
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

    @SuppressLint("ClickableViewAccessibility")
    fun bindPhoto(photo: MarsRoverPhotoTable, position: Int) {
        positionSel = position
        this.photo = photo
        binding.apply {
            photo.let {
                imageDescription.setImageResource(R.drawable.imageview_placeholder)

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

        var x = 0f
        var y = 0f

        binding.root.setOnTouchListener { v, event ->
            // save the X,Y coordinates
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                x = event.rawX
                y = event.rawY
            }
            return@setOnTouchListener false
        }

        binding.root.setOnLongClickListener {
            interaction?.onItemLongClick(photo, absoluteAdapterPosition, it,x,y) ?: false
        }

    }


}