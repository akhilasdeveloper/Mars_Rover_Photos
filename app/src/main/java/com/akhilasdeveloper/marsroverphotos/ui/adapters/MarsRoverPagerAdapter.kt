package com.akhilasdeveloper.marsroverphotos.ui.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.ViewPagerItemBinding
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.ui.fragments.PagerClickListener

class MarsRoverPagerAdapter(private val interaction: PagerClickListener? = null) :
    PagingDataAdapter<MarsRoverPhotoDb, MarsRoverPagerAdapter.PhotoViewHolder>(PHOTO_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val bindingPhoto =
            ViewPagerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(bindingPhoto, interaction)
    }

    class PhotoViewHolder(private val binding: ViewPagerItemBinding, private val interaction: PagerClickListener?) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindPhoto(photo: MarsRoverPhotoDb, position: Int) {
            binding.apply {
                root.animation = AnimationUtils.loadAnimation(binding.root.context, R.anim.fade_in)
                photo.let {
                    viewPageImage.showImage(Uri.parse(it.img_src))
                }
            }
            binding.viewPageImage.setOnClickListener {
                interaction?.onClick()
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

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val currentItem = getItem(position)

        currentItem?.let {
            holder.bindPhoto(currentItem, position)
        }
    }
}