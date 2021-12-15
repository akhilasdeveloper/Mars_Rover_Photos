package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.animation.Animator
import android.os.Bundle
import android.view.*
import androidx.core.view.*
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentHomeBinding
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverPhotoAdapter
import com.google.android.material.datepicker.*
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import com.akhilasdeveloper.marsroverphotos.databinding.LayoutDateSelectBinding
import com.akhilasdeveloper.marsroverphotos.databinding.LayoutSolSelectBinding
import kotlinx.android.synthetic.main.layout_sol_select.view.*
import com.akhilasdeveloper.marsroverphotos.paging.MarsRoverPhotoLoadStateAdapter
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverDateAdapter
import com.akhilasdeveloper.marsroverphotos.utilities.*
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.GALLERY_SPAN
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.MILLIS_IN_A_DAY
import kotlinx.coroutines.*

import java.util.*
import kotlin.collections.ArrayList
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint

import androidx.core.view.ViewCompat.animate
import androidx.lifecycle.lifecycleScope
import androidx.core.view.ViewCompat.animate
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.SCROLL_DIRECTION_UP

@AndroidEntryPoint
class HomeFragment : BaseFragment(R.layout.fragment_home), RecyclerClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var utilities: Utilities

    private lateinit var adapter: MarsRoverPhotoAdapter
    private var master: RoverMaster? = null

    private var currentDate: Long? = null

    private var job: Job? = null

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

        adapter = MarsRoverPhotoAdapter(this)

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
    }

    private fun setWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.homeAppbar) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            val layoutParams = (binding.homeToolbar.layoutParams as? ViewGroup.MarginLayoutParams)
            layoutParams?.setMargins(0, 0, 0, systemWindows.bottom)
            binding.homeToolbar.layoutParams = layoutParams
            return@setOnApplyWindowInsetsListener insets
        }

        val recyclerBottomPadding = binding.photoRecycler.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.photoRecycler) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            binding.photoRecycler.updatePadding(bottom = systemWindows.bottom + recyclerBottomPadding)
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

        val layoutParams = (binding.progress.layoutParams as? ViewGroup.MarginLayoutParams)
        val marginBottom = layoutParams?.bottomMargin ?: 0
        ViewCompat.setOnApplyWindowInsetsListener(binding.progress) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            layoutParams?.setMargins(0, 0, 0, systemWindows.bottom + marginBottom)
            binding.progress.layoutParams = layoutParams
            return@setOnApplyWindowInsetsListener insets
        }

        val layoutParamsTop = (binding.progressTop.layoutParams as? ViewGroup.MarginLayoutParams)
        val marginTop = layoutParamsTop?.topMargin ?: 0
        ViewCompat.setOnApplyWindowInsetsListener(binding.progressTop) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            layoutParamsTop?.setMargins(0, marginTop + systemWindows.top, 0, 0)
            binding.progressTop.layoutParams = layoutParamsTop
            return@setOnApplyWindowInsetsListener insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.slideFrame) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())
            resources.displayMetrics.let { matrics ->
                val bottomAppBarHeight = 76 * matrics.density.toInt()
                val topAppBarHeight = 200 * matrics.density.toInt()

                val layoutParams2 =
                    (binding.solSlider.layoutParams as? ViewGroup.LayoutParams)
                layoutParams2?.width =
                    matrics.heightPixels - (bottomAppBarHeight + topAppBarHeight)
                binding.solSlider.layoutParams = layoutParams2

                val layoutParams1 =
                    (binding.slideFrame.layoutParams as? ViewGroup.LayoutParams)
                layoutParams1?.height =
                    matrics.heightPixels - (bottomAppBarHeight + topAppBarHeight)
                binding.slideFrame.layoutParams = layoutParams1

                val layoutParams3 =
                    (binding.slideFrame.layoutParams as? ViewGroup.MarginLayoutParams)
                layoutParams3?.bottomMargin = bottomAppBarHeight + systemWindows.bottom
                binding.slideFrame.layoutParams = layoutParams3
            }
            binding.progress.layoutParams = layoutParams
            return@setOnApplyWindowInsetsListener insets
        }
    }

    private fun showProgress() {
        binding.progress.apply {
            if (!isVisible)
                isVisible = true
        }
    }

    private fun hideProgress() {
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
                    getData()
                    setDate()
                }
            }
        })

        viewModel.dataStateDate.observe(viewLifecycleOwner, {
            currentDate = it
            setSolButtonText()
        })

        viewModel.dataStateLoading.observe(viewLifecycleOwner, {
            if (it) showProgress()
            else
                hideProgress()
//            binding.progress.isVisible = it
        })

        viewModel.positionState.observe(viewLifecycleOwner, {
            scrollToPosition(it)
        })

        viewModel.dataStateDatePosition.observe(viewLifecycleOwner, {
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
        binding.apply {
            master?.let {
                homeToolbarTop.title = it.name
                homeCollapsingToolbarTop.title = it.name

                binding.solSlider.apply {

                    val count =
                        (master!!.max_date_in_millis - master!!.landing_date_in_millis) / MILLIS_IN_A_DAY

                    this.valueFrom = 0f
                    this.valueTo = count.toFloat()
                    this.value = count.toFloat()
                    this.stepSize = 1f

                    hideFastScrollerDate()
                }
            }
        }
    }

    private fun setDate() {
        master?.max_date_in_millis?.let { date ->
            viewModel.setDate(date)
        }
    }

    private fun setSolButtonText() {
        binding.apply {
            dateButtonText.text = currentDate!!.formatMillisToDate()
            master?.let {
                binding.solButtonText.text = getString(
                    R.string.sol,
                    utilities.calculateDays(it.landing_date_in_millis, currentDate).toString()
                )

                binding.solSlider.apply {

                    val count = (currentDate!! - master!!.landing_date_in_millis) / MILLIS_IN_A_DAY
                    this.value = count.toFloat()

                    hideFastScrollerDate()
                }
            }
        }
    }

    private fun scrollToPosition(position: Int) {
        binding.photoRecycler.scrollToCenter(position)
    }

    private fun showSolSelectorDialog() {
        master?.let { rover ->

            val dialogView: LayoutSolSelectBinding =
                LayoutSolSelectBinding.inflate(LayoutInflater.from(requireContext()))
            val builder: AlertDialog.Builder =
                AlertDialog.Builder(requireContext(), R.style.dialog_background)
                    .setView(dialogView.root)
            val alertDialog: AlertDialog = builder.create()

            dialogView.apply {
                solSlider.valueTo = rover.max_sol.toFloat()
                utilities.calculateDays(rover.landing_date_in_millis, currentDate)?.let {
                    solSlider.value = it.toFloat()
                }
                solSelectorCount.setText(dialogView.solSlider.value.toInt().toString())
                solSlider.addOnChangeListener { _, value, _ ->
                    solSelectorCount.setText("${value.toInt()}")
                }
                nextSolSelector.setOnClickListener {
                    if (solSlider.value < solSlider.valueTo)
                        solSlider.value += 1
                }
                prevSolSelector.setOnClickListener {
                    if (solSlider.value > 0)
                        solSlider.value -= 1
                }
                solSelectorCount.addTextChangedListener { edit ->
                    edit?.let {
                        val validatedText = validateSolText(it.toString())
                        if (validatedText != edit.toString()) {
                            solSelectorCount.setText(validatedText)
                        }
                        solSlider.value = validatedText.toInt().toFloat()
                    }
                }
                okSolSelector.setOnClickListener {
                    onSolSelected(solSlider.value.toLong())
                    alertDialog.cancel()
                }

                cancelSolSelector.setOnClickListener {
                    alertDialog.cancel()
                }
            }

            alertDialog.show()
        }
    }

    private fun validateSolText(it: String): String {
        var validated = it.filter { str -> str.isDigit() }
        if (validated.isNotEmpty()) {
            val sol = validated.toInt()
            master?.let { rover ->
                if (rover.max_sol < sol)
                    validated = rover.max_sol.toString()
                if (sol < 0)
                    validated = "0"
            }
        }
        return validated
    }

    private fun showDateSelectorDialog() {
        master?.let { rover ->

            val dialogView: LayoutDateSelectBinding =
                LayoutDateSelectBinding.inflate(LayoutInflater.from(requireContext()))
            val builder: AlertDialog.Builder =
                AlertDialog.Builder(requireContext()).setView(dialogView.root)
            val alertDialog: AlertDialog = builder.create()

            dialogView.apply {

                okSolSelector.setOnClickListener {
                    alertDialog.cancel()
                }

                cancelSolSelector.setOnClickListener {
                    alertDialog.cancel()
                }
            }

            alertDialog.show()
        }
    }

    private fun onSolSelected(toLong: Long) {
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
//            showDateSelectorDialog()
        }

        binding.solButtonText.setOnClickListener {
            showSolSelectorDialog()
        }
        adapter.addLoadStateListener { loadStates ->

            binding.progressTop.isVisible = loadStates.source.prepend is LoadState.Loading
            binding.progress.isVisible = loadStates.source.append is LoadState.Loading
            binding.progressCenter.isVisible = loadStates.source.refresh is LoadState.Loading

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
        job?.cancel()
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
                job?.cancel()
                job = lifecycleScope.launch {
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

    private fun showDatePicker() {
        master?.let { master ->

            val constraintsBuilder = CalendarConstraints.Builder()
            val validators: ArrayList<CalendarConstraints.DateValidator> = ArrayList()
            validators.add(DateValidatorPointForward.from(master.landing_date_in_millis))
            validators.add(DateValidatorPointBackward.before(master.max_date_in_millis + MILLIS_IN_A_DAY))
            constraintsBuilder.setValidator(CompositeDateValidator.allOf(validators))

            val setDate = currentDate?.let { it + MILLIS_IN_A_DAY }
            val datePicker =
                MaterialDatePicker.Builder.datePicker()
                    .setTitleText(getString(R.string.select_date))
                    .setSelection(setDate)
                    .setTheme(R.style.ThemeOverlay_App_DatePicker)
                    .setCalendarConstraints(constraintsBuilder.build())
                    .build()

            datePicker.show(requireActivity().supportFragmentManager, "RoverDatePicker")

            datePicker.addOnPositiveButtonClickListener {
                datePicker.selection?.let {
                    onDateSelected(it.formatMillisToDate().formatDateToMillis()!!, true)
                }
                binding.dateButtonText.text = currentDate!!.formatMillisToDate()
            }
        }
    }

    private fun onDateSelected(date: Long, fetch: Boolean = false) {
        currentDate = date
        viewModel.setDate(date)
        val snapShot = adapter.snapshot()
        val search = snapShot.filter { photo ->
            photo?.let { it.earth_date == date && it.is_placeholder } ?: false
        }
        if (search.isNotEmpty()) {
            val pos = snapShot.indexOf(search[0])
            binding.photoRecycler.scrollToPosition(pos)
        } else if (fetch) {
            getData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        job?.cancel()
    }

    override fun onItemSelected(marsRoverPhoto: MarsRoverPhotoTable, position: Int) {
        findNavController().navigate(R.id.action_homeFragment_to_roverViewFragment)
        val pos = adapter.snapshot().indexOf(marsRoverPhoto)
        Timber.d("position : $position : ${marsRoverPhoto.photo_id} : $pos")
        viewModel.setPosition(pos)
    }
}