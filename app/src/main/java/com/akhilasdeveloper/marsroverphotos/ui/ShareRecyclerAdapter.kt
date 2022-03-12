package com.akhilasdeveloper.marsroverphotos.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.*
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.ShareItemBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.ui.fragments.RecyclerShareClickListener
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class ShareRecyclerAdapter(
        private val interaction: RecyclerShareClickListener? = null,
        private val requestManager: RequestManager
    ) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val bindingPhoto = ShareItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PhotoViewHolder(bindingPhoto, interaction, requestManager)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val currentItem = differ.currentList[position]

            val photoItemViewHolder = holder as PhotoViewHolder
            currentItem?.let {
                photoItemViewHolder.bindPhoto(currentItem, position)
            }
        }


        class PhotoViewHolder(
            private val binding: ShareItemBinding,
            private val interaction: RecyclerShareClickListener?,
            private val requestManager: RequestManager
        ) :
            RecyclerView.ViewHolder(binding.root) {

            fun bindPhoto(photo: MarsRoverPhotoTable, position: Int) {
                binding.apply {

                    root.animation = AnimationUtils.loadAnimation(binding.root.context, R.anim.scale_in)
                    requestManager
                        .load(photo.img_src)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(imageDescription)
                }

                binding.delete.setOnClickListener {
                    interaction?.onItemDeleteClicked(photo, absoluteAdapterPosition)
                }
            }

        }

        fun submitList(list: List<MarsRoverPhotoTable>) {
            differ.submitList(list)
        }

        private val differ = AsyncListDiffer(
            RoverRecyclerChangeCallback(this),
            AsyncDifferConfig.Builder(DataDiffUtil).build()
        )

        internal inner class RoverRecyclerChangeCallback(
            private val adapter: ShareRecyclerAdapter
        ) : ListUpdateCallback {

            override fun onChanged(position: Int, count: Int, payload: Any?) {
                adapter.notifyItemChanged(position)
            }

            override fun onInserted(position: Int, count: Int) {
                adapter.notifyItemInserted(position)
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                adapter.notifyItemMoved(fromPosition, toPosition)
            }

            override fun onRemoved(position: Int, count: Int) {
                adapter.notifyItemRemoved(position)
            }
        }

        companion object {
            private val DataDiffUtil = object : DiffUtil.ItemCallback<MarsRoverPhotoTable>() {
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

        override fun getItemCount(): Int {
            return differ.currentList.size
        }
    }