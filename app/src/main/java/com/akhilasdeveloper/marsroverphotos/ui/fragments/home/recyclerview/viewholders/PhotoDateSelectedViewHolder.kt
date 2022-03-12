package com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoDateItemSelectedBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.RecyclerClickListener
import com.akhilasdeveloper.marsroverphotos.utilities.formatMillisToDisplayDate
import com.bumptech.glide.RequestManager

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


                count.text = root.context.getString(R.string.total_count_photos, it.total_count.toString())
                sol.text = it.sol.toString()
                date.text = it.earth_date.formatMillisToDisplayDate()
            }
        }

        binding.root.setOnClickListener {
            interaction?.onItemSelected(photo, absoluteAdapterPosition)
        }

    }

}