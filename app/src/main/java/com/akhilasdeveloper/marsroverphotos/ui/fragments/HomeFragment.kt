package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.core.view.*
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.akhilasdeveloper.marsroverphotos.utilities.Constants
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.utilities.Utilities
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentHomeBinding
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.ui.MarsRoverPhotoLoadStateAdapter
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverPhotoAdapter
import com.google.android.material.datepicker.*
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import android.view.LayoutInflater

import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.akhilasdeveloper.marsroverphotos.utilities.scrollToCenter

import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import kotlinx.android.synthetic.main.layout_sol_select.view.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@ExperimentalPagingApi
@AndroidEntryPoint
class HomeFragment : BaseFragment(R.layout.fragment_home), RecyclerClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var utilities: Utilities

    private val adapter = MarsRoverPhotoAdapter(this)
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.homeAppbar) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            val layoutParams = (binding.homeToolbar.layoutParams as? ViewGroup.MarginLayoutParams)
            layoutParams?.setMargins(0, 0, 0, systemWindows.bottom)
            binding.homeToolbar.layoutParams = layoutParams
            return@setOnApplyWindowInsetsListener insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.photoRecycler) { _, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            binding.photoRecycler.updatePadding(bottom = systemWindows.bottom)
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

        val layoutManager = GridLayoutManager(
            requireContext(),
            Constants.GALLERY_SPAN,
            GridLayoutManager.VERTICAL,
            false
        )

        /*layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                *//*return when {
                    position % 3 == 0 -> 1
                    position % 5 == 0 -> 3
                    else -> 2
                }*//*
                return if (adapter.snapshot().size > position)
                    if (adapter.snapshot()[position]?.is_placeholder == true)
                        1
                    else
                        2
                else
                    2
            }
        }*/

        binding.apply {
            photoRecycler.setHasFixedSize(true)
            photoRecycler.layoutManager = layoutManager
            photoRecycler.adapter = adapter/*.withLoadStateHeaderAndFooter(
                header = MarsRoverPhotoLoadStateAdapter { adapter.retry() },
                footer = MarsRoverPhotoLoadStateAdapter { adapter.retry() },
            )*/
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
                if (!isHandled)
                    setDate()
            }
        })

        viewModel.dataStateDate.observe(viewLifecycleOwner, {
            binding.dateButtonText.text = utilities.formatMillis(it)
            currentDate = it
            getData()
            setSolButtonText()
        })

        viewModel.dataStateLoading.observe(viewLifecycleOwner, {
            binding.progress.isVisible = it
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
                setSolButtonText()
//                Glide.with(requireContext()).load(Constants.URL_DATA + master.image).into(binding.toolbarImage)
            }
        }
    }

    private fun setDate() {
        utilities.formatDateToMillis(master.max_date)?.let { date ->
            viewModel.setDate(date)
        }
    }

    private fun setSolButtonText() {
        binding.solButtonText.text = getString(
            R.string.sol,
            utilities.calculateDays(master.landing_date, currentDate).toString()
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
            utilities.calculateDays(master.landing_date, currentDate)?.let {
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
                viewModel.setDate(
                    utilities.calculateDaysEarthDate(
                        slider.value.toLong(),
                        utilities.formatDateToMillis(master.landing_date)!!
                    )
                )
                alertDialog.cancel()
            }
            dialogView.findViewById<MaterialButton>(R.id.cancel_sol_selector).setOnClickListener {
                alertDialog.cancel()
            }
        }
    }

    private fun getData() {
        currentDate?.let {
            viewModel.getData(master, it)
        }
    }

    private fun setListeners() {
        binding.dateButtonText.setOnClickListener {
            showDatePicker()
        }
        binding.solButtonText.setOnClickListener {
            showDialog()
        }
        /*lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadStates ->
                viewModel.setLoading(loadStates.refresh is LoadState.Loading)
                binding.emptyMessage.isVisible = loadStates.refresh is LoadState.Error
            }
        }*/
        binding.emptyMessage.setOnClickListener {
            getData()
        }
        adapter.addLoadStateListener {loadState->
            binding.photoRecycler.isVisible = loadState.mediator?.refresh is LoadState.NotLoading
            binding.progress.isVisible = loadState.mediator?.refresh is LoadState.Loading
            binding.emptyMessage.isVisible = loadState.mediator?.refresh is LoadState.Error
        }
    }

    private fun showDatePicker() {
        if (::master.isInitialized) {

            val constraintsBuilder = CalendarConstraints.Builder()
            val validators: ArrayList<CalendarConstraints.DateValidator> = ArrayList()
            validators.add(DateValidatorPointForward.from(utilities.formatDateToMillis(master.landing_date)!!))
            validators.add(DateValidatorPointBackward.before(utilities.formatDateToMillis(master.max_date)!!))
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
                    viewModel.setDate(it)
                }
                binding.dateButtonText.text = utilities.formatMillis(currentDate!!)
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