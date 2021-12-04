package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.*
import androidx.core.widget.NestedScrollView
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.akhilasdeveloper.marsroverphotos.Constants
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.Utilities
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentRoversBinding
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@ExperimentalPagingApi
@AndroidEntryPoint
class RoversFragment : BaseFragment(R.layout.fragment_rovers), RecyclerRoverClickListener{

    private var _binding: FragmentRoversBinding? = null
    private val binding get() = _binding!!
    private var adapter: MarsRoverAdapter =  MarsRoverAdapter(this)
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    @Inject lateinit var utilities: Utilities

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRoversBinding.bind(view)

        init()
        subscribeObservers()
        getData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                        hideSheet()
                    else
                        if (isEnabled) {
                            isEnabled = false
                            requireActivity().onBackPressed()
                        }
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)

        super.onCreate(savedInstanceState)

    }

    private fun init() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.isGestureInsetBottomIgnored = true
        bottomSheetBehavior.addBottomSheetCallback(object :BottomSheetBehavior.BottomSheetCallback(){
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED)
                    binding.homeAppbarTop.visibility = View.VISIBLE
                else
                    binding.homeAppbarTop.visibility = View.INVISIBLE
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

        })
        ViewCompat.setOnApplyWindowInsetsListener(binding.recycler) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            binding.recycler.updatePadding(bottom = systemWindows.bottom)
            return@setOnApplyWindowInsetsListener insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.homeToolbarTop) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            binding.homeToolbarTop.updatePadding(top = systemWindows.top)
            return@setOnApplyWindowInsetsListener insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.sheetFrame) { _, insets ->
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

        binding.emptyMessage.setOnClickListener {
            hideEmptyMessage()
            viewModel.getRoverData(isRefresh = false)
        }
        viewModel.setEmptyPhotos()
    }

    private fun subscribeObservers() {
        viewModel.dataStateRover.observe(viewLifecycleOwner, { response ->
            response.data?.let {
                if (it.isEmpty())
                    setEmptyMessage("Tap to refresh")
                else
                    hideEmptyMessage()
                adapter.submitList(it)
            }
            response.isLoading.let {
                 if(!(it == null || !it)) {
                     hideEmptyMessage()
                     binding.progress.isVisible = true
                 }
            }
            response.error?.let {
                Toast.makeText(requireContext(),it,Toast.LENGTH_LONG).show()
                setEmptyMessage("$it\nTap to refresh")
            }
        })

    }

    private fun hideEmptyMessage(){
        binding.emptyMessage.isVisible = false
        binding.emptyMessage.text = ""
    }

    private fun setEmptyMessage(message: String){
        binding.emptyMessage.isVisible = true
        binding.emptyMessage.text = message
    }

    private fun getData() {
        if (viewModel.dataStateRover.value==null) {
            viewModel.getRoverData(isRefresh = false)
        }

        viewModel.dataStateRoverMaster.value?.let {
            setSheetData(it)
        }

        viewModel.dataStateLoading.observe(viewLifecycleOwner, {
            binding.progress.isVisible = it
        })
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

    private fun hideSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
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
            roverStatus.text = getString(R.string.rover_status,master.status)
            roverPhotosCount.text = getString(R.string.view_photos, master.total_photos.toString())
        }
        binding.roverPhotosCount.setOnClickListener {
            navigateToPhotos(master)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}