package com.akhilasdeveloper.marsroverphotos.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoDateItemBinding
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoItemBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.ui.fragments.RecyclerClickListener
import com.akhilasdeveloper.marsroverphotos.utilities.formatMillisToDisplayDate
import com.akhilasdeveloper.marsroverphotos.utilities.showShortToast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class MarsRoverPhotoAdapter(
    private val interaction: RecyclerClickListener? = null
) :
    PagingDataAdapter<MarsRoverPhotoTable, RecyclerView.ViewHolder>(PHOTO_COMPARATOR) {

    enum class ViewType {
        SMALL,
        DETAILED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val bindingPhoto =
            PhotoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val bindingDatePhoto =
            PhotoDateItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return when (viewType) {
            ViewType.DETAILED.ordinal -> PhotoDateViewHolder(bindingDatePhoto, interaction)
            else -> PhotoViewHolder(bindingPhoto, interaction)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = getItem(position)
        when (holder.itemViewType) {
            ViewType.DETAILED.ordinal -> {
                val photoItemViewHolder = holder as PhotoDateViewHolder
                currentItem?.let {
                    photoItemViewHolder.bindPhoto(currentItem, position)
                }
            }
            ViewType.SMALL.ordinal -> {
                val photoViewHolder = holder as PhotoViewHolder
                currentItem?.let {
                    photoViewHolder.bindPhoto(currentItem, position)
                }
            }
        }


    }

    class PhotoViewHolder(
        private val binding: PhotoItemBinding,
        private val interaction: RecyclerClickListener?
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindPhoto(photo: MarsRoverPhotoTable, position: Int) {
            binding.apply {
                root.animation = AnimationUtils.loadAnimation(binding.root.context, R.anim.fade_in)
                photo.let {
                    Glide.with(itemView)
                        .load(it.img_src)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(imageDescription)
                    cameraName.text = it.camera_name
                    cameraName.setOnClickListener {_->
                        binding.root.context.showShortToast(it.camera_full_name)
                    }
                }
            }
            binding.root.setOnClickListener {
                interaction?.onItemSelected(photo, position)
            }
        }

    }

    class PhotoDateViewHolder(
        private val binding: PhotoDateItemBinding,
        private val interaction: RecyclerClickListener?
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindPhoto(photo: MarsRoverPhotoTable, position: Int) {
            binding.apply {
                root.animation = AnimationUtils.loadAnimation(binding.root.context, R.anim.fade_in)
                photo.let {
                    photoItem.apply {
                        Glide.with(itemView)
                            .load(it.img_src)
                            .centerCrop()
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(imageDescription)
                        cameraName.text = it.camera_name
                        cameraName.setOnClickListener {_->
                            binding.root.context.showShortToast(it.camera_full_name)
                        }
                    }
                    binding.date.text = it.earth_date.formatMillisToDisplayDate()
                }
            }

            binding.root.setOnClickListener {
                interaction?.onItemSelected(photo, position)
            }
        }

    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position)?.is_placeholder == true) ViewType.DETAILED.ordinal
        else ViewType.SMALL.ordinal
    }

    companion object {
        private val PHOTO_COMPARATOR = object : DiffUtil.ItemCallback<MarsRoverPhotoTable>() {
            override fun areItemsTheSame(oldItem: MarsRoverPhotoTable, newItem: MarsRoverPhotoTable) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MarsRoverPhotoTable, newItem: MarsRoverPhotoTable) =
                oldItem == newItem

        }
    }

}