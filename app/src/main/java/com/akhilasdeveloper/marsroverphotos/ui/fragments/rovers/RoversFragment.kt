package com.akhilasdeveloper.marsroverphotos.ui.fragments.rovers

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.*
import androidx.core.widget.NestedScrollView
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.akhilasdeveloper.marsroverphotos.utilities.Constants
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentRoversBinding
import com.akhilasdeveloper.marsroverphotos.ui.fragments.BaseFragment
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.AD_ENABLED
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@ExperimentalPagingApi
@AndroidEntryPoint
class RoversFragment : BaseFragment(R.layout.fragment_rovers), RecyclerRoverClickListener {

    private var _binding: FragmentRoversBinding? = null
    private val binding get() = _binding!!
    private var adapter: MarsRoverAdapter = MarsRoverAdapter(this)
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>

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
        subscribeObservers()
        getData()
    }

    private fun init() {

        setBottomSheet()
        setRecyclerView()
        binding.roverSwipeRefresh.setColorSchemeResources(R.color.accent)
        viewModel.setEmptyPhotos()
        if (AD_ENABLED) {
            binding.itemAdBanner.isVisible = true
            val adRequest: AdRequest = AdRequest.Builder().build()
            binding.adView.loadAd(adRequest)
        }
    }

    private fun setListeners() {
        binding.emptyMessage.setOnClickListener {
            hideEmptyMessage()
            refreshData()
        }
        binding.roverSwipeRefresh.setOnRefreshListener {
            refreshData()
        }

        binding.adView.adListener = object : AdListener(){
            override fun onAdLoaded() {
                super.onAdLoaded()
                showAd()
            }
        }
    }

    private fun refreshData() {
        viewModel.getRoverData(isRefresh = true)
    }

    private fun subscribeObservers() {
        viewModel.dataStateRover.observe(viewLifecycleOwner, { response ->
            response?.data?.let {
                Timber.d("RoverFragment response : $it")
                if (it.isEmpty())
                    setEmptyMessage("Tap to refresh")
                else {
                    hideEmptyMessage()
                    adapter.submitList(it)
                }
            }

            viewModel.setLoading(response?.isLoading == true)

            response?.error?.let {
                uiCommunicationListener.showSnackBarMessage(
                    messageText = it,
                    buttonText = "Refresh"
                ) {
                    refreshData()
                }
                setEmptyMessage("$it\nTap to refresh")
            }
        })

        viewModel.dataStateRoverMaster.value?.let {
            it.peekContent?.let { rover ->
                setSheetData(rover)
            }
        }

        viewModel.dataStateLoading.observe(viewLifecycleOwner, {
            binding.roverSwipeRefresh.isRefreshing = it
        })

        binding.adView.adListener = object : AdListener(){
            override fun onAdLoaded() {
                super.onAdLoaded()
                showAd()
            }
        }
    }

    private fun getData() {
        if (viewModel.dataStateRover.value == null) {
            viewModel.getRoverData(isRefresh = false)
        }
    }

    private fun setRecyclerView() {
        binding.apply {
            recycler.setHasFixedSize(true)
            recycler.layoutManager = LinearLayoutManager(requireContext())
            recycler.adapter = adapter
        }
    }

    private fun setBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.isGestureInsetBottomIgnored = true
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED)
                    binding.homeAppbarTop.visibility = View.VISIBLE
                else
                    binding.homeAppbarTop.visibility = View.GONE
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

    private fun navigateToPhotos(master: RoverMaster) {
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
        binding.bottomSheetView.apply {
            Glide.with(binding.root)
                .load(Constants.URL_DATA + master.image)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(roverImage)
            roverName.text = master.name
            roverDescription.text = master.description
            roverLandingDate.text = master.landing_date
            roverLaunchDate.text = master.launch_date
            roverStatus.text = getString(R.string.rover_status, master.status)
            roverPhotosCount.text = getString(R.string.view_photos, master.total_photos.toString())
        }
        binding.bottomSheetView.roverPhotosCount.setOnClickListener {
            navigateToPhotos(master)
        }
    }

    private fun setWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.recycler) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            resources.displayMetrics.let { matrics ->
                val bottomAd = 60 * matrics.density.toInt()
                val bottomMargin = requireActivity().resources.getDimension(R.dimen.global_window_padding)
                binding.recycler.updatePadding(bottom = systemWindows.bottom + bottomAd + bottomMargin.toInt())
            }
            return@setOnApplyWindowInsetsListener insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.homeCollapsingToolbarTop) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            val layoutParams =
                (binding.homeToolbarTop.layoutParams as? ViewGroup.MarginLayoutParams)
            layoutParams?.setMargins(0, systemWindows.top, 0, 0)
            binding.homeToolbarTop.layoutParams = layoutParams
            return@setOnApplyWindowInsetsListener insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomSheetView.sheetFrame) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            val layoutParams =
                (binding.bottomSheetView.sheetFrame.layoutParams as? ViewGroup.MarginLayoutParams)
            layoutParams?.setMargins(0, systemWindows.top, 0, systemWindows.bottom)
            binding.bottomSheetView.sheetFrame.layoutParams = layoutParams
            return@setOnApplyWindowInsetsListener insets
        }

        if (AD_ENABLED) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.itemAdBanner) { _, insets ->
                val systemWindows =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val layoutParams =
                    (binding.itemAdBanner.layoutParams as? ViewGroup.MarginLayoutParams)

                    val bottomMargin = requireActivity().resources.getDimension(R.dimen.global_window_padding)
                    layoutParams?.setMargins(0, 0, 0, systemWindows.bottom + bottomMargin.toInt())

                binding.itemAdBanner.layoutParams = layoutParams
                return@setOnApplyWindowInsetsListener insets
            }
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

    private fun showAd(){
        binding.itemAdBanner.apply {
            if (AD_ENABLED) {
                animate()
                    .alpha(1.0f)
                    .setListener(null).duration = 800L
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}