package com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.databinding.*
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.viewholders.*
import com.bumptech.glide.RequestManager

class MarsRoverPhotoAdapter(
    private val interaction: RecyclerClickListener? = null,
    private val requestManager: RequestManager,
    private val isSavedView: Boolean
) :
    PagingDataAdapter<MarsRoverPhotoTable, RecyclerView.ViewHolder>(PHOTO_COMPARATOR) {

    var selectionChecker: SelectionChecker? = null

    enum class ViewType {
        SMALL,
        SMALL_SELECTED,
        DETAILED,
        DETAILED_SELECTED,
        DETAILED_SAVED,
        DETAILED_SELECTED_SAVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val bindingPhoto =
            PhotoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val bindingPhotoSelected =
            PhotoItemSelectedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val bindingDatePhoto =
            PhotoDateItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val bindingDatePhotoSelected =
            PhotoDateItemSelectedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val bindingPhotoSaved =
            LayoutSavedItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val bindingPhotoSelectedSaved = LayoutSavedItemSelectedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return when (viewType) {
            ViewType.DETAILED.ordinal -> PhotoDateViewHolder(
                bindingDatePhoto,
                interaction,
                requestManager
            )
            ViewType.DETAILED_SELECTED.ordinal -> PhotoDateSelectedViewHolder(
                bindingDatePhotoSelected,
                interaction,
                requestManager
            )
            ViewType.SMALL.ordinal -> PhotoViewHolder(
                bindingPhoto,
                interaction,
                requestManager
            )
            ViewType.SMALL_SELECTED.ordinal -> PhotoSelectedViewHolder(
                bindingPhotoSelected,
                interaction,
                requestManager
            )
            ViewType.DETAILED_SELECTED_SAVED.ordinal -> PhotoSavedSelectedViewHolder(
                bindingPhotoSelectedSaved,
                interaction,
                requestManager
            )
            ViewType.DETAILED_SAVED.ordinal -> PhotoSavedViewHolder(
                bindingPhotoSaved,
                interaction,
                requestManager
            )
            else -> PhotoViewHolder(
                bindingPhoto,
                interaction,
                requestManager
            )
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
                currentItem?.let { photo ->
                    photoViewHolder.bindPhoto(
                        photo,
                        position
                    )

                }
            }
            ViewType.DETAILED_SELECTED.ordinal -> {
                val photoViewHolder = holder as PhotoDateSelectedViewHolder
                currentItem?.let { photo ->
                    photoViewHolder.bindPhoto(
                        photo,
                        position
                    )

                }
            }
            ViewType.SMALL_SELECTED.ordinal -> {
                val photoViewHolder = holder as PhotoSelectedViewHolder
                currentItem?.let { photo ->
                    photoViewHolder.bindPhoto(
                        photo,
                        position
                    )

                }
            }
            ViewType.DETAILED_SELECTED_SAVED.ordinal -> {
                val photoItemSelectedViewHolder = holder as PhotoSavedSelectedViewHolder
                currentItem?.let {
                    photoItemSelectedViewHolder.bindPhoto(currentItem, position)
                }
            }
            ViewType.DETAILED_SAVED.ordinal -> {
                val photoViewHolder = holder as PhotoSavedViewHolder
                currentItem?.let {
                    photoViewHolder.bindPhoto(currentItem, position)
                }
            }
        }

    }

    override fun getItemViewType(position: Int): Int {
        val curr = getItem(position)
        return if (isSavedView){
            curr?.let {
                when {
                    selectionChecker?.isSelected(it) == true -> ViewType.DETAILED_SELECTED_SAVED.ordinal
                    else -> ViewType.DETAILED_SAVED.ordinal
                }
            }?:ViewType.DETAILED_SAVED.ordinal
        }else {
            if (curr?.is_placeholder == true)
                curr.let {
                    when {
                        selectionChecker?.isSelected(it) == true -> ViewType.DETAILED_SELECTED.ordinal
                        else -> ViewType.DETAILED.ordinal
                    }
                }
            else
                curr?.let {
                    when {
                        selectionChecker?.isSelected(it) == true -> ViewType.SMALL_SELECTED.ordinal
                        else -> ViewType.SMALL.ordinal
                    }
                } ?: ViewType.SMALL.ordinal
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