package com.akhilasdeveloper.marsroverphotos.ui.fragments.saved

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoItemBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.RecyclerClickListener
import com.akhilasdeveloper.marsroverphotos.utilities.*
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MarsRoverSavedPhotoAdapter(
    private val interaction: RecyclerClickListener? = null,
    private val requestManager: RequestManager,
    private val utilities: Utilities
) :
    PagingDataAdapter<MarsRoverPhotoTable, RecyclerView.ViewHolder>(PHOTO_COMPARATOR) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val bindingPhoto =
            PhotoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(bindingPhoto, interaction, requestManager, utilities)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = getItem(position)

        val photoViewHolder = holder as PhotoViewHolder
        currentItem?.let {
            photoViewHolder.bindPhoto(currentItem, position)
        }
    }


    class PhotoViewHolder(
        private val binding: PhotoItemBinding,
        private val interaction: RecyclerClickListener?,
        private val requestManager: RequestManager,
        private val utilities: Utilities
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
                }
            }
            binding.root.setOnClickListener {
                interaction?.onItemSelected(photo, position)
            }
        }

     }

    companion object {
        private val PHOTO_COMPARATOR = object : DiffUtil.ItemCallback<MarsRoverPhotoTable>() {
            override fun areItemsTheSame(
                oldItem: MarsRoverPhotoTable,
                newItem: MarsRoverPhotoTable
            ) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: MarsRoverPhotoTable,
                newItem: MarsRoverPhotoTable
            ) =
                oldItem == newItem

        }
    }

}