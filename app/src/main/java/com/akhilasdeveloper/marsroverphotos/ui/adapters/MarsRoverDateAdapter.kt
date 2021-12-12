package com.akhilasdeveloper.marsroverphotos.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingDataAdapter
import androidx.paging.cachedIn
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.data.DatePreviewData
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoDateItemBinding
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoItemBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.ui.fragments.RecyclerClickListener
import com.akhilasdeveloper.marsroverphotos.utilities.formatMillisToDisplayDate
import com.akhilasdeveloper.marsroverphotos.utilities.showShortToast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MarsRoverDateAdapter(
    private val scope: LifecycleCoroutineScope,
    private val interaction: RecyclerClickListener? = null
) :
    PagingDataAdapter<DatePreviewData, MarsRoverDateAdapter.DateViewHolder>(PHOTO_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val bindingDatePhoto =
            PhotoDateItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DateViewHolder(bindingDatePhoto, interaction, scope)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val currentItem = getItem(position)
        currentItem?.let {
            holder.bindPhoto(currentItem, position)
        }
    }

    class DateViewHolder(
        private val binding: PhotoDateItemBinding,
        private val interaction: RecyclerClickListener?,
        private val scope: LifecycleCoroutineScope
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindPhoto(photo: DatePreviewData, position: Int) {
            binding.apply {
                photo.let {
                    binding.date.text = it.currentDate.formatMillisToDisplayDate()
                    scope.launch {
                        it.photos
                            .onEach { photoList->
                                binding.photoItem.cameraName.text = photoList.toString()
                            }
                            .launchIn(this)
                    }
                }
            }
        }

    }

    companion object {
        private val PHOTO_COMPARATOR = object : DiffUtil.ItemCallback<DatePreviewData>() {
            override fun areItemsTheSame(oldItem: DatePreviewData, newItem: DatePreviewData) =
                oldItem.currentDate == newItem.currentDate && oldItem.roverName == newItem.roverName

            override fun areContentsTheSame(oldItem: DatePreviewData, newItem: DatePreviewData) =
                oldItem == newItem

        }
    }

}