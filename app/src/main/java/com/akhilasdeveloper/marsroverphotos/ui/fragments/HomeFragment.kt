package com.akhilasdeveloper.marsroverphotos.ui.fragments

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
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverPhotoAdapter
import com.google.android.material.datepicker.*
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import kotlinx.android.synthetic.main.layout_sol_select.view.*
import com.akhilasdeveloper.marsroverphotos.paging.MarsRoverPhotoLoadStateAdapter
import com.akhilasdeveloper.marsroverphotos.utilities.*
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.GALLERY_SPAN
import kotlinx.coroutines.*


@ExperimentalPagingApi
@AndroidEntryPoint
class HomeFragment : BaseFragment(R.layout.fragment_home), RecyclerClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var utilities: Utilities

    private lateinit var adapter: MarsRoverPhotoAdapter
    private lateinit var master: RoverMaster

    private var currentDate: Long? = null

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
        val loadStateAdapter = MarsRoverPhotoLoadStateAdapter { adapter.retry() }

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
            photoRecycler.adapter = adapter/*.withLoadStateHeaderAndFooter(
                header = loadStateAdapter,
                footer = loadStateAdapter,
            )*/
        }

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

        val layoutParams =  (binding.progress.layoutParams as? ViewGroup.MarginLayoutParams)
        val marginBottom = layoutParams?.bottomMargin ?: 0
        ViewCompat.setOnApplyWindowInsetsListener(binding.progress) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            layoutParams?.setMargins(0, 0, 0, systemWindows.bottom + marginBottom)
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

        viewModel.dataState.observe(viewLifecycleOwner, { response ->
            response?.let {
                Timber.d("dataState1 : $response")
                adapter.submitData(viewLifecycleOwner.lifecycle, response)
            }
        })

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
    }

    private fun setData() {
        binding.apply {
            if (::master.isInitialized) {
                homeToolbarTop.title = master.name
                homeCollapsingToolbarTop.title = master.name
            }
        }
    }

    private fun setDate() {
        master.max_date_in_millis.let { date ->
            viewModel.setDate(date)
        }
    }

    private fun setSolButtonText() {
        binding.dateButtonText.text = currentDate!!.formatMillisToDate()
        binding.solButtonText.text = getString(
            R.string.sol,
            utilities.calculateDays(master.landing_date_in_millis, currentDate).toString()
        )
    }

    private fun scrollToPosition(position: Int) {
        binding.photoRecycler.scrollToCenter(position)
    }

    private fun showDialog() {
        if (::master.isInitialized) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
            val viewGroup: ViewGroup = binding.root
            val dialogView: View = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_sol_select, viewGroup, false)
            val slider = dialogView.findViewById<Slider>(R.id.sol_slider)
            val sol = dialogView.findViewById<TextView>(R.id.solSelectorCount)
            slider.valueTo = master.max_sol.toFloat()
            utilities.calculateDays(master.landing_date_in_millis, currentDate)?.let {
                slider.value = it.toFloat()
            }
            sol.text = slider.value.toInt().toString()
            slider.addOnChangeListener { _, value, _ ->
                sol.text = "${value.toInt()}"
            }
            builder.setView(dialogView)
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
            dialogView.findViewById<MaterialButton>(R.id.ok_sol_selector).setOnClickListener {
                utilities.calculateDaysEarthDate(
                    slider.value.toLong(),
                    master.landing_date_in_millis
                ).let {
                    currentDate = it
                    getData()
                    viewModel.setDate(it)
                }
                alertDialog.cancel()
            }
            dialogView.findViewById<MaterialButton>(R.id.cancel_sol_selector).setOnClickListener {
                alertDialog.cancel()
            }
        }
    }

    private fun getData() {
        currentDate?.let { currentDate ->
            viewModel.getData(master, currentDate)
        }
    }

    private fun setListeners() {
        binding.dateButtonText.setOnClickListener {
            showDatePicker()
        }
        binding.solButtonText.setOnClickListener {
            showDialog()
        }
        adapter.addLoadStateListener { loadStates ->
            binding.photoRecycler.isVisible = loadStates.mediator?.refresh is LoadState.NotLoading
            viewModel.setLoading(
                        loadStates.mediator?.append is LoadState.Loading ||
                        loadStates.mediator?.refresh is LoadState.Loading
            )
            binding.emptyMessage.isVisible = loadStates.mediator?.refresh is LoadState.Error

            if (loadStates.mediator?.refresh is LoadState.NotLoading &&
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

    }

    private fun showDatePicker() {
        if (::master.isInitialized) {

            val constraintsBuilder = CalendarConstraints.Builder()
            val validators: ArrayList<CalendarConstraints.DateValidator> = ArrayList()
            validators.add(DateValidatorPointForward.from(master.landing_date_in_millis))
            validators.add(DateValidatorPointBackward.before(master.max_date_in_millis))
            constraintsBuilder.setValidator(CompositeDateValidator.allOf(validators))

            val datePicker =
                MaterialDatePicker.Builder.datePicker()
                    .setTitleText(getString(R.string.select_date))
                    .setSelection(currentDate)
                    .setTheme(R.style.ThemeOverlay_App_DatePicker)
                    .setCalendarConstraints(constraintsBuilder.build())
                    .build()

            datePicker.show(requireActivity().supportFragmentManager, "RoverDatePicker")

            datePicker.addOnPositiveButtonClickListener {
                datePicker.selection?.let {
                    Timber.d("Selected Date : $it")
                    currentDate = it
                    viewModel.setDate(it)
                    getData()
                }
                binding.dateButtonText.text = currentDate!!.formatMillisToDate()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onItemSelected(marsRoverPhoto: MarsRoverPhotoDb, position: Int) {
        findNavController().navigate(R.id.action_homeFragment_to_roverViewFragment)
        viewModel.setPosition(position)
    }
}