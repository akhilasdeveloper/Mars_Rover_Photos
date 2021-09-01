package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.*
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.akhilasdeveloper.marsroverphotos.Constants
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.Utilities
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentHomeBinding
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentRoversBinding
import com.akhilasdeveloper.marsroverphotos.ui.MarsRoverPhotoLoadStateAdapter
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@ExperimentalPagingApi
@AndroidEntryPoint
class RoversFragment : BaseFragment(R.layout.fragment_rovers), RecyclerRoverClickListener{

    private var _binding: FragmentRoversBinding? = null
    private val binding get() = _binding!!
    private val adapter: MarsRoverAdapter =  MarsRoverAdapter(this)
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    @Inject lateinit var utilities: Utilities

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRoversBinding.bind(view)

        init()
        setListeners()
        subscribeObservers()
        getData()
    }

    private fun setListeners() {
        /*bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback(){
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (bottomSheetBehavior.state == BottomSheetBehavior.SAVE_PEEK_HEIGHT)
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

        })*/
    }

    private fun init() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.isGestureInsetBottomIgnored = true
        ViewCompat.setOnApplyWindowInsetsListener(binding.recycler) { v, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            binding.recycler.updatePadding(bottom = systemWindows.bottom, top = systemWindows.top)
            return@setOnApplyWindowInsetsListener insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.sheetFrame) { v, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            val layoutParams = (binding.sheetFrame.layoutParams as? ViewGroup.MarginLayoutParams)
            layoutParams?.setMargins(0, systemWindows.top, 0, systemWindows.bottom)
            binding.sheetFrame.layoutParams = layoutParams
            return@setOnApplyWindowInsetsListener insets
        }

        val layoutManager = LinearLayoutManager(requireContext())

        binding.apply {
            recycler.setHasFixedSize(true)
            recycler.layoutManager = layoutManager
            recycler.adapter = adapter
        }
    }

    private fun subscribeObservers() {
        viewModel.dataStateRover.observe(viewLifecycleOwner, Observer { response ->
            response.data?.let {
                adapter.submitList(it)
            }
            response.isLoading.let {
                binding.progress.isVisible = !(it == null || !it)
            }
            response.error?.let {
                Toast.makeText(requireContext(),it,Toast.LENGTH_LONG).show()
            }
        })

    }

    private fun getData() {
        if (viewModel.dataStateRover.value==null) {
            binding.progress.isVisible = true
            viewModel.getRoverData(isRefresh = false)
        }
    }

    override fun onItemSelected(master: RoverMaster, position: Int) {
        navigateToPhotos(master)
    }

    private fun navigateToPhotos(master: RoverMaster){
        viewModel.getData(date = utilities.formatDateToMillis(master.max_date)!!, roverName = master.name)
        viewModel.setRoverMaster(master)
        findNavController().navigate(R.id.action_roversFragment_to_homeFragment)
    }

    override fun onReadMoreSelected(master: RoverMaster, position: Int) {
        setSheetData(master)
        showSheet()
    }

    private fun showSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setSheetData(master: RoverMaster) {
        binding.apply {
            Glide.with(binding.root)
                .load(Constants.URL_DATA + master.image)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(roverImage)
            roverName.text = master.name
            roverDescription.text = master.description
            roverLandingDate.text = master.landing_date
            roverLaunchDate.text = master.launch_date
            roverStatus.text = "Rover Status : ${master.status}"
            roverPhotosCount.text = "${master.total_photos} Photos"
        }
        binding.roverPhotosCount.setOnClickListener {
            navigateToPhotos(master)
        }
    }

}