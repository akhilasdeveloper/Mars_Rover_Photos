package com.akhilasdeveloper.marsroverphotos.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.*
import com.akhilasdeveloper.marsroverphotos.utilities.Constants
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.databinding.RoverItemBinding
import com.akhilasdeveloper.marsroverphotos.utilities.simplify
import com.akhilasdeveloper.marsroverphotos.ui.fragments.RecyclerRoverClickListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class MarsRoverAdapter(private val interaction: RecyclerRoverClickListener? = null) : RecyclerView.Adapter<MarsRoverAdapter.PhotoViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val bindingPhoto =
            RoverItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(bindingPhoto, interaction )
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val currentItem = differ.currentList[position]
        holder.bindPhoto(currentItem, position)

    }

    class PhotoViewHolder(private val binding: RoverItemBinding, private val interaction: RecyclerRoverClickListener?) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindPhoto(photo: RoverMaster, position: Int) {
            binding.apply {

                root.animation = AnimationUtils.loadAnimation(binding.root.context, R.anim.scale_in)
                Glide.with(itemView)
                    .load(Constants.URL_DATA + photo.image)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(roverImage)
                roverName.text = photo.name
                roverPhotosCount.text = photo.total_photos.simplify()
            }

            binding.roverPhotosCount.setOnClickListener {
                interaction?.onItemSelected(photo, position)
            }
            binding.root.setOnClickListener {
                interaction?.onReadMoreSelected(photo, position)
            }
        }

    }
    fun submitList(list: List<RoverMaster>) {
        differ.submitList(list)
    }
    private val differ = AsyncListDiffer(
        RoverRecyclerChangeCallback(this),
        AsyncDifferConfig.Builder(DataDiffUtil).build())

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
}