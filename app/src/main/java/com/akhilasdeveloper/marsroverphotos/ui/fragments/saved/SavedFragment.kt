package com.akhilasdeveloper.marsroverphotos.ui.fragments.saved

import android.os.Bundle
import android.view.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.akhilasdeveloper.marsroverphotos.utilities.*

import com.akhilasdeveloper.marsroverphotos.databinding.FragmentSavedBinding
import com.akhilasdeveloper.marsroverphotos.ui.fragments.BaseFragment
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.RecyclerClickListener
import com.bumptech.glide.RequestManager
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SavedFragment : BaseFragment(R.layout.fragment_saved), RecyclerClickListener {


    private var _binding: FragmentSavedBinding? = null
    internal val binding get() = _binding!!

    @Inject
    lateinit var utilities: Utilities
    internal var master: RoverMaster? = null

    @Inject
    lateinit var requestManager: RequestManager
    private var adapter: MarsRoverSavedPhotoAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSavedBinding.bind(view)

        init()
        sync()
        subscribeObservers()
    }

    private fun sync() {
        lifecycleScope.launch {
            if (utilities.isLikesInSync() == Constants.DATASTORE_LIKES_SYNC_FALSE)
                viewModel.syncLikedPhotos()
        }
    }

    private fun init() {
        adapter = MarsRoverSavedPhotoAdapter(this, requestManager, utilities = utilities)
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
            adapter?.submitData(viewLifecycleOwner.lifecycle, it)
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
        view: View,
        x: Float,
        y: Float
    ): Boolean {
        return true
    }

}