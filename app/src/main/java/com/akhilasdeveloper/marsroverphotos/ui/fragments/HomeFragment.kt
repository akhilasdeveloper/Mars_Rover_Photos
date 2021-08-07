package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import com.akhilasdeveloper.marsroverphotos.Constants
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.data.RoverPhotoViewItem
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentHomeBinding
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverDetalsDb
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.ui.MarsRoverPhotoLoadStateAdapter
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverDateAdapter
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverPhotoAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment: BaseFragment(R.layout.fragment_home), RecyclerClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val adapter = MarsRoverPhotoAdapter(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        init()
        setListeners()
        subscribeObservers()
        getData()
    }

    private fun init() {
        uiCommunicationListener.setupActionBar(binding.homeToolbar)
        binding.homeToolbar.title = "Mars Rover Images"
        ViewCompat.setOnApplyWindowInsetsListener(binding.homeAppbar) { v, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            val layoutParams = (binding.homeToolbar.layoutParams as? ViewGroup.MarginLayoutParams)
            layoutParams?.setMargins(0, 0, 0, systemWindows.bottom)
            binding.homeToolbar.layoutParams = layoutParams
            return@setOnApplyWindowInsetsListener insets
        }
        /*ViewCompat.setOnApplyWindowInsetsListener(binding.homeTitleToolbar) { v, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            val layoutParams = (binding.homeTitleToolbar.layoutParams as? ViewGroup.MarginLayoutParams)
            layoutParams?.setMargins(0, systemWindows.top,0,0)
            binding.homeTitleToolbar.layoutParams = layoutParams
            return@setOnApplyWindowInsetsListener insets
        }*/
        ViewCompat.setOnApplyWindowInsetsListener(binding.filter) { v, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            val layoutParams = (binding.filter.layoutParams as? ViewGroup.MarginLayoutParams)
            layoutParams?.setMargins(0, 0, 0, systemWindows.bottom + 20)
            binding.filter.layoutParams = layoutParams
            return@setOnApplyWindowInsetsListener insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.photoRecycler) { v, insets ->
            val systemWindows =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            binding.photoRecycler.updatePadding(bottom = systemWindows.bottom)
            binding.photoRecycler.updatePadding(top = systemWindows.top)
            return@setOnApplyWindowInsetsListener insets
        }

        val layoutManager = GridLayoutManager(requireContext(),Constants.GALLERY_SPAN,GridLayoutManager.VERTICAL,false)
        layoutManager.spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (adapter.getItemViewType(position)) {
                    adapter.DATEITEM -> layoutManager.spanCount
                    adapter.PHOTOITEM -> 1
                    else -> 1
                }
            }
        }
        binding.apply {
            photoRecycler.setHasFixedSize(true)
            photoRecycler.layoutManager = layoutManager
            photoRecycler.adapter = adapter/*.withLoadStateHeaderAndFooter(
                header = MarsRoverPhotoLoadStateAdapter { adapter.retry() },
                footer = MarsRoverPhotoLoadStateAdapter { adapter.retry() },
            )*/
        }
        viewLifecycleOwner.lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadStates ->
                binding.progress.isVisible = loadStates.refresh is LoadState.Loading
                /*retry.isVisible = loadState.refresh !is LoadState.Loading
                errorMsg.isVisible = loadState.refresh is LoadState.Error*/
            }
        }
    }

    private fun subscribeObservers() {
        viewModel.dataState.observe(viewLifecycleOwner, Observer { response ->
            response?.let {
                binding.progress.isVisible = false
                adapter.submitData(viewLifecycleOwner.lifecycle,response)
            }
        })

    }

    private fun getData() {
        if (viewModel.dataState.value==null) {
            binding.progress.isVisible = true
            viewModel.getData(
                MarsRoverDetalsDb(
                    rover_landing_date = "2012-08-06",
                    rover_launch_date = "2011-11-26",
                    rover_name = "Curiosity",
                    rover_status = "active"
                )
            )
        }
    }

    /*override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.filter -> {

                true
            }
            R.id.settings -> {

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }*/

    private fun setListeners() {
        binding.homeAppbar.setNavigationOnClickListener {

        }
        binding.filter.setOnClickListener {

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

    override fun onItemSelected(marsRoverPhoto: RoverPhotoViewItem, position: Int) {
        findNavController().navigate(R.id.action_homeFragment_to_roverViewFragment)
        viewModel.setPosition(position)
    }
}