package com.akhilasdeveloper.marsroverphotos.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.paging.PagingDataAdapter
import androidx.paging.liveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.data.DateItem
import com.akhilasdeveloper.marsroverphotos.databinding.DateItemBinding
import timber.log.Timber

class MarsRoverDateAdapter(private val context: Context, private val lifecycleOwner: LifecycleOwner) :
    PagingDataAdapter<DateItem, MarsRoverDateAdapter.PhotoViewHolder>(PHOTO_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val bindingPhoto =
            DateItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(bindingPhoto)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val currentItem = getItem(position)

        currentItem?.let {
            holder.bindPhoto(currentItem, position, context,lifecycleOwner)
        }
    }

    class PhotoViewHolder(private val binding: DateItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindPhoto(photo: DateItem, position: Int, context: Context, lifecycleOwner: LifecycleOwner) {
            val adapt = MarsRoverPhotoAdapter()
            val layoutMana = GridLayoutManager(context,3)
            binding.apply {
                date.text = photo.date
                dataRecycler.apply {
                    setHasFixedSize(true)
                    layoutManager = layoutMana
                    adapter = adapt
                }

                photo.data.liveData.observe(lifecycleOwner,{
                    adapt.submitData(lifecycleOwner.lifecycle,it)
                    Timber.d("Livedata ${photo.data}")
                })

                /*lifecycleOwner.lifecycleScope.launch {
                    val job = withTimeoutOrNull(NETWORK_TIMEOUT) {
                        photo.data.flow.cachedIn(lifecycleOwner.lifecycleScope)
                            .onEach { dataState ->
                                dataState.filter { dd->
                                    Timber.d("Livedata ${dd.camera_full_name}")
                                    true
                                }
                            }
                            .launchIn(this)
                    }

                    if (job == null) {

                    }
                }*/
            }
        }

    }

    companion object {
        private val PHOTO_COMPARATOR = object : DiffUtil.ItemCallback<DateItem>() {
            override fun areItemsTheSame(oldItem: DateItem, newItem: DateItem) =
                oldItem.date == newItem.date

            override fun areContentsTheSame(oldItem: DateItem, newItem: DateItem) =
                oldItem == newItem

        }
    }
}