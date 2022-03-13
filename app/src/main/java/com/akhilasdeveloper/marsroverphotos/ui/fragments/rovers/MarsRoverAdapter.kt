package com.akhilasdeveloper.marsroverphotos.ui.fragments.rovers

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.*
import com.akhilasdeveloper.marsroverphotos.utilities.Constants
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.databinding.LayoutAboutButtonBinding
import com.akhilasdeveloper.marsroverphotos.databinding.RoverItemBinding
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.MarsRoverPhotoAdapter
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.viewholders.*
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.ABOUT_VIEW_ID
import com.akhilasdeveloper.marsroverphotos.utilities.simplify
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions


class MarsRoverAdapter(
    private val interaction: RecyclerRoverClickListener? = null,
    private val requestManager: RequestManager
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class ViewType {
        ROVER,
        ABOUT
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val bindingPhoto =
            RoverItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val bindingAbout =
            LayoutAboutButtonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return when (viewType) {
            ViewType.ROVER.ordinal -> PhotoViewHolder(bindingPhoto, interaction, requestManager)
            ViewType.ABOUT.ordinal -> AboutViewHolder(
                bindingAbout,
                interaction
            )
            else -> PhotoViewHolder(bindingPhoto, interaction, requestManager)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = differ.currentList[position]

        when (holder.itemViewType) {
            ViewType.ROVER.ordinal -> {
                val photoItemViewHolder = holder as PhotoViewHolder
                currentItem?.let {
                    photoItemViewHolder.bindPhoto(currentItem, position)
                }
            }
            ViewType.ABOUT.ordinal -> {
                val aboutItemViewHolder = holder as AboutViewHolder
                aboutItemViewHolder.bindPhoto()
            }
        }
    }


    class PhotoViewHolder(
        private val binding: RoverItemBinding,
        private val interaction: RecyclerRoverClickListener?,
        private val requestManager: RequestManager
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindPhoto(photo: RoverMaster, position: Int) {
            binding.apply {

                root.animation = AnimationUtils.loadAnimation(binding.root.context, R.anim.scale_in)
                requestManager
                    .load(Constants.URL_DATA + photo.image)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(roverImage)
                roverName.text = photo.name
                roverPhotosCount.text = root.context.getString(
                    R.string.simplify_photo_count,
                    photo.total_photos.simplify()
                )
            }

            binding.roverPhotosCount.setOnClickListener {
                interaction?.onItemSelected(photo, position)
            }
            binding.roverSavedPhotosCount.setOnClickListener {
                interaction?.onItemSaveSelected(photo, position)
            }
            binding.root.setOnClickListener {
                interaction?.onReadMoreSelected(photo, position)
            }
        }

    }

    class AboutViewHolder(
        private val binding: LayoutAboutButtonBinding,
        private val interaction: RecyclerRoverClickListener?
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindPhoto() {
            binding.apply {
                root.animation = AnimationUtils.loadAnimation(binding.root.context, R.anim.scale_in)
            }
            binding.root.setOnClickListener {
                interaction?.onAboutSelected()
            }
        }

    }

    fun submitList(list: List<RoverMaster>) {
        differ.submitList(list)
    }

    private val differ = AsyncListDiffer(
        RoverRecyclerChangeCallback(this),
        AsyncDifferConfig.Builder(DataDiffUtil).build()
    )

    internal inner class RoverRecyclerChangeCallback(
        private val adapter: MarsRoverAdapter
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
        private val DataDiffUtil = object : DiffUtil.ItemCallback<RoverMaster>() {
            override fun areItemsTheSame(oldItem: RoverMaster, newItem: RoverMaster) =
                oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: RoverMaster, newItem: RoverMaster) =
                oldItem == newItem

        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun getItemViewType(position: Int): Int {
        val curr = differ.currentList[position]
        return if (curr.id == ABOUT_VIEW_ID)
            ViewType.ABOUT.ordinal
        else
            ViewType.ROVER.ordinal
    }
}