package com.akhilasdeveloper.marsroverphotos.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.data.RoverPhotoViewItem
import com.akhilasdeveloper.marsroverphotos.databinding.DateItemBinding
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoItemBinding
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.ui.fragments.RecyclerClickListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import timber.log.Timber

class MarsRoverPhotoAdapter(private val interaction: RecyclerClickListener? = null) :
    PagingDataAdapter<RoverPhotoViewItem, RecyclerView.ViewHolder>(PHOTO_COMPARATOR) {

    val PHOTOITEM = 1
    val DATEITEM = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):RecyclerView.ViewHolder {
        return when(viewType){
            DATEITEM -> PhotoDateViewHolder(DateItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> PhotoViewHolder(PhotoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false), interaction)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = getItem(position)
        currentItem?.let {
            if (getType(currentItem) == PHOTOITEM){
                val hol = holder as PhotoViewHolder
                hol.bindPhoto(currentItem, position)
            }else{
                val hol = holder as PhotoDateViewHolder
                hol.bindPhoto(currentItem)
            }
        }
    }

    class PhotoViewHolder(private val binding: PhotoItemBinding, private val interaction: RecyclerClickListener?) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindPhoto(photo: RoverPhotoViewItem, position: Int) {
            binding.apply {
                photo.photo?.let {
                    Glide.with(itemView)
                        .load(it.img_src)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(imageDescription)
                    cameraName.text = "${it.id} Camera"
                    roverName.text = "${it.earth_date} Rover"
                }
            }

            binding.root.setOnClickListener {
                interaction?.onItemSelected(photo, position)
            }
        }

    }


    class PhotoDateViewHolder(private val binding: DateItemBinding):RecyclerView.ViewHolder(binding.root){
        fun bindPhoto(photo: RoverPhotoViewItem) {
            binding.apply {
                photo.date?.let {
                    date.text = it
                }
            }
        }
    }

    companion object {
        private val PHOTO_COMPARATOR = object : DiffUtil.ItemCallback<RoverPhotoViewItem>() {
            override fun areItemsTheSame(oldItem: RoverPhotoViewItem, newItem: RoverPhotoViewItem) =
                oldItem.date == newItem.date && oldItem.photo?.id == newItem.photo?.id

            override fun areContentsTheSame(oldItem: RoverPhotoViewItem, newItem: RoverPhotoViewItem) =
                oldItem == newItem

        }
    }

//    override fun getItemViewType(position: Int) = getType(getItem(position))

    override fun getItemViewType(position: Int): Int {
        return if (itemCount > position) getType(getItem(position)) else DATEITEM
    }


    private fun getType(data : RoverPhotoViewItem?):Int {
        data?.date?.let {
            return DATEITEM
        }
        return PHOTOITEM
    }
}