package com.akhilasdeveloper.marsroverphotos.ui.fragments.saved

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.LayoutSavedItemBinding
import com.akhilasdeveloper.marsroverphotos.databinding.LayoutSavedItemSelectedBinding
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoItemBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.MarsRoverPhotoAdapter
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.RecyclerClickListener
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.SelectionChecker
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.viewholders.PhotoDateViewHolder
import com.akhilasdeveloper.marsroverphotos.utilities.*
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class MarsRoverSavedPhotoAdapter(
    private val interaction: RecyclerClickListener? = null,
    private val requestManager: RequestManager,
    private val utilities: Utilities
) :
    PagingDataAdapter<MarsRoverPhotoTable, RecyclerView.ViewHolder>(PHOTO_COMPARATOR) {

    var selectionChecker: SelectionChecker? = null

    enum class ViewType {
        DETAILED,
        DETAILED_SELECTED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val bindingPhoto =
            LayoutSavedItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val bindingPhotoSelected = LayoutSavedItemSelectedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return when (viewType) {
            ViewType.DETAILED_SELECTED.ordinal -> PhotoSelectedViewHolder(
                bindingPhotoSelected,
                interaction,
                requestManager,
                utilities
            )
            else -> PhotoViewHolder(bindingPhoto, interaction, requestManager, utilities)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = getItem(position)
        when (holder.itemViewType) {
            ViewType.DETAILED_SELECTED.ordinal -> {
                val photoItemSelectedViewHolder = holder as PhotoSelectedViewHolder
                currentItem?.let {
                    photoItemSelectedViewHolder.bindPhoto(currentItem, position)
                }
            }
            else -> {
                val photoViewHolder = holder as PhotoViewHolder
                currentItem?.let {
                    photoViewHolder.bindPhoto(currentItem, position)
                }
            }
        }

    }


    class PhotoViewHolder(
        private val binding: LayoutSavedItemBinding,
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

                    sol.text = it.sol.toString()
                    date.text = it.earth_date.formatMillisToDisplayDate()
                }
            }
            binding.root.setOnClickListener {
                interaction?.onItemSelected(photo, position)
            }
            binding.root.setOnLongClickListener {
                interaction?.onItemLongClick(photo, absoluteAdapterPosition) ?: false
            }
        }

    }

    class PhotoSelectedViewHolder(
        private val binding: LayoutSavedItemSelectedBinding,
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

                    sol.text = it.sol.toString()
                    date.text = it.earth_date.formatMillisToDisplayDate()
                }
            }
            binding.root.setOnClickListener {
                interaction?.onItemSelected(photo, absoluteAdapterPosition)
            }
        }

    }

    override fun getItemViewType(position: Int): Int {
        val curr = getItem(position)
        return curr?.let {
            if (selectionChecker?.isSelected(it) == true) ViewType.DETAILED_SELECTED.ordinal else null
        } ?: ViewType.DETAILED.ordinal
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