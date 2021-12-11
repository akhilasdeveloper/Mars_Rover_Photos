package com.akhilasdeveloper.marsroverphotos.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.databinding.ViewPagerItemBinding
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.ui.fragments.PagerClickListener
import com.akhilasdeveloper.marsroverphotos.utilities.downloadImageAsUri
import com.davemorrissey.labs.subscaleview.ImageSource

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
                photo.let {
//                    viewPageImage.showImage(Uri.parse(it.img_src))
                    /*Glide.with(itemView)
                        .load(it.img_src)
                        .centerInside()
                        .into(viewPageImage)*/
                    it.img_src.downloadImageAsUri(root.context){ resource->
                        resource?.let {
                            viewPageImage.setImage(ImageSource.uri(resource))
                        }
                    }
                    viewPageImage.setMinimumDpi(60)
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