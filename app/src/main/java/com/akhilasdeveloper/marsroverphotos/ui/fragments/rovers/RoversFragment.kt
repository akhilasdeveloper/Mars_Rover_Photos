package com.akhilasdeveloper.marsroverphotos.ui.fragments.rovers

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.*
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.GridLayoutManager
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentRoversBinding
import com.akhilasdeveloper.marsroverphotos.ui.fragments.BaseFragment
import com.akhilasdeveloper.marsroverphotos.utilities.*
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.ROVER_STATUS_COMPLETE
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.snack_bar_layout.*
import javax.inject.Inject

@ExperimentalPagingApi
@AndroidEntryPoint
class RoversFragment : BaseFragment(R.layout.fragment_rovers), RecyclerRoverClickListener {

    private var _binding: FragmentRoversBinding? = null
    private val binding get() = _binding!!
    @Inject
    lateinit var requestManager: RequestManager
    private var adapter: MarsRoverAdapter? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    lateinit var roversViewModel: RoversViewModel

    override fun onCreate(savedInstanceState: Bundle?) {

        setBackPressCallBack()
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRoversBinding.bind(view)

        init()
        setWindowInsets()
        setListeners()
        uiObservers()
        getData()
    }

    private fun uiObservers() {
        roversViewModel.apply {
            viewStateRoverSwipeRefresh.observe(viewLifecycleOwner, {
                binding.roverSwipeRefresh.isRefreshing = it
            })
            viewStateErrorMessage.observe(viewLifecycleOwner, {
                it.contentIfNotHandled?.let { message ->
                    uiCommunicationListener.showSnackBarMessage(
                        messageText = message,
                        buttonText = getString(R.string.trefresh)
                    ) {
                        refreshData()
                    }
                }
            })
            viewStateMessage.observe(viewLifecycleOwner, {
                it.contentIfNotHandled?.let { message ->
                    uiCommunicationListener.showSnackBarMessage(message)
                }
            })
            viewStateRoverMasterList.observe(viewLifecycleOwner, {
                adapter?.submitList(it)
            })
            viewStateSetEmptyMessage.observe(viewLifecycleOwner, {
                it.let { event ->
                    if (event == null)
                        hideEmptyMessage()
                    else
                        event.contentIfNotHandled?.let { it ->
                            setEmptyMessage(it)
                        }
                }

            })
            viewStateSheetData.observe(viewLifecycleOwner, {
                setSheetData(it)
            })
            viewStateSheetState.observe(viewLifecycleOwner, {
                bottomSheetBehavior.state = it
            })
            viewStateTopBarVisibility.observe(viewLifecycleOwner, {
                binding.topAppbar.homeAppbarTop.isVisible = it
            })
        }
    }

    private fun init() {

        roversViewModel = ViewModelProvider(requireActivity())[RoversViewModel::class.java]
        viewModel.setEmptyPhotos()
        binding.roverSwipeRefresh.setColorSchemeResources(R.color.accent)

        setBottomSheet()
        setRecyclerView()
    }

    private fun setListeners() {
        binding.emptyMessage.setOnClickListener {
            hideEmptyMessage()
            refreshData()
        }
        binding.roverSwipeRefresh.setOnRefreshListener {
            refreshData()
        }
    }

    private fun refreshData() {
        roversViewModel.getRoverData(isRefresh = true)
    }

    private fun getData() {
        if (roversViewModel.dataStateRover.value == null) {
            roversViewModel.getRoverData(isRefresh = false)
        }
    }

    private fun setRecyclerView() {
        adapter = MarsRoverAdapter(this, requestManager)
        binding.apply {
            recycler.setHasFixedSize(true)
            recycler.layoutManager = GridLayoutManager(requireContext(),getGallerySpan())
            recycler.adapter = adapter
        }
    }


