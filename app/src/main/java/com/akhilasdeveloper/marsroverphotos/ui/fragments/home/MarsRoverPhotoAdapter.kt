package com.akhilasdeveloper.marsroverphotos.ui.fragments.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoDateItemBinding
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoItemBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.utilities.formatMillisToDisplayDate
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class MarsRoverPhotoAdapter(
    private val interaction: RecyclerClickListener? = null
) :
    PagingDataAdapter<MarsRoverPhotoTable, RecyclerView.ViewHolder>(PHOTO_COMPARATOR) {

    var selectionChecker: SelectionChecker? = null

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
                    photoItemViewHolder.bindPhoto(currentItem, position,selectionChecker?.isSelected(currentItem) == true)
                }
            }
            ViewType.SMALL.ordinal -> {
                val photoViewHolder = holder as PhotoViewHolder
                currentItem?.let { photo ->
                    photoViewHolder.bindPhoto(
                        photo,
                        position,
                        selectionChecker?.isSelected(currentItem) == true
                    )

                }
            }
        }

    }

    class PhotoViewHolder(
        private val binding: PhotoItemBinding,
        private val interaction: RecyclerClickListener?
    ) :
        RecyclerView.ViewHolder(binding.root) {

        var positionSel = 0
        var photo: MarsRoverPhotoTable? = null

        fun bindPhoto(photo: MarsRoverPhotoTable, position: Int, selected: Boolean) {
            positionSel = position
            this.photo = photo
            binding.apply {
                photo.let {
                    imageDescription.transitionName = it.photo_id.toString()
                    Glide.with(itemView)
                        .load(it.img_src)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(imageDescription)
                }
            }

            binding.selection.isChecked = selected
            binding.selection.isVisible = selected
            binding.overlay.isVisible = selected

            binding.root.setOnClickListener {
                interaction?.onItemSelected(photo, position)
            }
        }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int = positionSel
                override fun getSelectionKey(): Long? = photo?.photo_id
            }
    }

    class PhotoDateViewHolder(
        private val binding: PhotoDateItemBinding,
        private val interaction: RecyclerClickListener?
    ) :
        RecyclerView.ViewHolder(binding.root) {

        var positionSel = 0
        var photo: MarsRoverPhotoTable? = null

        fun bindPhoto(photo: MarsRoverPhotoTable, position: Int, selected: Boolean) {
            positionSel = position
            this.photo = photo
            binding.apply {
                photo.let {
                    imageDescription.transitionName = it.photo_id.toString()
                    Glide.with(itemView)
                        .load(it.img_src)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(imageDescription)


                    binding.count.text = it.total_count.toString() + " photos"
                    binding.sol.text = it.sol.toString()
                    binding.date.text = it.earth_date.formatMillisToDisplayDate()
                }
            }

            binding.selection.isChecked = selected
            binding.selection.isVisible = selected
            binding.overlay.isVisible = selected

            binding.root.setOnClickListener {
                interaction?.onItemSelected(photo, position)
            }
        }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int = positionSel
                override fun getSelectionKey(): Long? = photo?.photo_id
            }

    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position)?.is_placeholder == true) ViewType.DETAILED.ordinal
        else ViewType.SMALL.ordinal
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