package com.akhilasdeveloper.marsroverphotos.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.Constants
import com.akhilasdeveloper.marsroverphotos.data.RoverPhotoViewItem
import com.akhilasdeveloper.marsroverphotos.databinding.ViewPagerDateItemBinding
import com.akhilasdeveloper.marsroverphotos.databinding.ViewPagerItemBinding
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.ui.fragments.RecyclerClickListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class MarsRoverPagerAdapter(private val interaction: RecyclerClickListener? = null) :
    PagingDataAdapter<MarsRoverPhotoDb, RecyclerView.ViewHolder>(PHOTO_COMPARATOR) {

    private val PHOTOITEM = 1
    private val DATEITEM = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):RecyclerView.ViewHolder {
        return when(viewType){
            DATEITEM -> PhotoDateViewHolder(ViewPagerDateItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> PhotoViewHolder(ViewPagerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false), interaction)
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


    class PhotoDateViewHolder(private val binding: ViewPagerDateItemBinding):RecyclerView.ViewHolder(binding.root){
        fun bindPhoto(photo: MarsRoverPhotoDb) {
            binding.apply {
                photo.let {
                    date.text = it.earth_date.toString()
                }
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

    override fun getItemViewType(position: Int): Int = getType(getItem(position))


    private fun getType(data : MarsRoverPhotoDb?):Int {
        data?.let {
            if (it.is_placeholder == Constants.TRUE)
                return DATEITEM
        }
        return PHOTOITEM
    }
}