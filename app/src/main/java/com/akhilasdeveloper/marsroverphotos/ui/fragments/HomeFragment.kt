package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.core.view.*
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akhilasdeveloper.marsroverphotos.Constants
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.Utilities
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentHomeBinding
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.ui.MarsRoverPhotoLoadStateAdapter
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverPhotoAdapter
import com.google.android.material.datepicker.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject

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

        binding.progress.visibility = View.VISIBLE

        ViewCompat.setOnApplyWindowInsetsListener(binding.homeAppbar) { v, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            val layoutParams = (binding.homeToolbar.layoutParams as? ViewGroup.MarginLayoutParams)
            layoutParams?.setMargins(0, 0, 0, systemWindows.bottom)
            binding.homeToolbar.layoutParams = layoutParams
            return@setOnApplyWindowInsetsListener insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.photoRecycler) { v, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            binding.photoRecycler.updatePadding(bottom = systemWindows.bottom)
            binding.photoRecycler.updatePadding(top = systemWindows.top)
            return@setOnApplyWindowInsetsListener insets
        }

        val layoutManager = GridLayoutManager(
            requireContext(),
            Constants.GALLERY_SPAN,
            GridLayoutManager.VERTICAL,
            false
        )

        binding.apply {
            photoRecycler.setHasFixedSize(true)
            photoRecycler.layoutManager = layoutManager
            photoRecycler.adapter = adapter.withLoadStateHeaderAndFooter(
                header = MarsRoverPhotoLoadStateAdapter { adapter.retry() },
                footer = MarsRoverPhotoLoadStateAdapter { adapter.retry() },
            )
        }

        adapter.addLoadStateListener {
            if (it.prepend is LoadState.NotLoading && it.prepend.endOfPaginationReached) {
                binding.progress.visibility = View.GONE
            }else{
                binding.progress.visibility = View.VISIBLE
            }
            if (it.append is LoadState.NotLoading && it.append.endOfPaginationReached) {
                binding.emptyMessage.isVisible = adapter.itemCount < 1
            }
        }

        WindowInsetsControllerCompat(requireActivity().window, binding.homeFragmentRoot).show(
            WindowInsetsCompat.Type.systemBars()
        )
    }

    private fun subscribeObservers() {

        viewModel.dataState.observe(viewLifecycleOwner, Observer { response ->
            response?.let {
                Timber.d("dataState1 : ${response}")
                adapter.submitData(viewLifecycleOwner.lifecycle, response)
                binding.progress.visibility = View.GONE
            }
        })

        viewModel.dataStateRoverMaster.observe(viewLifecycleOwner, Observer {
            master = it
            setData()
        })
    }

    private fun setData() {
        binding.apply {
            if (::master.isInitialized) {
                dateButtonText.text = master.max_date
                toolbarTitle.text = master.name
                currentDate = utilities.formatDateToMillis(master.max_date)
            }
        }
    }

    private fun getData() {
        currentDate?.let {
            viewModel.getData(master.name, it)
        }

    }

    private fun setListeners() {
        binding.dateButtonText.setOnClickListener {
            showDatePicker()
        }
        /*adapter.addLoadStateListener {
            if (adapter.itemCount <= 0) {
                binding.emptyMessage.visibility = View.VISIBLE
            } else {
                binding.emptyMessage.visibility = View.GONE
            }
        }*/
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
                    .setTitleText("Select date")
                    .setSelection(currentDate)
                    .setTheme(R.style.ThemeOverlay_App_DatePicker)
                    .setCalendarConstraints(constraintsBuilder.build())
                    .build()

            datePicker.show(requireActivity().supportFragmentManager, "RoverDatePicker")

            datePicker.addOnPositiveButtonClickListener {
                currentDate = datePicker.selection
                binding.dateButtonText.text = utilities.formatMillis(currentDate!!)
                getData()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
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