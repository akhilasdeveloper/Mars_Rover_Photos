package com.akhilasdeveloper.marsroverphotos.ui.fragments.saved

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.akhilasdeveloper.marsroverphotos.utilities.*

import com.akhilasdeveloper.marsroverphotos.databinding.FragmentSavedBinding
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoDateItemBinding
import com.akhilasdeveloper.marsroverphotos.databinding.PhotoItemBinding
import com.akhilasdeveloper.marsroverphotos.ui.fragments.BaseFragment
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.MarsRoverPhotoAdapter
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.RecyclerClickListener

@AndroidEntryPoint
class SavedFragment : BaseFragment(R.layout.fragment_saved), RecyclerClickListener {

    private var _binding: FragmentSavedBinding? = null
    internal val binding get() = _binding!!

    @Inject
    lateinit var utilities: Utilities
    internal var master: RoverMaster? = null

    private val adapter = MarsRoverSavedPhotoAdapter(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSavedBinding.bind(view)

        init()
        subscribeObservers()
    }

    private fun init() {
        val layoutManager = GridLayoutManager(
            requireContext(),
            Constants.GALLERY_SPAN,
            GridLayoutManager.VERTICAL,
            false
        )
        binding.apply {
            photoRecycler.setHasFixedSize(true)
            photoRecycler.layoutManager = layoutManager
            photoRecycler.adapter = adapter
        }
    }

    private fun subscribeObservers() {
        viewModel.dataStateRoverMaster.observe(viewLifecycleOwner, {
            val isHandled = it.hasBeenHandled()
            it.peekContent?.let { rover ->
                it.setAsHandled()
                master = rover
                setData()
                if (!isHandled) {
                    getData()
                }
            }
        })

        viewModel.dataStateLikedPhotos.observe(viewLifecycleOwner, {
            it?.let {
                adapter.submitData(viewLifecycleOwner.lifecycle, it)
            }
        })
    }

    private fun getData() {
        master?.let { master ->
            viewModel.getLikedPhotos(master)
        }
    }

    private fun setData() {
        master?.let {
            setTitle()
        }
    }

    private fun setTitle() {
        binding.topAppbar.homeToolbarTop.title = master!!.name + " Rover (Liked Photos)"
        binding.topAppbar.homeCollapsingToolbarTop.title = master!!.name + " Rover (Liked Photos)"
    }

    override fun onItemSelected(
        marsRoverPhoto: MarsRoverPhotoTable,
        position: Int
    ) {

    }

    override fun onItemLongClick(
        marsRoverPhoto: MarsRoverPhotoTable,
        position: Int,
        view: PhotoItemBinding
    ): Boolean {
        return true
    }

    override fun onDateItemLongClick(
        photo: MarsRoverPhotoTable,
        position: Int,
        binding: PhotoDateItemBinding
    ): Boolean {
        return true
    }


}