package com.akhilasdeveloper.marsroverphotos.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.akhilasdeveloper.marsroverphotos.Constants
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.Utilities
import com.akhilasdeveloper.marsroverphotos.databinding.FragmentHomeBinding
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverPhotoDb
import com.akhilasdeveloper.marsroverphotos.ui.MarsRoverPhotoLoadStateAdapter
import com.akhilasdeveloper.marsroverphotos.ui.adapters.MarsRoverPhotoAdapter
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment: BaseFragment(R.layout.fragment_home), RecyclerClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var utilities: Utilities

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

        val layoutManager = GridLayoutManager(requireContext(),Constants.GALLERY_SPAN,GridLayoutManager.VERTICAL,false)

        binding.apply {
            photoRecycler.setHasFixedSize(true)
            photoRecycler.layoutManager = layoutManager
            photoRecycler.adapter = adapter.withLoadStateHeaderAndFooter(
                header = MarsRoverPhotoLoadStateAdapter { adapter.retry() },
                footer = MarsRoverPhotoLoadStateAdapter { adapter.retry() },
            )
        }
    }

    private fun subscribeObservers() {

        viewModel.dataState.observe(viewLifecycleOwner, Observer { response ->
            response?.let {
                Timber.d("dataState1 : ${response}")
                adapter.submitData(viewLifecycleOwner.lifecycle,response)
            }
        })
    }

    private fun getData() {
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
        /*binding.homeAppbar.setNavigationOnClickListener {

        }*/
        /*binding.filter.setOnClickListener {

        }*/
        adapter.addLoadStateListener {
            binding.progress.isVisible = it.refresh is LoadState.Loading
            binding.progress.isVisible = it.append is LoadState.Loading
            Timber.d("Loading state : ${it.refresh is LoadState.Loading} : ${it.append.endOfPaginationReached}")
            if (adapter.itemCount <= 0){
                binding.emptyMessage.visibility = View.VISIBLE
            }else{
                binding.emptyMessage.visibility = View.GONE
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