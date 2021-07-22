package com.akhilasdeveloper.marsroverphotos.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.databinding.ViewPagerItemBinding
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.ui.fragments.RecyclerClickListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class MarsRoverPagerAdapter(private val interaction: RecyclerClickListener? = null) :
    PagingDataAdapter<MarsRoverPhotoDb, MarsRoverPagerAdapter.PhotoViewHolder>(PHOTO_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val bindingPhoto =
            ViewPagerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(bindingPhoto, interaction)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val currentItem = getItem(position)

        currentItem?.let {
            holder.bindPhoto(currentItem, position)
        }
    }

    class PhotoViewHolder(private val binding: ViewPagerItemBinding, private val interaction: RecyclerClickListener?) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindPhoto(photo: MarsRoverPhotoDb, position: Int) {
            binding.apply {
                photo.let {
                    Glide.with(itemView)
                        .load(it.img_src)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(viewPageImage)
                    /*cameraName.text = "${it.camera_name} Camera"
                    roverName.text = "${it.rover_name} Rover"*/
                }
            }

            binding.root.setOnClickListener {
                interaction?.onItemSelected(photo, position)
            }
        }

    }

    companion object {
        private val PHOTO_COMPARATOR = object : DiffUtil.ItemCallback<MarsRoverPhotoDb>() {
            override fun areItemsTheSame(oldItem: MarsRoverPhotoDb, newItem: MarsRoverPhotoDb) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MarsRoverPhotoDb, newItem: MarsRoverPhotoDb) =
                oldItem == newItem

        }
    }
}