    private fun getGallerySpan(): Int =
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Constants.ROVER_SPAN_MULTI
        } else {
            when (screenSize) {
                Configuration.SCREENLAYOUT_SIZE_NORMAL -> Constants.ROVER_SPAN
                Configuration.SCREENLAYOUT_SIZE_SMALL -> Constants.ROVER_SPAN
                else -> {
                    Constants.ROVER_SPAN_MULTI
                }
            }
        }

    private fun setBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.isGestureInsetBottomIgnored = true
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                roversViewModel.setViewStateSheetState(newState)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

        })
    }

    private fun hideEmptyMessage() {
        binding.emptyMessage.isVisible = false
        binding.emptyMessage.text = ""
    }

    private fun setEmptyMessage(message: String) {
        binding.emptyMessage.isVisible = true
        binding.emptyMessage.text = message
    }

    override fun onItemSelected(master: RoverMaster, position: Int) {
        navigateToPhotos(master)
    }

    override fun onItemSaveSelected(master: RoverMaster, position: Int) {
        navigateToSavedPhotos(master)
    }

    private fun navigateToPhotos(master: RoverMaster) {
        viewModel.setIsSavedView(false)
        navigateToPhotoGallery(master)
    }

    private fun navigateToPhotoGallery(master: RoverMaster){
        viewModel.setPosition(0)
        viewModel.setRoverMaster(master)
        findNavController().navigate(R.id.action_roversFragment_to_homeFragment)
    }

    private fun navigateToSavedPhotos(master: RoverMaster) {
        viewModel.setIsSavedView(true)
        navigateToPhotoGallery(master)
    }

    override fun onReadMoreSelected(master: RoverMaster, position: Int) {
        roversViewModel.setViewStateSheetData(master)
        roversViewModel.setViewStateSheetState(BottomSheetBehavior.STATE_EXPANDED)
    }

    override fun onAboutSelected() {

    }

    private fun hideSheet() {
        roversViewModel.setViewStateSheetState(BottomSheetBehavior.STATE_COLLAPSED)
    }

    private fun setSheetData(master: RoverMaster) {
        binding.bottomSheetView.apply {
            Glide.with(binding.root)
                .load(Constants.URL_DATA + master.image)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(roverImage)
            roverName.text = master.name
            roverDescription.text = master.description
            roverLandingDate.text = master.landing_date_in_millis.formatMillisToDisplayDate()
            roverLaunchDate.text = master.launch_date_in_millis.formatMillisToDisplayDate()
            roverMaxDate.text = getString(R.string.last_photo_updated, master.max_date_in_millis.formatMillisToDisplayDate())
            roverStatus.text = getString(R.string.rover_status, master.status)
            roverPhotosCount.text = getString(
                R.string.view_photos,
                if (master.status == ROVER_STATUS_COMPLETE) master.total_photos.toString() else master.total_photos.simplify() + "+"
            )
        }
        binding.bottomSheetView.roverPhotosCount.setOnClickListener {
            navigateToPhotos(master)
        }
        binding.bottomSheetView.roverFav.setOnClickListener {
            navigateToSavedPhotos(master)
        }
    }

    private fun setWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.recycler) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomAd = toDpi(60)
            val bottomMargin =
                requireActivity().resources.getDimension(R.dimen.global_window_padding)
            binding.recycler.updatePadding(bottom = systemWindows.bottom + bottomAd + bottomMargin.toInt(), left = systemWindows.left, right = systemWindows.right)

            return@setOnApplyWindowInsetsListener insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.topAppbar.homeCollapsingToolbarTop) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            binding.topAppbar.homeToolbarTop.updateMarginAndHeight(top = systemWindows.top)
            return@setOnApplyWindowInsetsListener insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomSheetView.sheetFrame) { _, insets ->
            val systemWindows = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.bottomSheetView.sheetFrame.updatePadding(left = systemWindows.left, right = systemWindows.right)
            binding.bottomSheetView.sheetFrame.updateMarginAndHeight(
                top = systemWindows.top,
                bottom = systemWindows.bottom + toDpi(60)
            )
            return@setOnApplyWindowInsetsListener insets
        }

    }

    private fun setBackPressCallBack() {
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
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}