package com.akhilasdeveloper.marsroverphotos.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.*
import com.akhilasdeveloper.marsroverphotos.Constants
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.databinding.RoverItemBinding
import com.akhilasdeveloper.marsroverphotos.ui.fragments.RecyclerRoverClickListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class MarsRoverAdapter(private val interaction: RecyclerRoverClickListener? = null,
private val context: Context) : RecyclerView.Adapter<MarsRoverAdapter.PhotoViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val bindingPhoto =
            RoverItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(bindingPhoto, interaction , context)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val currentItem = differ.currentList[position]
        holder.bindPhoto(currentItem, position)

    }

    class PhotoViewHolder(private val binding: RoverItemBinding, private val interaction: RecyclerRoverClickListener?, private val context: Context) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindPhoto(photo: RoverMaster, position: Int) {
            binding.apply {
                Glide.with(itemView)
                    .load(Constants.URL_DATA + photo.image)
                    .centerCrop()
                    .placeholder(R.drawable.imageview_placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(roverImage)
                roverName.text = photo.name
                roverPhotosCount.text = context.getString(R.string.view_photos, photo.total_photos.toString())
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