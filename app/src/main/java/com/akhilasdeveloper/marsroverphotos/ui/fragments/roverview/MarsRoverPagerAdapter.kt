package com.akhilasdeveloper.marsroverphotos.ui.fragments.roverview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.databinding.ViewPagerItemBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.utilities.downloadImageAsUri
import com.davemorrissey.labs.subscaleview.ImageSource

class MarsRoverPagerAdapter(private val interaction: PagerClickListener? = null) :
    PagingDataAdapter<MarsRoverPhotoTable, MarsRoverPagerAdapter.PhotoViewHolder>(PHOTO_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val bindingPhoto =
            ViewPagerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(bindingPhoto, interaction)
    }

    class PhotoViewHolder(private val binding: ViewPagerItemBinding, private val interaction: PagerClickListener?) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindPhoto(photo: MarsRoverPhotoTable, position: Int) {
            binding.apply {
                photo.let {
                    viewPageImage.transitionName = it.photo_id.toString()
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
        private val PHOTO_COMPARATOR = object : DiffUtil.ItemCallback<MarsRoverPhotoTable>() {
            override fun areItemsTheSame(oldItem: MarsRoverPhotoTable, newItem: MarsRoverPhotoTable) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MarsRoverPhotoTable, newItem: MarsRoverPhotoTable) =
                oldItem == newItem

        }
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val currentItem = getItem(position)

        currentItem?.let {
            holder.bindPhoto(currentItem, position)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
}