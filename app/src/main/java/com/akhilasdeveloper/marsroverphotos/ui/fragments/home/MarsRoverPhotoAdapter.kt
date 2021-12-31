package com.akhilasdeveloper.marsroverphotos.ui.fragments.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoDateItemBinding
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoDateItemSelectedBinding
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoItemBinding
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoItemSelectedBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.utilities.Utilities
import com.akhilasdeveloper.marsroverphotos.utilities.downloadImageAsBitmap2
import com.akhilasdeveloper.marsroverphotos.utilities.formatMillisToDisplayDate
import com.akhilasdeveloper.marsroverphotos.utilities.formatMillisToFileDate
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MarsRoverPhotoAdapter(
    private val interaction: RecyclerClickListener? = null,
    private val requestManager: RequestManager
) :
    PagingDataAdapter<MarsRoverPhotoTable, RecyclerView.ViewHolder>(PHOTO_COMPARATOR) {

    var selectionChecker: SelectionChecker? = null

    enum class ViewType {
        SMALL,
        SMALL_SELECTED,
        DETAILED,
        DETAILED_SELECTED
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
        }

    }

    class PhotoViewHolder(
        private val binding: PhotoItemBinding,
        private val interaction: RecyclerClickListener?,
        private val requestManager: RequestManager
    ) :
        RecyclerView.ViewHolder(binding.root) {

        private var positionSel = 0
        var photo: MarsRoverPhotoTable? = null

        fun bindPhoto(photo: MarsRoverPhotoTable, position: Int) {
            positionSel = position
            this.photo = photo
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
                interaction?.onItemSelected(photo, absoluteAdapterPosition)
            }

            binding.root.setOnLongClickListener {
                interaction?.onItemLongClick(photo, absoluteAdapterPosition) ?: false
            }
        }


    }

    class PhotoSelectedViewHolder(
        private val binding: PhotoItemSelectedBinding,
        private val interaction: RecyclerClickListener?,
        private val requestManager: RequestManager
    ) :
        RecyclerView.ViewHolder(binding.root) {

        var positionSel = 0
        var photo: MarsRoverPhotoTable? = null

        fun bindPhoto(photo: MarsRoverPhotoTable, position: Int) {
            positionSel = position
            this.photo = photo
            binding.apply {
                photo.let {
                    imageDescription.transitionName = it.photo_id.toString()
                    requestManager
                        .load(it.img_src)
                        .centerCrop()
                        .into(imageDescription)
                }
            }

            binding.root.setOnClickListener {
                interaction?.onItemSelected(photo, absoluteAdapterPosition)
            }

            /*binding.root.setOnLongClickListener {
                interaction?.onItemLongClick(photo, position, binding) ?: false
            }*/
        }

    }

    class PhotoDateViewHolder(
        private val binding: PhotoDateItemBinding,
        private val interaction: RecyclerClickListener?,
        private val requestManager: RequestManager
    ) :
        RecyclerView.ViewHolder(binding.root) {

        var positionSel = 0
        var photo: MarsRoverPhotoTable? = null

        fun bindPhoto(photo: MarsRoverPhotoTable, position: Int) {
            positionSel = position
            this.photo = photo
            binding.apply {
                photo.let {
                    imageDescription.transitionName = it.photo_id.toString()
                    requestManager
                        .load(it.img_src)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(imageDescription)


                    binding.count.text = it.total_count.toString() + " photos"
                    binding.sol.text = it.sol.toString()
                    binding.date.text = it.earth_date.formatMillisToDisplayDate()
                }
            }

            binding.root.setOnClickListener {
                interaction?.onItemSelected(photo, absoluteAdapterPosition)
            }

            binding.root.setOnLongClickListener {
                interaction?.onItemLongClick(photo, absoluteAdapterPosition) ?: false
            }
        }

    }

    class PhotoDateSelectedViewHolder(
        private val binding: PhotoDateItemSelectedBinding,
        private val interaction: RecyclerClickListener?,
        private val requestManager: RequestManager
    ) :
        RecyclerView.ViewHolder(binding.root) {

        var positionSel = 0
        var photo: MarsRoverPhotoTable? = null

        fun bindPhoto(photo: MarsRoverPhotoTable, position: Int) {
            positionSel = position
            this.photo = photo
            binding.apply {
                photo.let {
                    imageDescription.transitionName = it.photo_id.toString()
                    requestManager
                        .load(it.img_src)
                        .centerCrop()
                        .into(imageDescription)


                    binding.count.text = it.total_count.toString() + " photos"
                    binding.sol.text = it.sol.toString()
                    binding.date.text = it.earth_date.formatMillisToDisplayDate()
                }
            }

            binding.root.setOnClickListener {
                interaction?.onItemSelected(photo, absoluteAdapterPosition)
            }

            /*binding.root.setOnLongClickListener {
                interaction?.onDateItemLongClick(photo, position, binding) ?: false
            }*/
        }

    }

    override fun getItemViewType(position: Int): Int {
        val curr = getItem(position)
        return if (curr?.is_placeholder == true)
            curr.let {
                if (selectionChecker?.isSelected(it) == true)
                    ViewType.DETAILED_SELECTED.ordinal
                else
                    ViewType.DETAILED.ordinal
            }
        else
            curr?.let {
                if (selectionChecker?.isSelected(it) == true)
                    ViewType.SMALL_SELECTED.ordinal
                else
                    ViewType.SMALL.ordinal
            } ?: ViewType.SMALL.ordinal
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