package com.akhilasdeveloper.marsroverphotos.ui.fragments.home

import android.os.Bundle
import android.view.*
import androidx.core.view.*
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentHomeBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.google.android.material.datepicker.*
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import kotlinx.android.synthetic.main.layout_sol_select.view.*
import com.akhilasdeveloper.marsroverphotos.utilities.*
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.GALLERY_SPAN
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.MILLIS_IN_A_DAY
import kotlinx.coroutines.*

import java.util.*
import kotlin.collections.ArrayList
import android.annotation.SuppressLint
import androidx.coordinatorlayout.widget.CoordinatorLayout

import androidx.lifecycle.lifecycleScope
import com.akhilasdeveloper.marsroverphotos.ui.fragments.BaseFragment
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.AD_ENABLED
import com.google.android.gms.ads.AdRequest

@AndroidEntryPoint
class HomeFragment : BaseFragment(R.layout.fragment_home), RecyclerClickListener {

    private var _binding: FragmentHomeBinding? = null
    internal val binding get() = _binding!!

    @Inject
    lateinit var utilities: Utilities

    private val adapter = MarsRoverPhotoAdapter(this)
    internal var master: RoverMaster? = null
    internal var currentDate: Long? = null
    private var hideFastScrollerJob: Job? = null
    private var navigateToDate = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        init()
        setListeners()
        subscribeObservers()
    }

    private fun init() {

        setWindowInsets()

        val layoutManager = GridLayoutManager(
            requireContext(),
            GALLERY_SPAN,
            GridLayoutManager.VERTICAL,
            false
        )

        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter.snapshot().size > position)
                    if (adapter.snapshot()[position]?.is_placeholder != true)
                        1
                    else
                        GALLERY_SPAN
                else
                    GALLERY_SPAN
            }
        }

        binding.apply {
            photoRecycler.setHasFixedSize(true)
            photoRecycler.layoutManager = layoutManager
            photoRecycler.adapter = adapter
        }
        hideFastScroller()
        if (AD_ENABLED) {
            binding.itemAdBanner.isVisible = true
            val adRequest: AdRequest = AdRequest.Builder().build()
            binding.adView.loadAd(adRequest)
        }
    }

    private fun showMainProgress() {
        binding.progress.apply {
            if (!isVisible)
                isVisible = true
        }
    }

    private fun hideMainProgress() {
        binding.progress.apply {
            if (isVisible)
                isVisible = false
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
                    currentDate = rover.max_date_in_millis
                    viewModel.setDate(currentDate!!)
                    getData()
                }
            }
        })

        viewModel.dataStateDate.observe(viewLifecycleOwner, {
            currentDate = it
            setDateAndSolButtonText()
        })

        viewModel.dataStateLoading.observe(viewLifecycleOwner, {
            if (it) showMainProgress()
            else
                hideMainProgress()
        })

        viewModel.positionState.observe(viewLifecycleOwner, {
            scrollToPosition(it)
        })

        viewModel.dataStatePaging.observe(viewLifecycleOwner, {
            it?.let {
                val isHandled = it.hasBeenHandled()
                it.peekContent?.let { photos ->
                    it.setAsHandled()
                    adapter.submitData(viewLifecycleOwner.lifecycle, photos)
                    if (!isHandled) {
                        onDateSelected(currentDate!!)
                    }
                }
            }
        })
    }

    private fun setData() {
        master?.let {
            setTitle()
            initFastScroller()
        }
    }

    private fun setTitle() {
        binding.homeToolbarTop.title = master!!.name
        binding.homeCollapsingToolbarTop.title = master!!.name
    }

    private fun initFastScroller() {
        binding.solSlider.apply {
            val count =
                (master!!.max_date_in_millis - master!!.landing_date_in_millis) / MILLIS_IN_A_DAY
            this.valueFrom = 0f
            this.valueTo = count.toFloat()
            this.value = count.toFloat()
            this.stepSize = 1f
        }
    }

    private fun setDateAndSolButtonText() {
        setDateButtonText()
        setSolButtonText()
        setFastScrollerValue()
    }

    private fun setFastScrollerValue() {
        master?.let {
            binding.solSlider.apply {
                val count = (currentDate!! - master!!.landing_date_in_millis) / MILLIS_IN_A_DAY
                this.value = count.toFloat()
                hideFastScrollerDate()
            }
        }
    }

    private fun setSolButtonText() {
        master?.let {
            binding.solButtonText.text = getString(
                R.string.sol,
                utilities.calculateDays(it.landing_date_in_millis, currentDate).toString()
            )
        }
    }


    private fun setDateButtonText() {
        binding.dateButtonText.text = currentDate!!.formatMillisToDate()
    }

    private fun scrollToPosition(position: Int) {
        binding.photoRecycler.scrollToPosition(position)
    }

    internal fun onSolSelected(toLong: Long) {
        utilities.calculateDaysEarthDate(
            toLong,
            master!!.landing_date_in_millis
        ).let {
            currentDate = it
            onDateSelected(currentDate!!, true)
        }
    }

    private fun getData() {
        currentDate?.let { currentDate ->
            master?.let { master ->
                viewModel.getData(master, currentDate)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListeners() {
        binding.dateButtonText.setOnClickListener {
            binding.photoRecycler.stopScroll()
            showDatePicker()
        }

        binding.solButtonText.setOnClickListener {
            showSolSelectorDialog()
        }
        adapter.addLoadStateListener { loadStates ->

            binding.progressTop.isVisible = loadStates.source.prepend is LoadState.Loading
            binding.progress.isVisible = loadStates.source.append is LoadState.Loading
            binding.progressCenter.isVisible = loadStates.source.refresh is LoadState.Loading

            if (navigateToDate &&
                loadStates.source.prepend !is LoadState.Loading &&
                loadStates.source.append !is LoadState.Loading &&
                loadStates.source.refresh !is LoadState.Loading){
                    navigateToDate = false
                onDateSelected(currentDate!!)
            }

            binding.emptyMessage.isVisible = loadStates.source.refresh is LoadState.Error

            if (loadStates.source.refresh is LoadState.NotLoading &&
                loadStates.append.endOfPaginationReached &&
                adapter.itemCount < 1
            ) {
                binding.photoRecycler.isVisible = false
                binding.emptyMessage.isVisible = true
            }
        }

        binding.emptyMessage.setOnClickListener {
            getData()
        }

        binding.photoRecycler.observeFirstItemPosition(firstItemPosition = { position ->
            adapter.snapshot().let { items ->
                if (items.isNotEmpty() && items.size > position)
                    items[position]?.let {
                        viewModel.setDate(it.earth_date)
                    }
            }
        })

        binding.photoRecycler.observeVisibleItemPositions() { firstVisibleItemPosition, secondVisibleItemPosition ->
            if (firstVisibleItemPosition != -1 && secondVisibleItemPosition != -1) {
                Timber.d("Scroll position : $firstVisibleItemPosition : $secondVisibleItemPosition")
            }
        }

        binding.photoRecycler.fastScrollListener {
            Timber.d("isFast :$it")
            if (it)
                showFastScroller()
        }

        binding.photoRecycler.isIdle {
            if (it)
                hideFastScroller()
        }

        binding.solSlider.addOnChangeListener { slider, value, fromUser ->
            master?.let { rover ->
                val date = ((value.toLong() * MILLIS_IN_A_DAY) + rover.landing_date_in_millis)
                binding.scrollDateDisplayText.text = date.formatMillisToDisplayDate()
                showFastScrollerDate()
            }
        }

        binding.solSlider.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                hideFastScrollerDate()
                master?.let { rover ->
                    val date =
                        ((binding.solSlider.value.toLong() * MILLIS_IN_A_DAY) + rover.landing_date_in_millis)
                    onDateSelected(date, true)
                }
                hideFastScroller()
            } else {
                showFastScroller()
            }
            false
        }

        val params = binding.homeAppbar.layoutParams as CoordinatorLayout.LayoutParams
        params.behavior = object : HideListenableBottomAppBarBehavior() {
            override fun onSlideDown() {
                showAd()
            }

            override fun onSlideUp() {
                hideAd()
            }
        }
    }

    private fun showAd() {
        binding.itemAdBanner.apply {
            if (AD_ENABLED) {
                animate()
                    .alpha(1.0f)
                    .setListener(null).duration = 800L
            }
        }
    }

    private fun hideAd() {
        binding.itemAdBanner.apply {
            if (AD_ENABLED) {
                animate()
                    .alpha(0.0f)
                    .setListener(null).duration = 800L
            }
        }
    }


    private fun hideFastScrollerDate() {
        binding.scrollDateDisplayText.apply {
            if (isVisible) {
                alpha = 0f
                this@apply.isVisible = false
            }
        }
    }

    private fun showFastScrollerDate() {
        hideFastScrollerJob?.cancel()
        binding.scrollDateDisplayText.apply {
            if (!isVisible) {
                this@apply.isVisible = true
                animate()
                    .alpha(1.0f)
                    .setListener(null).duration = 800L
            }
        }
    }

    private fun hideFastScroller() {
        binding.slideFrame.apply {
            if (alpha == 1f) {
                hideFastScrollerJob?.cancel()
                hideFastScrollerJob = lifecycleScope.launch {
                    delay(2000L)
                    animate()
                        .translationY(0f)
                        .alpha(0.0f).duration = 400L
                }
            }
        }
    }

    private fun showFastScroller() {
        binding.slideFrame.apply {
            if (alpha == 0f) {
                animate()
                    .alpha(1.0f)
                    .setListener(null).duration = 400L
            }
        }
    }



    internal fun onDateSelected(date: Long, fetch: Boolean = false) {
        currentDate = date
        viewModel.setDate(date)
        val snapShot = adapter.snapshot()
        val search = snapShot.filter { photo ->
            photo?.earth_date == date && photo.is_placeholder
        }
        if (search.isNotEmpty()) {
            val pos = snapShot.indexOf(search[0])
            scrollToPosition(pos)
        } else if (fetch) {
            navigateToDate = true
            getData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        hideFastScrollerJob?.cancel()
    }

    override fun onItemSelected(marsRoverPhoto: MarsRoverPhotoTable, position: Int) {
        findNavController().navigate(R.id.action_homeFragment_to_roverViewFragment)
        val pos = adapter.snapshot().indexOf(marsRoverPhoto)
        viewModel.setPosition(pos)
    }
}